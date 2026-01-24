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

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.simrail.backend.collector.util.PerServerGauge;
import tools.simrail.backend.collector.server.SimRailServerDescriptor;
import tools.simrail.backend.collector.server.SimRailServerService;
import tools.simrail.backend.common.border.MapBorderPointProvider;
import tools.simrail.backend.common.journey.JourneyEntity;
import tools.simrail.backend.common.journey.JourneyEventEntity;
import tools.simrail.backend.common.journey.JourneyEventType;
import tools.simrail.backend.common.journey.JourneyPassengerStopInfo;
import tools.simrail.backend.common.journey.JourneyStopType;
import tools.simrail.backend.common.journey.JourneyTimeType;
import tools.simrail.backend.common.journey.JourneyTransport;
import tools.simrail.backend.common.journey.JourneyTransportType;
import tools.simrail.backend.common.point.SimRailPointProvider;
import tools.simrail.backend.common.util.RomanNumberConverter;
import tools.simrail.backend.common.util.UuidV5Factory;
import tools.simrail.backend.external.sraws.SimRailAwsApiClient;
import tools.simrail.backend.external.sraws.model.SimRailAwsTimetableEntry;
import tools.simrail.backend.external.sraws.model.SimRailAwsTrainRun;

@Component
class SimRailServerTimetableCollector {

  private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");

  private final UuidV5Factory journeyIdFactory;
  private final UuidV5Factory journeyEventIdFactory;

  private final SimRailAwsApiClient awsApiClient;
  private final SimRailPointProvider pointProvider;
  private final SimRailServerService serverService;
  private final CollectorJourneyService journeyService;
  private final MapBorderPointProvider borderPointProvider;

  private final PerServerGauge collectedJourneyCounter;
  private final Meter.MeterProvider<Timer> collectionDurationTimer;

  @Autowired
  public SimRailServerTimetableCollector(
    @NonNull SimRailAwsApiClient awsApiClient,
    @NonNull SimRailPointProvider pointProvider,
    @NonNull SimRailServerService serverService,
    @NonNull CollectorJourneyService journeyService,
    @NonNull MapBorderPointProvider borderPointProvider,
    @NonNull @Qualifier("timetable_collected_runs_total") PerServerGauge collectedJourneyCounter,
    @Qualifier("timetable_run_collect_duration") Meter.@NonNull MeterProvider<Timer> collectionDurationTimer
  ) {
    this.awsApiClient = awsApiClient;
    this.pointProvider = pointProvider;
    this.serverService = serverService;
    this.journeyService = journeyService;
    this.borderPointProvider = borderPointProvider;

    this.journeyIdFactory = new UuidV5Factory(JourneyEntity.ID_NAMESPACE);
    this.journeyEventIdFactory = new UuidV5Factory(JourneyEventEntity.ID_NAMESPACE);

    this.collectedJourneyCounter = collectedJourneyCounter;
    this.collectionDurationTimer = collectionDurationTimer;
  }

  @Scheduled(
    initialDelay = 30,
    fixedRate = 15 * 60,
    timeUnit = TimeUnit.SECONDS,
    scheduler = "timetable_collect_scheduler"
  )
  public void collectServerTimetables() {
    var servers = this.serverService.getServers();
    for (var server : servers) {
      // get the trains running on the server and their associated run ids
      var span = Timer.start();
      var trainRuns = this.awsApiClient.getTrainRuns(server.code());
      var journeysInTimetable = trainRuns.stream()
        .map(run -> this.collectJourney(server, run))
        .filter(Objects::nonNull)
        .toList();
      this.journeyService.persistScheduledUpdatedJourneys(journeysInTimetable);

      // print information about the collection run
      this.collectedJourneyCounter.setValue(server, trainRuns.size());
      span.stop(this.collectionDurationTimer.withTag("server_code", server.code()));
    }
  }

