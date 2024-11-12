/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024 Pasqual Koschmieder and contributors
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
import jakarta.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.simrail.backend.collector.server.SimRailServerDescriptor;
import tools.simrail.backend.collector.server.SimRailServerService;
import tools.simrail.backend.common.border.MapBorderPointProvider;
import tools.simrail.backend.common.concurrent.TransactionalFailShutdownTaskScopeFactory;
import tools.simrail.backend.common.journey.JourneyEntity;
import tools.simrail.backend.common.journey.JourneyEventEntity;
import tools.simrail.backend.common.journey.JourneyEventType;
import tools.simrail.backend.common.journey.JourneyPassengerStopInfo;
import tools.simrail.backend.common.journey.JourneyStopDescriptor;
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
  private static final Logger LOGGER = LoggerFactory.getLogger(SimRailServerTimetableCollector.class);

  private final UuidV5Factory journeyIdFactory;
  private final UuidV5Factory journeyEventIdFactory;

  private final SimRailAwsApiClient awsApiClient;
  private final SimRailPointProvider pointProvider;
  private final SimRailServerService serverService;
  private final CollectorJourneyService journeyService;
  private final MapBorderPointProvider borderPointProvider;
  private final TransactionalFailShutdownTaskScopeFactory transactionalTaskScopeFactory;

  @Autowired
  public SimRailServerTimetableCollector(
    @Nonnull SimRailPointProvider pointProvider,
    @Nonnull SimRailServerService serverService,
    @Nonnull CollectorJourneyService journeyService,
    @Nonnull MapBorderPointProvider borderPointProvider,
    @Nonnull TransactionalFailShutdownTaskScopeFactory transactionalTaskScopeFactory
  ) {
    this.pointProvider = pointProvider;
    this.serverService = serverService;
    this.journeyService = journeyService;
    this.borderPointProvider = borderPointProvider;
    this.transactionalTaskScopeFactory = transactionalTaskScopeFactory;
    this.awsApiClient = SimRailAwsApiClient.create();

    this.journeyIdFactory = new UuidV5Factory(JourneyEntity.ID_NAMESPACE);
    this.journeyEventIdFactory = new UuidV5Factory(JourneyEventEntity.ID_NAMESPACE);
  }

  @Transactional
  @Scheduled(initialDelay = 1, fixedRate = 15, timeUnit = TimeUnit.MINUTES, scheduler = "timetable_collect_scheduler")
  public void collectServerTimetables() {
    var servers = this.serverService.getServers();
    for (var server : servers) {
      // get the trains running on the server and their associated run ids
      var startTime = Instant.now();
      var trainRuns = this.awsApiClient.getTrainRuns(server.code());
      var runIds = trainRuns.stream().map(SimRailAwsTrainRun::getRunId).toList();

      // collect the scheduled journeys based on the timetable information
      var existingJourneys = this.journeyService.retrieveJourneysOfServerByRunIds(server.id(), runIds)
        .stream()
        .collect(Collectors.toMap(JourneyEntity::getId, Function.identity()));
      trainRuns.forEach(trainRun -> this.collectJourney(server, trainRun, existingJourneys));

      // print information about the collection run
      var elapsedTime = Duration.between(startTime, Instant.now()).toSeconds();
      LOGGER.info("Collected {} journeys for server {} in {}s", trainRuns.size(), server.code(), elapsedTime);
    }
  }

  private void collectJourney(
    @Nonnull SimRailServerDescriptor server,
    @Nonnull SimRailAwsTrainRun run,
    @Nonnull Map<UUID, JourneyEntity> existingJourneys
  ) {
    // generate the identifier for the journey & find the journey if it was already registered
    var runId = run.getRunId();
    var trainNumber = run.getTrainNumber();
    var id = this.journeyIdFactory.create(trainNumber + runId + server.id());
    var journey = existingJourneys.get(id);
    if (journey == null) {
      var newJourney = new JourneyEntity();
      newJourney.setId(id);
      newJourney.setForeignRunId(runId);
      newJourney.setServerId(server.id());
      newJourney.setServerCode(server.code());
      journey = this.journeyService.persistJourney(newJourney);
    } else if (journey.getFirstSeenTime() != null) {
      // don't update the timetable of a journey that is already active
      return;
    }

    // extract the line information from the display name of the train
    var cleanedTrainName = WHITESPACE_PATTERN.matcher(run.getTrainDisplayName()).replaceAll("");
    var trainNameParts = cleanedTrainName.split("-");
    var trainLine = trainNameParts.length > 1 ? trainNameParts[1] : null;

    // create events for all timetable entries
    var inBorder = false; // keeps track if the journey is within the playable border
    var timetable = this.fixupTimetable(run.getTimetable()); // timetable of the journey
    var lastTimetableIndex = timetable.size() - 1; // the last index of the timetable
    JourneyEventEntity previousEvent = null; // the previous constructed event
    var events = new ArrayList<JourneyEventEntity>(); // all constructed events
    for (var index = 0; index < timetable.size(); index++) {
      // update the current status if the journey is within the playable border
      var timetableEntry = timetable.get(index);
      var isBorderPoint = this.borderPointProvider.isMapBorderPoint(timetableEntry.getPointId());
      if (isBorderPoint) {
        inBorder = !inBorder;
      }

      // get the point associated with the event, if the point is not registered
      // we deem it irrelevant and don't want an event for the point
      var eventPoint = this.pointProvider.findPointByPointId(timetableEntry.getPointId()).orElse(null);
      if (eventPoint == null) {
        continue;
      }

      // construct the arrival event for the entry, if the event is not the first along the route
      if (index != 0) {
        previousEvent = this.registerJourneyEvent(
          id,
          trainLine,
          server,
          JourneyEventType.ARRIVAL,
          previousEvent,
          timetableEntry,
          events,
          inBorder);
      }

      // construct the departure event for the entry, if the event is not the last along the route
      if (index != lastTimetableIndex) {
        previousEvent = this.registerJourneyEvent(
          id,
          trainLine,
          server,
          JourneyEventType.DEPARTURE,
          previousEvent,
          timetableEntry,
          events,
          inBorder);
      }
    }

    // update the events associated with the journey if they changed
    var journeyEvents = journey.getEvents();
    if (!events.equals(journeyEvents)) {
      this.journeyService.forcePersistJourneyEvents(journey, events);
    }
  }

  private @Nullable JourneyEventEntity registerJourneyEvent(
    @Nonnull UUID journeyId,
    @Nullable String trainLine,
    @Nonnull SimRailServerDescriptor server,
    @Nonnull JourneyEventType eventType,
    @Nullable JourneyEventEntity previousEvent,
    @Nonnull SimRailAwsTimetableEntry timetableEntry,
    @Nonnull List<JourneyEventEntity> journeyEvents,
    boolean inMapBorder
  ) {
    var previousTime = previousEvent == null ? null : previousEvent.getScheduledTime();
    var event = this.createJourneyEvent(
      journeyId,
      trainLine,
      server,
      eventType,
      previousTime,
      timetableEntry,
      journeyEvents.size(),
      inMapBorder);
    if (event != null) {
      journeyEvents.add(event);
      return event;
    } else {
      return previousEvent;
    }
  }

  private @Nullable JourneyEventEntity createJourneyEvent(
    @Nonnull UUID journeyId,
    @Nullable String trainLine,
    @Nonnull SimRailServerDescriptor server,
    @Nonnull JourneyEventType eventType,
    @Nullable OffsetDateTime previousEventTime,
    @Nonnull SimRailAwsTimetableEntry timetableEntry,
    int eventIndex,
    boolean inPlayableBorder
  ) {
    // get the point where the event is happening, return if the point is not registered
    var point = this.pointProvider.findPointByPointId(timetableEntry.getPointId()).orElse(null);
    if (point == null) {
      return null;
    }

    // extract the scheduled time of the event
    var scheduledLocalTime = switch (eventType) {
      case ARRIVAL -> timetableEntry.getArrivalTime();
      case DEPARTURE -> timetableEntry.getDepartureTime();
    };
    var scheduledOffsetTime = switch (previousEventTime) {
      case OffsetDateTime previousTime -> {
        // previous time is present, add the diff between the last and current event
        // to the last time to get this event time, there are some cases in which dates
        // of the provided time-date info are one day off
        var diff = Duration.between(previousTime.toLocalTime(), scheduledLocalTime.toLocalTime()).abs();
        yield previousTime.plus(diff);
      }
      case null -> // no previous time present, use the information from the server and event
        OffsetDateTime.of(scheduledLocalTime, server.timezoneOffset());
    };

    // create the event entity id and the base event entity
    var id = this.journeyEventIdFactory.create(journeyId.toString() + point.getId() + scheduledLocalTime + eventType);
    var eventEntity = new JourneyEventEntity();
    eventEntity.setId(id);
    eventEntity.setJourneyId(journeyId);
    eventEntity.setEventType(eventType);
    eventEntity.setEventIndex(eventIndex * 50);
    eventEntity.setScheduledTime(scheduledOffsetTime);
    eventEntity.setRealtimeTime(scheduledOffsetTime);
    eventEntity.setRealtimeTimeType(JourneyTimeType.SCHEDULE);

    // add information about the stop where the event is happening
    var stopDescriptorEntity = new JourneyStopDescriptor();
    stopDescriptorEntity.setId(point.getId());
    stopDescriptorEntity.setName(point.getName());
    stopDescriptorEntity.setPlayable(inPlayableBorder);
    eventEntity.setStopDescriptor(stopDescriptorEntity);

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
    transportEntity.setMaxSpeed(maxSpeed);
    if (transportType == JourneyTransportType.REGIONAL_TRAIN
      || transportType == JourneyTransportType.REGIONAL_FAST_TRAIN) {
      // these types are the only ones with a line that is actually meaningful
      transportEntity.setLine(trainLine);
    }
    eventEntity.setTransport(transportEntity);

    return eventEntity;
  }

  private @Nonnull List<SimRailAwsTimetableEntry> fixupTimetable(@Nonnull List<SimRailAwsTimetableEntry> timetable) {
    var fixedEvents = new ArrayList<SimRailAwsTimetableEntry>();
    var currentEvent = timetable.getFirst();
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
        currentEvent = entry;
        fixedEvents.add(entry);
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
}
