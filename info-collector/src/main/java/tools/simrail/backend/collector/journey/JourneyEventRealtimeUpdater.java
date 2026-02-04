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

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.simrail.backend.common.journey.JourneyEntity;
import tools.simrail.backend.common.journey.JourneyEventEntity;
import tools.simrail.backend.common.journey.JourneyEventRepository;
import tools.simrail.backend.common.journey.JourneyEventType;
import tools.simrail.backend.common.journey.JourneyPassengerStopInfo;
import tools.simrail.backend.common.journey.JourneyStopType;
import tools.simrail.backend.common.journey.JourneyTimeType;
import tools.simrail.backend.common.journey.JourneyTransport;
import tools.simrail.backend.common.point.SimRailPoint;
import tools.simrail.backend.common.signal.PlatformSignalProvider;

@Component
final class JourneyEventRealtimeUpdater {

  private static final Logger LOGGER = LoggerFactory.getLogger(JourneyEventRealtimeUpdater.class);

  private final JourneyIdService journeyIdService;
  private final PlatformSignalProvider signalProvider;
  private final TransactionTemplate transactionTemplate;
  private final JourneyEventRepository journeyEventRepository;

  private final Timer eventUpdateTimer;
  private final BlockingQueue<JourneyEventUpdateRequest> pendingUpdates;

  JourneyEventRealtimeUpdater(
    @NonNull JourneyIdService journeyIdService,
    @NonNull PlatformSignalProvider signalProvider,
    @NonNull TransactionTemplate transactionTemplate,
    @NonNull JourneyEventRepository journeyEventRepository,
    @NonNull MeterRegistry meterRegistry
  ) {
    this.journeyIdService = journeyIdService;
    this.signalProvider = signalProvider;
    this.transactionTemplate = transactionTemplate;
    this.journeyEventRepository = journeyEventRepository;

    this.pendingUpdates = new LinkedBlockingQueue<>();

    // init metrics
    this.eventUpdateTimer = Timer.builder("journey_event_update_time_seconds")
      .description("Elapsed seconds while processing event updates for a single journey")
      .register(meterRegistry);
    Gauge.builder("journey_event_update_queue_size", this.pendingUpdates::size)
      .description("The updates that are pending processing by the event realtime updater")
      .register(meterRegistry);
  }

  @PostConstruct
  public void startEventUpdates() {
    Thread.ofPlatform()
      .daemon()
      .stackSize(32)
      .name("Journey-Event-RT-Updater")
      .start(() -> {
        var currentThread = Thread.currentThread();
        while (!currentThread.isInterrupted()) {
          try {
            var nextRequest = this.pendingUpdates.take();
            for (var attempt = 1; attempt <= 5; attempt++) {
              var sample = Timer.start();
              try {
                LOGGER.debug("Processing journey event update request: {} (attempt: {})", nextRequest, attempt);
                this.transactionTemplate.executeWithoutResult(_ -> this.processUpdateRequest(nextRequest));
                break; // update succeeded
              } catch (TransientDataAccessException _) {
                // transient exception: operation can succeed on next attempt, just try again
              } finally {
                sample.stop(this.eventUpdateTimer);
              }
            }
          } catch (InterruptedException _) {
            // interrupt signal received, exit
            Thread.currentThread().interrupt();
            break;
          } catch (Exception exception) {
            LOGGER.error("Caught exception while processing event realtime updates", exception);
          }
        }
      });
  }

  public void requestEventUpdate(@NonNull JourneyEventUpdateRequest request) {
    this.pendingUpdates.add(request);
  }

  // this method runs in a transaction
  private void processUpdateRequest(@NonNull JourneyEventUpdateRequest request) {
    var events = this.journeyEventRepository.findAllByJourneyId(request.journeyId());
    events.sort(JourneyEventEntity.BY_EVENT_INDEX_COMPARATOR);
    if (events.isEmpty()) {
      return;
    }

    var eventUpdated = switch (request) {
      case JourneyEventUpdateRequest.ForPointChange fpc -> {
        // the journey departed from a point or arrived at a point or both.
        // first, process the departure, then process the arrival at the next point
        // to keep the event time updating in order
        var anyUpdated = fpc.departedFromPoint() && this.updateEventsDueToDeparture(fpc, events);
        anyUpdated |= fpc.arrivedAtPoint() && this.updateEventsDueToArrival(fpc, events);
        yield anyUpdated;
      }
      case JourneyEventUpdateRequest.ForSignalUpdate fsu ->
        // the journey is currently at a point but the signal in front
        // of the journey changed. this might change the platform where
        // the journey actually stopped at
        this.updateEventsDueToSignalChange(fsu, events);
      case JourneyEventUpdateRequest.ForRemoval _ ->
        // the journey was removed from the server, we need to mark all
        // subsequent journey events as canceled
        this.updateEventsDueToRemoval(events);
    };

    // re-persist the journey events if some were updated
    if (eventUpdated) {
      this.journeyEventRepository.saveAll(events);
    }
  }