  @Nullable
  private JourneyEntity collectJourney(@NonNull SimRailServerDescriptor server, @NonNull SimRailAwsTrainRun run) {
    // timetable can sometimes be empty (probably a testing thing), just ignore these journeys
    var originalTimetable = run.getTimetable();
    if (originalTimetable.isEmpty()) {
      return null;
    }

    var runId = run.getRunId();
    var journeyId = this.generateJourneyId(server.id(), runId);

    // construct the base journey data
    var journey = new JourneyEntity();
    journey.setNew(true);
    journey.setId(journeyId);
    journey.setForeignRunId(runId);
    journey.setServerId(server.id());

    // extract the line information from the display name of the train
    var cleanedTrainName = WHITESPACE_PATTERN.matcher(run.getTrainDisplayName()).replaceAll("");
    var trainNameParts = List.of(cleanedTrainName.split("-"));
    var trainLine = trainNameParts.stream()
      .skip(1) // skip the first entry as it is always the train category
      .filter(part -> !part.startsWith("\""))
      .findFirst()
      .orElse(null);
    var trainLabel = trainNameParts.stream()
      .skip(1) // skip the first entry as it is always the train category
      .filter(part -> part.startsWith("\"") && part.endsWith("\""))
      .findFirst()
      .map(label -> label.substring(1, label.length() - 1))
      .orElse(null);

    // create events for all timetable entries
    var inBorder = false; // keeps track if the journey is within the playable border
    var timetable = this.fixupTimetable(originalTimetable); // timetable of the journey
    var lastTimetableIndex = timetable.size() - 1; // the last index of the timetable
    JourneyEventEntity previousEvent = null; // the previous constructed event
    var events = new ArrayList<JourneyEventEntity>(); // all constructed events
    for (var index = 0; index < timetable.size(); index++) {
      // update the current status if the journey is within the playable border
      var timetableEntry = timetable.get(index);
      var wasInBorder = inBorder; // keeps track if the journey was in border before the border check
      var borderPoint = this.borderPointProvider.findMapBorderPoint(timetableEntry.getPointId()).orElse(null);
      if (borderPoint != null && !inBorder) {
        // if we are currently not within the map border we need to check if the journey moved into the
        // map border before recording the events as they should be marked as in-border at the map border
        // points. note that the journey only moves into the border if there are no required next points or
        // the required next points are met. example route where this is needed:
        // Podlesie - Koniecpol - Żelisławice - Włoszczowa Północ. both Koniecpol and Żelisławice are border
        // points, so Koniecpol must validate that the journey actually moves into the border or else Żelisławice
        // will instantly mark the journey out-of-border again
        var requiredNextPoints = borderPoint.getRequiredNextPoints();
        if (requiredNextPoints != null) {
          if (index != lastTimetableIndex) {
            var next = timetable.get(index + 1);
            if (requiredNextPoints.contains(next.getPointId())) {
              inBorder = true;
            }
          }
        } else {
          inBorder = true;
        }
      }

      // get the point associated with the event, if the point is not registered
      // we deem it irrelevant and don't want an event for the point
      var eventPoint = this.pointProvider.findPointByPointId(timetableEntry.getPointId()).orElse(null);
      if (eventPoint == null) {
        if (borderPoint != null && wasInBorder) {
          // if the current event was at a border point, and we were in border before the check
          // (so the check didn't move us into the map border) the in border state resets here
          // to keep the current event still marked as in-border
          inBorder = false;
        }

        continue;
      }

      // construct the arrival event for the entry, if the event is not the first along the route
      if (index != 0) {
        var prev = previousEvent;
        previousEvent = this.createAndRegisterJourneyEvent(
          journey,
          trainLine,
          trainLabel,
          JourneyEventType.ARRIVAL,
          previousEvent,
          timetableEntry,
          events);
        if (previousEvent != null && prev != previousEvent) {
          previousEvent.setInPlayableBorder(inBorder);
        }
      }

      // construct the departure event for the entry, if the event is not the last along the route
      if (index != lastTimetableIndex) {
        var prev = previousEvent;
        previousEvent = this.createAndRegisterJourneyEvent(
          journey,
          trainLine,
          trainLabel,
          JourneyEventType.DEPARTURE,
          previousEvent,
          timetableEntry,
          events);
        if (previousEvent != null && prev != previousEvent) {
          previousEvent.setInPlayableBorder(inBorder);
        }
      }

      if (events.size() > 1 && previousEvent != null && previousEvent.getEventType() == JourneyEventType.DEPARTURE) {
        var arrivalEvent = events.get(events.size() - 2);
        var overlayDuration = Duration.between(arrivalEvent.getScheduledTime(), previousEvent.getScheduledTime())
          .abs()
          .truncatedTo(ChronoUnit.SECONDS);
        if (previousEvent.getStopType() == JourneyStopType.NONE && overlayDuration.toMinutes() >= 5) {
          // add a technical stop for the journey at the event, the journey stays at
          // the point for at least 5 minutes, this should be some stop, not a drive-trough
          arrivalEvent.setStopType(JourneyStopType.TECHNICAL);
          previousEvent.setStopType(JourneyStopType.TECHNICAL);
        } else if (previousEvent.getStopType() == JourneyStopType.PASSENGER && overlayDuration.isZero()) {
          // if the journey has a passenger stop scheduled but no overlay set for
          // the station, schedule an overlay of 30 seconds for the stations to allow
          // for passenger change. apparently the SimRail backend can currently not
          // handle overlay that's only seconds long, therefore all times are rounded
          // to a full minute
          var scheduledTime = previousEvent.getScheduledTime();
          var timeWithOverlay = scheduledTime.plusSeconds(30);
          previousEvent.setScheduledTime(timeWithOverlay);
          previousEvent.setRealtimeTime(timeWithOverlay);
        } else if (overlayDuration.isZero()) {
          // remove the stop type at the point in case there is no overlay time
          // scheduled for the journey (basically a stop without a stop)
          arrivalEvent.setStopType(JourneyStopType.NONE);
          previousEvent.setStopType(JourneyStopType.NONE);
        }
      }

      if (borderPoint != null && wasInBorder) {
        // if the current event was at a border point, and we were in border before the check
        // (so the check didn't move us into the map border) the in border state resets here
        // to keep the current event still marked as in-border
        inBorder = false;
      }
    }

    // drop the first event if it is an arrival event - journeys can only depart
    // from the first station along their route, never arrive there
    // this case should usually not happen (due to a previous check in the for loop),
    // however it can happen if there is no point registered for the timetable
    // entry, and it gets skipped leaving another event as the unexpected tail
    var firstEvent = events.getFirst();
    if (firstEvent.getEventType() == JourneyEventType.ARRIVAL) {
      events.removeFirst();
      if (firstEvent.getStopType() == JourneyStopType.TECHNICAL) {
        // ensure that the first event never has a technical stop scheduled
        var newFirst = events.getFirst();
        newFirst.setStopType(JourneyStopType.NONE);
      }
    }

    // drop the last event if it is a departure event - journeys cannot depart from
    // a station as their last event, they have to change to another train to depart.
    var lastEvent = events.getLast();
    if (lastEvent.getEventType() == JourneyEventType.DEPARTURE) {
      events.removeLast();
      if (lastEvent.getStopType() == JourneyStopType.TECHNICAL) {
        // ensure that the last event never has a technical stop scheduled
        var newLast = events.getLast();
        newLast.setStopType(JourneyStopType.NONE);
      }
    }

    // set the event index for the recorded events. this ensures that the
    // event ordering is always according to the events that are actually
    // stored, especially that the first event uses the index 0
    for (var eventIndex = 0; eventIndex < events.size(); eventIndex++) {
      var event = events.get(eventIndex);
      if (eventIndex == 0) {
        // first event must use the index 0
        event.setEventIndex(0);
      } else {
        switch (event.getEventType()) {
          case ARRIVAL -> {
            // arrival event just use the actual index * 100 to leave space for additional events
            var index = eventIndex * 100;
            event.setEventIndex(index);
          }
          case DEPARTURE -> {
            // departure events should use the same index as the arrival event plus 1
            var arrivalEventIndex = (eventIndex - 1) * 100;
            event.setEventIndex(arrivalEventIndex + 1);
          }
        }
      }
    }

    // copy the events into the journey
    var movedEvents = new LinkedHashSet<>(events);
    journey.setEvents(movedEvents);
    return journey;
  }

