/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024-2025 Pasqual Koschmieder and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package tools.simrail.backend.collector.journey;

import jakarta.annotation.Nonnull;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import tools.simrail.backend.collector.server.SimRailServerDescriptor;
import tools.simrail.backend.common.journey.JourneyEntity;
import tools.simrail.backend.common.journey.JourneyEventEntity;
import tools.simrail.backend.common.journey.JourneyEventType;
import tools.simrail.backend.common.journey.JourneyPassengerStopInfo;
import tools.simrail.backend.common.journey.JourneyStopDescriptor;
import tools.simrail.backend.common.journey.JourneyStopType;
import tools.simrail.backend.common.journey.JourneyTimeType;
import tools.simrail.backend.common.journey.JourneyTransport;
import tools.simrail.backend.common.point.SimRailPoint;
import tools.simrail.backend.common.point.SimRailPointProvider;
import tools.simrail.backend.common.signal.PlatformSignalProvider;
import tools.simrail.backend.common.util.UuidV5Factory;

final class JourneyEventRealtimeUpdater {

  // services provided externally
  private final SimRailPointProvider pointProvider;
  private final UuidV5Factory journeyEventIdFactory;
  private final PlatformSignalProvider signalProvider;

  // state provided externally
  private final JourneyEntity journey;
  private final SimRailServerDescriptor server;
  private final List<JourneyEventEntity> journeyEvents;

  // state maintained internally
  private final Map<UUID, JourneyEventEntity> updatedJourneyEvents;

  private JourneyEventRealtimeUpdater(
    @Nonnull JourneyEntity journey,
    @Nonnull SimRailServerDescriptor server,
    @Nonnull List<JourneyEventEntity> journeyEvents,
    @Nonnull SimRailPointProvider pointProvider,
    @Nonnull UuidV5Factory journeyEventIdFactory,
    @Nonnull PlatformSignalProvider signalProvider
  ) {
    this.journey = journey;
    this.server = server;
    this.journeyEvents = journeyEvents;

    this.pointProvider = pointProvider;
    this.signalProvider = signalProvider;
    this.journeyEventIdFactory = journeyEventIdFactory;

    this.updatedJourneyEvents = new HashMap<>();
  }

  /**
   * Update the times of all events due to a position change of the journey.
   */
  public void updateEventsDueToPositionChange() {
    Optional.ofNullable(this.journey.getPosition())
      .flatMap(pos -> this.pointProvider.findPointWherePosInBounds(pos.getLongitude(), pos.getLatitude()))
      .ifPresentOrElse(this::updateEventTimesDueToArrivalAtPoint, this::updateEventTimesDueToDepartureFromPoint);
  }