  private boolean updateEventsDueToRemoval(@NonNull List<JourneyEventEntity> events) {
    // find the last event that was confirmed
    var lastConfirmedEventIdx = -1;
    var eventCount = events.size();
    for (var index = eventCount - 1; index >= 0; index--) {
      var event = events.get(index);
      if (event.getRealtimeTimeType() == JourneyTimeType.REAL) {
        lastConfirmedEventIdx = index;
        break;
      }
    }

    // no events that weren't confirmed before, nothing to do
    if (lastConfirmedEventIdx == -1 || lastConfirmedEventIdx == (eventCount - 1)) {
      return false;
    }

    // mark all events after the last confirmed one canceled
    for (var index = lastConfirmedEventIdx + 1; index < eventCount; index++) {
      var event = events.get(index);
      event.setCancelled(true);
    }
    return true;
  }

  private boolean updateEventsDueToSignalChange(
    JourneyEventUpdateRequest.@NonNull ForSignalUpdate request,
    @NonNull List<JourneyEventEntity> events
  ) {
    // find the events that are associated with the point where the journey is currently at.
    // we filter for passenger stops as technically additional events for the same points might get
    // added multiple times, but they never have a passenger stop scheduled
    var pointId = request.currentPoint().getId();
    var eventOfPointByType = events.stream()
      .filter(event -> event.getPointId().equals(pointId))
      .filter(event -> event.getStopType() == JourneyStopType.PASSENGER)
      .collect(Collectors.toMap(JourneyEventEntity::getEventType, Function.identity()));
    var arrivalEvent = eventOfPointByType.get(JourneyEventType.ARRIVAL);
    if (arrivalEvent == null) {
      return false;
    }

    // find the information about the signal that the journey is currently at
    var signalInfo = this.signalProvider.findSignalInfo(pointId, request.nextSignalId()).orElse(null);
    if (signalInfo == null) {
      return false;
    }

    var realtimeStopInfo = new JourneyPassengerStopInfo(signalInfo.getTrack(), signalInfo.getPlatform());
    arrivalEvent.setRealtimePassengerStopInfo(realtimeStopInfo);

    // check if an associated departure event exists for the journey, update the signal info of it too
    var departureEvent = eventOfPointByType.get(JourneyEventType.DEPARTURE);
    if (departureEvent != null) {
      departureEvent.setRealtimePassengerStopInfo(realtimeStopInfo);
    }

    return true;
  }