  private @Nullable JourneyEventEntity createAndRegisterJourneyEvent(
    @NonNull JourneyEntity journey,
    @Nullable String trainLine,
    @Nullable String trainLabel,
    @NonNull JourneyEventType eventType,
    @Nullable JourneyEventEntity previousEvent,
    @NonNull SimRailAwsTimetableEntry timetableEntry,
    @NonNull List<JourneyEventEntity> journeyEvents
  ) {
    var previousTime = previousEvent == null ? null : previousEvent.getScheduledTime();
    var event = this.createJourneyEvent(journey, trainLine, trainLabel, eventType, previousTime, timetableEntry);
    if (event != null) {
      journeyEvents.add(event);
      return event;
    } else {
      return previousEvent;
    }
  }

  private @Nullable JourneyEventEntity createJourneyEvent(
    @NonNull JourneyEntity journey,
    @Nullable String trainLine,
    @Nullable String trainLabel,
    @NonNull JourneyEventType eventType,
    @Nullable LocalDateTime previousEventTime,
    @NonNull SimRailAwsTimetableEntry timetableEntry
  ) {
    // get the point where the event is happening, return if the point is not registered
    var point = this.pointProvider.findPointByPointId(timetableEntry.getPointId()).orElse(null);
    if (point == null) {
      return null;
    }

    // extract the scheduled time of the event
    var originalScheduledTime = switch (eventType) {
      case ARRIVAL -> timetableEntry.getArrivalTime();
      case DEPARTURE -> timetableEntry.getDepartureTime();
    };
    var scheduledTime = switch (previousEventTime) {
      case LocalDateTime previousTime -> {
        // previous time is present, add the diff between the last and current event
        // to the last time to get this event time, there are some cases in which dates
        // of the provided time-date info are one day off. note that the diff might be negative
        // in case when the event times are at the dates border (e.g. prev: 23:55, curr: 00:05 yields PT-23H-50M)
        // in these cases we add a full day to the duration to fixup these issues
        var diff = Duration.between(previousTime.toLocalTime(), originalScheduledTime.toLocalTime());
        if (diff.isNegative()) {
          diff = diff.plusDays(1);
        }

        yield previousTime.plus(diff);
      }
      // no previous time present, use the information from the server and event
      case null -> originalScheduledTime;
    };

    @SuppressWarnings("DataFlowIssue") // journey id is not null here as it's not set by the persistence provider
    var journeyId = journey.getId().toString();
    var id = this.journeyEventIdFactory.create(journeyId + point.getId() + originalScheduledTime + eventType);
    var eventEntity = new JourneyEventEntity();
    eventEntity.setId(id);
    eventEntity.setNew(true);
    eventEntity.setJourney(journey);
    eventEntity.setEventType(eventType);
    eventEntity.setScheduledTime(scheduledTime);
    eventEntity.setRealtimeTime(scheduledTime);
    eventEntity.setRealtimeTimeType(JourneyTimeType.SCHEDULE);
    eventEntity.setPointId(point.getId());

    // add information about the stop type at the event
    var stopType = timetableEntry.getStopType();
    var scheduledTrack = timetableEntry.getStopTrack();
    var scheduledPlatform = timetableEntry.getStopPlatform();
    if (stopType == SimRailAwsTimetableEntry.StopType.PH && scheduledTrack != null && scheduledPlatform != null) {
      var platformNumber = RomanNumberConverter.decodeRomanNumber(scheduledPlatform);
      var scheduledStopInfo = new JourneyPassengerStopInfo(scheduledTrack, platformNumber);
      eventEntity.setStopType(JourneyStopType.PASSENGER);
      eventEntity.setScheduledPassengerStopInfo(scheduledStopInfo);
    } else {
      var mappedStopType = stopType == SimRailAwsTimetableEntry.StopType.PT
        ? JourneyStopType.TECHNICAL
        : JourneyStopType.NONE;
      eventEntity.setStopType(mappedStopType);
    }

    // add information about the transport at the station
    var maxSpeed = timetableEntry.getTrainMaxAllowedSpeed();
    var category = timetableEntry.getTrainType();
    var number = timetableEntry.getTrainNumber();
    var transportType = JourneyTransportType.fromTrainType(category);
    var transportEntity = new JourneyTransport();
    transportEntity.setCategory(category);
    transportEntity.setNumber(number);
    transportEntity.setType(transportType);
    transportEntity.setLabel(trainLabel);
    transportEntity.setMaxSpeed(maxSpeed);
    if (transportType == JourneyTransportType.REGIONAL_TRAIN
      || transportType == JourneyTransportType.REGIONAL_FAST_TRAIN) {
      // these types are the only ones with a line that is actually meaningful
      transportEntity.setLine(trainLine);
    }
    eventEntity.setTransport(transportEntity);

    return eventEntity;
  }