  private void updateEventTimesDueToArrivalAtPoint(@Nonnull SimRailPoint currentPoint) {
    // journey is currently at a point along the route, find the point
    var eventsOfPoint = this.journeyEvents.stream()
      .filter(event -> event.getStopDescriptor().getId().equals(currentPoint.getId()))
      .collect(Collectors.toMap(JourneyEventEntity::getEventType, Function.identity()));
    if (eventsOfPoint.isEmpty()) {
      // if the current point has no prefix it is a stopping point - these can
      // be passed without being scheduled so we don't want to record these
      var prefix = currentPoint.getPrefix();
      if (prefix == null) {
        return;
      }

      // find the last confirmed event along the route and validate that it was a departure
      // to not insert an arrival event after another arrival event
      var lastConfirmedEvent = this.journeyEvents.reversed()
        .stream()
        .filter(event -> event.getRealtimeTimeType() == JourneyTimeType.REAL)
        .findFirst()
        .orElse(null);
      if (lastConfirmedEvent == null || lastConfirmedEvent.getEventType() != JourneyEventType.DEPARTURE) {
        return;
      }

      // create a JIT additional event for the point and mark them as updated
      var additionalEventPair = this.createJitAdditionalEvent(currentPoint, lastConfirmedEvent);
      this.markEventAsUpdated(additionalEventPair.getFirst());
      this.markEventAsUpdated(additionalEventPair.getSecond());
      return;
    }

    // get the arrival event of the point, if no arrival event is scheduled we can skip the process
    var arrivalEvent = eventsOfPoint.get(JourneyEventType.ARRIVAL);
    if (arrivalEvent == null) {
      return;
    }

    // update the passenger stop info if the journey has a passenger stop at the point and the next signal is known
    var nextSignal = this.journey.getNextSignal();
    if (arrivalEvent.getStopType() == JourneyStopType.PASSENGER
      && arrivalEvent.getRealtimePassengerStopInfo() == null
      && nextSignal != null) {
      this.signalProvider.findSignalInfo(currentPoint.getId(), nextSignal.getName()).ifPresent(platformInfo -> {
        // found the associated platform, update the passenger stop info
        var realtimeStopInfo = new JourneyPassengerStopInfo(platformInfo.getTrack(), platformInfo.getPlatform());
        arrivalEvent.setRealtimePassengerStopInfo(realtimeStopInfo);
        this.markEventAsUpdated(arrivalEvent);

        // set the info in the departure event as well, if one exists
        var departureEvent = eventsOfPoint.get(JourneyEventType.DEPARTURE);
        if (departureEvent != null) {
          departureEvent.setRealtimePassengerStopInfo(realtimeStopInfo);
          this.markEventAsUpdated(departureEvent);
        }
      });
    }

    // check if the time of the event has already been updated, this must be done after the
    // passenger stop info update as the signal might change after the first arrival,
    // but the time should not
    if (arrivalEvent.getRealtimeTimeType() == JourneyTimeType.REAL) {
      return;
    }

    // update the realtime time info of the event and
    // predict the times of the subsequent journey events based on this arrival
    var eventIndex = this.journeyEvents.indexOf(arrivalEvent);
    this.updateEventTimeAndMarkPreviousEventsAsCancelled(arrivalEvent, eventIndex);
    this.updateSubsequentJourneyEventTimes(arrivalEvent, eventIndex);
  }

  private void updateEventTimesDueToDepartureFromPoint() {
    // the last event of a journey is always an arrival event. as we only want to look at pairs of events
    // (arrival and departure), we need to skip the last event (only an arrival event) and the first event
    // (only a departure event). also the loop only looks at the departure event indexes, therefore we skip
    // every second index as it would be an arrival event
    JourneyEventEntity departureEvent = null;
    var lastDepartureIndex = this.journeyEvents.size() - 2;
    for (var index = lastDepartureIndex; index >= 2; index -= 2) {
      var currentEvent = this.journeyEvents.get(index);
      var arrivalEvent = this.journeyEvents.get(index - 1);
      if (arrivalEvent.getRealtimeTimeType() == JourneyTimeType.REAL
        && currentEvent.getRealtimeTimeType() != JourneyTimeType.REAL) {
        departureEvent = currentEvent;
        break;
      }
    }

    if (departureEvent == null) {
      // assume that the journey is at the first playable point along the route if no previous
      // arrival event was recorded for the journey. this check however is only relevant if the
      // first playable event is also the first event along the route, as we can record an arrival
      // at the first point using the journey position anyway
      departureEvent = this.journeyEvents.stream()
        .filter(event -> event.getEventType() == JourneyEventType.DEPARTURE)
        .filter(event -> event.getStopDescriptor().isPlayable())
        .findFirst()
        .orElse(null);
      if (departureEvent == null || departureEvent.getRealtimeTimeType() == JourneyTimeType.REAL) {
        return; // should usually not happen
      }

      // check the comment above for the check reasoning
      var firstEvent = this.journeyEvents.getFirst();
      if (firstEvent != departureEvent) {
        return;
      }
    }

    // update the realtime time info of the event and
    // predict the times of the subsequent journey events based on this arrival
    var eventIndex = this.journeyEvents.indexOf(departureEvent);
    this.updateEventTimeAndMarkPreviousEventsAsCancelled(departureEvent, eventIndex);
    this.updateSubsequentJourneyEventTimes(departureEvent, eventIndex);
  }