  private boolean updateEventsDueToArrival(
    JourneyEventUpdateRequest.@NonNull ForPointChange request,
    @NonNull List<JourneyEventEntity> events
  ) {
    var currentPoint = request.currentPoint();
    Objects.requireNonNull(currentPoint, "BUG! updateEventsDueToArrival() called, but not an arrival at point");

    // find the scheduled arrival event for the point or create an additional JIT event if required
    var arrivalEventOfPoint = events.stream()
      .filter(event -> event.getEventType() == JourneyEventType.ARRIVAL)
      .filter(event -> event.getPointId().equals(currentPoint.getId()))
      .findFirst()
      .orElseGet(() -> {
        // journey was not scheduled to stop at the point, we possibly want to add a JIT
        // event for the journey and the point. however, we don't do this for stopping
        // points, as journeys can pass them on the scheduled route without them actually
        // appearing in the timetable - we don't want to do it either
        var pointPrefix = currentPoint.getPrefix();
        if (pointPrefix == null) {
          return null;
        }

        // find the last confirmed event of the journey. if the event is missing, this likely
        // indicates that the journey arrived at the first point, we don't want to add an
        // event for that case, the known departure event is sufficient
        var lastConfirmedDepartureEvent = events.reversed().stream()
          .filter(event -> event.getEventType() == JourneyEventType.DEPARTURE)
          .filter(event -> event.getRealtimeTimeType() == JourneyTimeType.REAL)
          .findFirst()
          .orElse(null);
        if (lastConfirmedDepartureEvent == null) {
          return null;
        }

        // if the last confirmed departure is at the point where the journey now arrived, we
        // don't want to register an additional event for that - this can possibly be caused
        // by a player reversing or by the train respawning after a server restart
        if (lastConfirmedDepartureEvent.getPointId().equals(currentPoint.getId())) {
          return null;
        }

        var jitEventPair = this.createJitAdditionalEvent(
          currentPoint,
          lastConfirmedDepartureEvent,
          events,
          request.serverTime());
        events.add(jitEventPair.getFirst()); // arrival event
        events.add(jitEventPair.getSecond()); // departure event
        events.sort(JourneyEventEntity.BY_EVENT_INDEX_COMPARATOR); // ensure stable ordering of list
        return jitEventPair.getFirst();
      });
    if (arrivalEventOfPoint == null || arrivalEventOfPoint.getRealtimeTimeType() == JourneyTimeType.REAL) {
      return false;
    }

    // compute the index where the journey is located in the list once
    var eventIndex = events.indexOf(arrivalEventOfPoint);
    var hasMoreEvents = events.size() > (eventIndex + 1);

    // update the information where the journey actually stopped
    var nextSignal = request.nextSignal();
    if (arrivalEventOfPoint.getStopType() == JourneyStopType.PASSENGER && nextSignal != null) {
      this.signalProvider.findSignalInfo(currentPoint.getId(), nextSignal.getName()).ifPresent(signal -> {
        var realtimeStopInfo = new JourneyPassengerStopInfo(signal.getTrack(), signal.getPlatform());
        arrivalEventOfPoint.setRealtimePassengerStopInfo(realtimeStopInfo);

        // check if an associated departure event exists for the journey, update the signal info of it too
        if (hasMoreEvents) {
          var departureEvent = events.get(eventIndex + 1);
          departureEvent.setRealtimePassengerStopInfo(realtimeStopInfo);
        }
      });
    }

    // update the realtime time info of the event and
    // predict the times of the subsequent journey events based on this arrival
    this.updateEventTimeAndMarkPreviousEventsAsCancelled(eventIndex, request.serverTime(), events);
    this.updateSubsequentJourneyEventTimes(eventIndex, events);
    return true;
  }

  private boolean updateEventsDueToDeparture(
    JourneyEventUpdateRequest.@NonNull ForPointChange request,
    @NonNull List<JourneyEventEntity> events
  ) {
    var prevPointId = request.prevPointId();
    Objects.requireNonNull(prevPointId, "BUG! updateEventTimesDueToDepartureFromPoint() called without prev point");

    // find the scheduled departure event for the point
    var departureEventOfPoint = events.stream()
      .filter(event -> event.getEventType() == JourneyEventType.DEPARTURE)
      .filter(event -> event.getPointId().equals(prevPointId))
      .findFirst()
      .orElse(null);
    if (departureEventOfPoint == null) {
      return false;
    }

    // update the realtime time information of the event and
    // predict the times of the subsequent journey events based on this departure
    var eventIndex = events.indexOf(departureEventOfPoint);
    this.updateEventTimeAndMarkPreviousEventsAsCancelled(eventIndex, request.serverTime(), events);
    this.updateSubsequentJourneyEventTimes(eventIndex, events);
    return true;
  }

  private void updateEventTimeAndMarkPreviousEventsAsCancelled(
    int eventIndex,
    @NonNull LocalDateTime serverTime,
    @NonNull List<JourneyEventEntity> events
  ) {
    // update the realtime time information of the event,
    // remove the marking as canceled as it actually happened
    var event = events.get(eventIndex);
    event.setCancelled(false);
    event.setRealtimeTime(serverTime);
    event.setRealtimeTimeType(JourneyTimeType.REAL);

    // mark all previous unconfirmed events as canceled as they can no longer happen
    for (var index = (eventIndex - 1); index >= 0; index--) {
      var previousEvent = events.get(index);
      if (previousEvent.getRealtimeTimeType() != JourneyTimeType.REAL) {
        previousEvent.setCancelled(true);
      }
    }
  }