  private @NonNull List<SimRailAwsTimetableEntry> fixupTimetable(@NonNull List<SimRailAwsTimetableEntry> timetable) {
    // fixing the events starts from the second event, to facilitate it which means
    // that the first event must always be in the list of events as the loop never touches it
    var firstEvent = timetable.getFirst();
    var fixedEvents = new ArrayList<SimRailAwsTimetableEntry>();
    fixedEvents.add(firstEvent);

    // list of points that were already encountered. used to prevent duplicate
    // points, which is unsupported by the realtime collector.
    var seenPointIds = new HashSet<UUID>();

    var currentEvent = firstEvent;
    for (var index = 1; index < timetable.size(); index++) {
      // check if the point of the event is known if not just add it as fixed
      // as the event will be sorted out later anyway
      var entry = timetable.get(index);
      var point = this.pointProvider.findPointByPointId(entry.getPointId()).orElse(null);
      if (point == null) {
        currentEvent = entry;
        fixedEvents.add(entry);
        continue;
      }

      // check if the point of the current event and the checking event are the same in our mapping
      // if that is not the case just continue as there is nothing to merge
      if (!point.getSimRailPointIds().contains(currentEvent.getPointId())) {
        if (seenPointIds.add(point.getId())) {
          currentEvent = entry;
          fixedEvents.add(entry);
        }

        continue;
      }

      // update the departure time and max speed of the current event
      // (uses the current event as that one is already registered in the fixed list)
      var maxSpeed = Math.max(currentEvent.getTrainMaxAllowedSpeed(), entry.getTrainMaxAllowedSpeed());
      currentEvent.setTrainMaxAllowedSpeed(maxSpeed);
      currentEvent.setDepartureTime(entry.getDepartureTime());

      // check if the stop type of the checking event is higher prioritized as the current event
      var entryStopType = entry.getStopType();
      var currentStopType = currentEvent.getStopType();
      if (currentStopType.compareTo(entryStopType) < 0) {
        currentEvent.setStopType(entryStopType);
        currentEvent.setStopTrack(entry.getStopTrack());
        currentEvent.setStopPlatform(entry.getStopPlatform());
        currentEvent.setStationCategory(entry.getStationCategory());
      }
    }

    return fixedEvents;
  }

  /**
   * Creates a new uuid for a journey based on the given server and run id.
   *
   * @param serverId the id of the server the train run will take place on.
   * @param runId    the id of the run to generate the journey id for.
   * @return the generated journey id based on the given server and run id.
   */
  public @NonNull UUID generateJourneyId(@NonNull UUID serverId, @NonNull UUID runId) {
    return this.journeyIdFactory.create(serverId.toString() + runId);
  }
}