  private void updateEventTimeAndMarkPreviousEventsAsCancelled(@Nonnull JourneyEventEntity event, int eventIndex) {
    // update the realtime time information of the event,
    // remove the marking as cancelled as it actually happened
    event.setCancelled(false);
    event.setRealtimeTimeType(JourneyTimeType.REAL);
    event.setRealtimeTime(this.server.currentTime());
    this.markEventAsUpdated(event);

    // mark all previous unconfirmed events as cancelled as they can no longer happen
    for (var index = (eventIndex - 1); index >= 0; index--) {
      var previousEvent = this.journeyEvents.get(index);
      if (previousEvent.getRealtimeTimeType() != JourneyTimeType.REAL) {
        previousEvent.setCancelled(true);
        this.markEventAsUpdated(previousEvent);
      }
    }
  }

  private void updateSubsequentJourneyEventTimes(@Nonnull JourneyEventEntity event, int eventIndex) {
    var nextEventIndex = eventIndex + 1;
    var maxEventIndex = this.journeyEvents.size() - 1;
    if (nextEventIndex <= maxEventIndex) {
      // update all events after the current one with a predicated time. note that the event
      // is never the first in the list, so we can assume that we're always pulling event tuples
      // (arrival, departure) from the collection, aside from the last event which is only an arrival
      var lastEvent = event;
      for (var index = nextEventIndex; index <= maxEventIndex; index++) {
        var currentEvent = this.journeyEvents.get(index);
        var predictedTime = switch (currentEvent.getEventType()) {
          // at arrival events we can just add the scheduled driving time from the last departure
          // event to the current event to the realtime departure time of the last event
          case ARRIVAL -> {
            var scheduledDrive = Duration.between(lastEvent.getScheduledTime(), currentEvent.getScheduledTime());
            yield lastEvent.getRealtimeTime().plus(scheduledDrive);
          }
          // departure events have a different time than arrival events, depending on if a stopover was
          // scheduled or not. in case a stopover was scheduled, in might be partly skipped to gain
          // some time on delays again, so we need to check a bit more deeply
          case DEPARTURE -> switch (currentEvent.getStopType()) {
            // if there is no stopover scheduled for the journey then the scheduled departure time
            // is equal to the scheduled arrival time, which means we can just copy the realtime
            // arrival time as well
            case NONE -> lastEvent.getRealtimeTime();
            // technical stops can be skipped entirely in case they are not needed. if the journey is
            // late we just expect the technical stop to be completely skippable. if no delay is remaining
            // after the stop we continue to use the scheduled time from here
            case TECHNICAL -> {
              var delay = Duration.between(lastEvent.getScheduledTime(), lastEvent.getRealtimeTime());
              var stopTime = Duration.between(lastEvent.getScheduledTime(), currentEvent.getScheduledTime());
              var delayAfterStop = delay.minus(stopTime);
              if (delayAfterStop.isPositive()) {
                yield currentEvent.getScheduledTime().plus(delayAfterStop);
              } else {
                yield currentEvent.getScheduledTime();
              }
            }
            // passenger stops cannot be skipped entirely, and we expect a journey to stop at least one minute
            // at a station for passenger exchange. if the stop is scheduled to be longer (e.g. 5 minutes), we
            // just remove 4 minutes of delay from the journey. if no delay is remaining after the stop we
            // continue to use the scheduled time from here
            case PASSENGER -> {
              var delay = Duration.between(lastEvent.getScheduledTime(), lastEvent.getRealtimeTime());
              var stopTime = Duration.between(lastEvent.getScheduledTime(), currentEvent.getScheduledTime());
              var skippableStopTime = stopTime.minusMinutes(1);
              if (skippableStopTime.isPositive()) {
                // stop time is scheduled to be more than one minute, check if we can
                //  1. just decrease the overall delay by of the journey
                //  2. completely eliminate the remaining delay of the journey
                var delayAfterStop = delay.minus(skippableStopTime);
                if (delayAfterStop.isPositive()) {
                  yield currentEvent.getScheduledTime().plus(delayAfterStop);
                } else {
                  yield currentEvent.getScheduledTime();
                }
              } else {
                // stop time is scheduled to be less than one minute, so there really is nothing to
                // catch up here, just add the scheduled stop time to the confirmed arrival time. however,
                // if the journey arrived too early but only has a short stop it cannot depart early
                var timeWithStop = lastEvent.getRealtimeTime().plus(stopTime);
                if (timeWithStop.isBefore(currentEvent.getScheduledTime())) {
                  yield currentEvent.getScheduledTime();
                } else {
                  yield timeWithStop;
                }
              }
            }
          };
        };

        // ensure that the event is not marked as cancelled as
        // this method predicts the time when an event might happen
        // and is called due to an event happening. therefore the
        // subsequent event will (maybe) happen as well
        var eventWasCancelled = currentEvent.isCancelled();
        currentEvent.setCancelled(false);

        // update the realtime time of the current event to the constructed prediction
        var normalizedPredictedTime = this.roundAndTruncatePredictedTime(predictedTime);
        currentEvent.setRealtimeTime(normalizedPredictedTime);
        currentEvent.setRealtimeTimeType(JourneyTimeType.PREDICTION);
        this.markEventAsUpdated(currentEvent);
        lastEvent = currentEvent;

        // if the scheduled time is now equal to the realtime time again we can
        // stop making predictions for the following stops as this wouldn't make
        // any difference to the currently present time anyway
        if (!eventWasCancelled && predictedTime.isEqual(currentEvent.getScheduledTime())) {
          break;
        }
      }
    }
  }