  private void updateSubsequentJourneyEventTimes(int eventIndex, @NonNull List<JourneyEventEntity> events) {
    var nextEventIndex = eventIndex + 1;
    var maxEventIndex = events.size() - 1;
    if (nextEventIndex <= maxEventIndex) {
      // update all events after the current one with a predicated time. note that the event
      // is never the first in the list, so we can assume that we're always pulling event tuples
      // (arrival, departure) from the collection, aside from the last event which is only an arrival
      var lastEvent = events.get(eventIndex);
      for (var index = nextEventIndex; index <= maxEventIndex; index++) {
        var currentEvent = events.get(index);
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

        // ensure that the event is not marked as canceled as
        // this method predicts the time when an event might happen
        // and is called due to an event happening. therefore the
        // subsequent event will (maybe) happen as well
        var eventWasCancelled = currentEvent.isCancelled();
        currentEvent.setCancelled(false);

        // update the realtime time of the current event to the constructed prediction
        var normalizedPredictedTime = this.roundAndTruncatePredictedTime(predictedTime);
        currentEvent.setRealtimeTime(normalizedPredictedTime);
        currentEvent.setRealtimeTimeType(JourneyTimeType.PREDICTION);
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

  private @NonNull LocalDateTime roundAndTruncatePredictedTime(@NonNull LocalDateTime time) {
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

  private @NonNull Pair<JourneyEventEntity, JourneyEventEntity> createJitAdditionalEvent(
    @NonNull SimRailPoint currPoint,
    @NonNull JourneyEventEntity prevEvent,
    @NonNull List<JourneyEventEntity> events,
    @NonNull LocalDateTime serverTime
  ) {
    // generate a scheduled time info based on the current server time
    var eventScheduledTime = this.roundAndTruncatePredictedTime(serverTime);

    // build the transport info
    var maxSpeedAtCurrentPoint = events.stream()
      .map(event -> event.getTransport().getMaxSpeed())
      .max(Integer::compare)
      .map(maxJourneySpeed -> Math.min(maxJourneySpeed, currPoint.getMaxSpeed()))
      .orElse(currPoint.getMaxSpeed());
    var previousTransport = prevEvent.getTransport();
    var currentTransport = new JourneyTransport(
      previousTransport.getCategory(),
      previousTransport.getNumber(),
      previousTransport.getType(),
      previousTransport.getLine(),
      previousTransport.getLabel(),
      maxSpeedAtCurrentPoint);

    // create the jit arrival and departure event
    var jitArrivalEvent = this.createJitAdditionalEvent(
      prevEvent.getEventIndex() + 1,
      currPoint.getId(),
      prevEvent.getJourney(),
      JourneyEventType.ARRIVAL,
      currentTransport,
      eventScheduledTime,
      prevEvent);
    var jitDepartureEvent = this.createJitAdditionalEvent(
      prevEvent.getEventIndex() + 2,
      currPoint.getId(),
      prevEvent.getJourney(),
      JourneyEventType.DEPARTURE,
      currentTransport,
      eventScheduledTime,
      prevEvent);
    return Pair.of(jitArrivalEvent, jitDepartureEvent);
  }

  private @NonNull JourneyEventEntity createJitAdditionalEvent(
    int index,
    @NonNull UUID pointId,
    @NonNull JourneyEntity journey,
    @NonNull JourneyEventType type,
    @NonNull JourneyTransport transport,
    @NonNull LocalDateTime scheduledTime,
    @NonNull JourneyEventEntity prevEvent
  ) {
    @SuppressWarnings("DataFlowIssue") // journey/prev event id is not null here
    var eventId = this.journeyIdService.generateJitJourneyEventId(journey.getId(), pointId, prevEvent.getId(), type);

    var journeyEvent = new JourneyEventEntity();
    journeyEvent.setNew(true);
    journeyEvent.setId(eventId);
    journeyEvent.setJourney(journey);
    journeyEvent.setEventIndex(index);
    journeyEvent.setEventType(type);
    journeyEvent.setPointId(pointId);
    journeyEvent.setTransport(transport);
    journeyEvent.setAdditional(true);
    journeyEvent.setStopType(JourneyStopType.NONE);
    journeyEvent.setScheduledTime(scheduledTime);
    journeyEvent.setRealtimeTime(scheduledTime);
    journeyEvent.setRealtimeTimeType(JourneyTimeType.PREDICTION);
    journeyEvent.setInPlayableBorder(prevEvent.isInPlayableBorder());

    return journeyEvent;
  }
}