  private @Nonnull OffsetDateTime roundAndTruncatePredictedTime(@Nonnull OffsetDateTime time) {
    var truncated = time.truncatedTo(ChronoUnit.SECONDS);
    if (truncated.getSecond() >= 30) {
      // round the predicted time up as we're after the half minute mark
      // :30 - :59 (both inclusive) should be rounded to :00 of the next minute
      return truncated.plusMinutes(1).withSecond(0);
    } else {
      // round the predicted time down as we're before the half minute mark
      // :00 - :29 (both inclusive) should be rounded to :00 of the current minute
      return truncated.withSecond(0);
    }
  }

  private @Nonnull Pair<JourneyEventEntity, JourneyEventEntity> createJitAdditionalEvent(
    @Nonnull SimRailPoint currentPoint,
    @Nonnull JourneyEventEntity previousEvent
  ) {
    // build information about the current stop, assume that this new stop is playable
    // if the previous stop was playable as well
    var previousStop = previousEvent.getStopDescriptor();
    var currentStop = new JourneyStopDescriptor(
      currentPoint.getId(),
      currentPoint.getName(),
      previousStop.isPlayable());

    // build the transport info
    var maxSpeedAtCurrentPoint = this.journeyEvents.stream()
      .map(event -> event.getTransport().getMaxSpeed())
      .max(Integer::compare)
      .map(maxJourneySpeed -> Math.min(maxJourneySpeed, currentPoint.getMaxSpeed()))
      .orElse(currentPoint.getMaxSpeed());
    var previousTransport = previousEvent.getTransport();
    var currentTransport = new JourneyTransport(
      previousTransport.getCategory(),
      previousTransport.getNumber(),
      previousTransport.getType(),
      previousTransport.getLine(),
      previousTransport.getLabel(),
      maxSpeedAtCurrentPoint);

    // create the jit arrival and departure event
    var jitArrivalEvent = this.createJitAdditionalEvent(
      previousEvent.getEventIndex() + 1,
      previousEvent.getJourneyId(),
      previousEvent.getId(),
      JourneyEventType.ARRIVAL,
      currentStop,
      currentTransport);
    var jitDepartureEvent = this.createJitAdditionalEvent(
      previousEvent.getEventIndex() + 2,
      previousEvent.getJourneyId(),
      previousEvent.getId(),
      JourneyEventType.DEPARTURE,
      currentStop,
      currentTransport);
    return Pair.of(jitArrivalEvent, jitDepartureEvent);
  }

  private @Nonnull JourneyEventEntity createJitAdditionalEvent(
    int index,
    @Nonnull UUID journeyId,
    @Nonnull UUID previousEventId,
    @Nonnull JourneyEventType type,
    @Nonnull JourneyStopDescriptor stop,
    @Nonnull JourneyTransport transport
  ) {
    var eventId = this.journeyEventIdFactory.create(journeyId.toString() + stop.getId() + previousEventId + type);
    var journeyEvent = new JourneyEventEntity();
    journeyEvent.setId(eventId);
    journeyEvent.setEventType(type);
    journeyEvent.setAdditional(true);
    journeyEvent.setEventIndex(index);
    journeyEvent.setJourneyId(journeyId);
    journeyEvent.setTransport(transport);
    journeyEvent.setStopDescriptor(stop);
    journeyEvent.setStopType(JourneyStopType.NONE);

    // get the time information for the event, as this method is used to build events
    // when a journey arrived at an unknown point, the arrival events can directly
    // use the REAL time type, while departure events will update when the journey left
    var realtimeTime = this.server.currentTime();
    var normalizedScheduledTime = this.roundAndTruncatePredictedTime(realtimeTime);
    journeyEvent.setScheduledTime(normalizedScheduledTime);
    switch (type) {
      case ARRIVAL -> {
        journeyEvent.setRealtimeTime(realtimeTime);
        journeyEvent.setRealtimeTimeType(JourneyTimeType.REAL);
      }
      case DEPARTURE -> {
        journeyEvent.setRealtimeTime(normalizedScheduledTime);
        journeyEvent.setRealtimeTimeType(JourneyTimeType.PREDICTION);
      }
    }

    return journeyEvent;
  }

  /**
   * Updates all events after the last confirmed event and marks them as cancelled if the journey was removed (e.g.
   * because it reached the end of the playable map, derailed, ...).
   */
  public void updateEventsDueToRemoval() {
    for (var index = this.journeyEvents.size() - 1; index >= 0; index--) {
      // get the current event, break in case it was confirmed and happened or if it was already cancelled
      var currentEvent = this.journeyEvents.get(index);
      if (currentEvent.getRealtimeTimeType() == JourneyTimeType.REAL || currentEvent.isCancelled()) {
        break;
      }

      // cancel the event as it cannot happen anymore
      currentEvent.setCancelled(true);
      this.markEventAsUpdated(currentEvent);
    }
  }

  private void markEventAsUpdated(@Nonnull JourneyEventEntity event) {
    this.updatedJourneyEvents.putIfAbsent(event.getId(), event);
  }

  public @Nonnull Collection<JourneyEventEntity> getUpdatedJourneyEvents() {
    return this.updatedJourneyEvents.values();
  }

  @Component
  static final class Factory {

    private final SimRailPointProvider pointProvider;
    private final UuidV5Factory journeyEventIdFactory;
    private final PlatformSignalProvider signalProvider;

    @Autowired
    public Factory(@Nonnull SimRailPointProvider pointProvider, @Nonnull PlatformSignalProvider signalProvider) {
      this.pointProvider = pointProvider;
      this.signalProvider = signalProvider;
      this.journeyEventIdFactory = new UuidV5Factory(JourneyEventEntity.ID_NAMESPACE);
    }

    public @Nonnull JourneyEventRealtimeUpdater create(
      @Nonnull JourneyEntity journey,
      @Nonnull SimRailServerDescriptor server,
      @Nonnull List<JourneyEventEntity> journeyEvents
    ) {
      return new JourneyEventRealtimeUpdater(
        journey,
        server,
        journeyEvents,
        this.pointProvider,
        this.journeyEventIdFactory,
        this.signalProvider);
    }
  }
}
