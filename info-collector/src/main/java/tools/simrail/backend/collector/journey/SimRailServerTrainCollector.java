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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.simrail.backend.collector.server.SimRailServerDescriptor;
import tools.simrail.backend.collector.server.SimRailServerService;
import tools.simrail.backend.common.concurrent.TransactionalFailShutdownTaskScopeFactory;
import tools.simrail.backend.common.journey.JourneyEntity;
import tools.simrail.backend.common.journey.JourneyEventEntity;
import tools.simrail.backend.common.journey.JourneyEventRepository;
import tools.simrail.backend.common.journey.JourneySignalInfo;
import tools.simrail.backend.common.shared.GeoPositionEntity;
import tools.simrail.backend.external.srpanel.SimRailPanelApiClient;
import tools.simrail.backend.external.srpanel.model.SimRailPanelTrain;

@Component
class SimRailServerTrainCollector {

  private static final Logger LOGGER = LoggerFactory.getLogger(SimRailServerTrainCollector.class);

  private final SimRailServerService serverService;
  private final SimRailPanelApiClient panelApiClient;
  private final CollectorJourneyService journeyService;
  private final JourneyUpdateHandler journeyUpdateHandler;
  private final JourneyEventRepository journeyEventRepository;
  private final JourneyEventRealtimeUpdater.Factory journeyEventRealtimeUpdaterFactory;
  private final TransactionalFailShutdownTaskScopeFactory transactionalTaskScopeFactory;

  // last time when the full train information was fetched
  private transient Instant lastTrainsFetch;

  @Autowired
  public SimRailServerTrainCollector(
    @Nonnull SimRailServerService serverService,
    @Nonnull CollectorJourneyService journeyService,
    @Nonnull JourneyUpdateHandler journeyUpdateHandler,
    @Nonnull JourneyEventRepository journeyEventRepository,
    @Nonnull JourneyEventRealtimeUpdater.Factory journeyEventRealtimeUpdaterFactory,
    @Nonnull TransactionalFailShutdownTaskScopeFactory transactionalTaskScopeFactory
  ) {
    this.serverService = serverService;
    this.journeyService = journeyService;
    this.journeyUpdateHandler = journeyUpdateHandler;
    this.journeyEventRepository = journeyEventRepository;
    this.transactionalTaskScopeFactory = transactionalTaskScopeFactory;
    this.journeyEventRealtimeUpdaterFactory = journeyEventRealtimeUpdaterFactory;
    this.panelApiClient = SimRailPanelApiClient.create();
  }

  @Scheduled(initialDelay = 0, fixedDelay = 2, timeUnit = TimeUnit.SECONDS, scheduler = "train_collect_scheduler")
  public void collectActiveTrains() throws InterruptedException {
    try (var scope = this.transactionalTaskScopeFactory.get()) {
      // get if a full train data collection should be performed this time
      var lastFullFetch = this.lastTrainsFetch;
      var fullCollection = lastFullFetch == null || Duration.between(lastFullFetch, Instant.now()).toSeconds() >= 7;
      if (fullCollection) {
        this.lastTrainsFetch = Instant.now();
      }

      var servers = this.serverService.getServers();
      for (var server : servers) {
        scope.fork(() -> {
          var startTime = Instant.now();
          var activeServerJourneys = this.journeyService.resolveCachedActiveJourneysOfServer(server.id());

          // keeps track of the dirty journeys without retaining duplicates
          var dirtyRecorders = new HashMap<UUID, JourneyDirtyStateRecorder>();
          Function<JourneyEntity, JourneyDirtyStateRecorder> dirtyRecorderFactory =
            journey -> dirtyRecorders.computeIfAbsent(
              journey.getId(),
              _ -> new JourneyDirtyStateRecorder(journey, server));

          if (fullCollection) {
            // fetch full train details in case we're doing a full collection
            this.fetchFullTrainInformation(server, activeServerJourneys, dirtyRecorderFactory);
          } else {
            // fetch and update train positions
            this.fetchTrainPositionInformation(server, activeServerJourneys, dirtyRecorderFactory);
          }

          // check if any journeys changed and take appropriate action
          var updatedJourneys = dirtyRecorders.values().stream().filter(JourneyDirtyStateRecorder::isDirty).toList();
          if (!updatedJourneys.isEmpty()) {
            if (fullCollection) {
              // if this is a full collection run, persist the changed journeys into the database as well
              var updatedJourneyEntities = updatedJourneys.stream()
                .map(JourneyDirtyStateRecorder::applyChangesToOriginal)
                .map(JourneyDirtyStateRecorder::getOriginal)
                .toList();
              this.journeyService.persistJourneysAndPopulateCache(server.id(), updatedJourneyEntities);

              // update journey events that are associated with journeys that got relevant updates
              var relevantJourneys = updatedJourneys.stream()
                .filter(journey -> journey.hasPositionChanged() || journey.wasRemoved() || journey.wasFirstSeen())
                .collect(Collectors.toMap(recorder -> recorder.getOriginal().getId(), Function.identity()));
              if (!relevantJourneys.isEmpty()) {
                var eventsByJourneyId = this.journeyEventRepository.findAllByJourneyIdIn(relevantJourneys.keySet())
                  .stream()
                  .collect(Collectors.groupingBy(JourneyEventEntity::getJourneyId, Collectors.collectingAndThen(
                    Collectors.toList(),
                    events -> {
                      events.sort(Comparator.comparingInt(JourneyEventEntity::getEventIndex));
                      return events;
                    }
                  )));
                this.updateJourneyEvents(server, relevantJourneys.values(), eventsByJourneyId);
              }
            }

            // publish the updated journey information to listeners
            this.journeyUpdateHandler.publishJourneyUpdates(updatedJourneys);

            var elapsedTime = Duration.between(startTime, Instant.now()).toMillis();
            LOGGER.info("Updated {} trains on {} in {}ms", updatedJourneys.size(), server.code(), elapsedTime);
          }

          return null;
        });
      }

      // wait for all update tasks to complete, log the first exception if one occurred
      scope.join();
      scope.firstException().ifPresent(throwable -> LOGGER.warn("Exception collecting active trains", throwable));
    }
  }

  private void fetchFullTrainInformation(
    @Nonnull SimRailServerDescriptor server,
    @Nonnull Map<UUID, JourneyEntity> activeJourneys,
    @Nonnull Function<JourneyEntity, JourneyDirtyStateRecorder> dirtyRecorderFactory
  ) {
    // get the trains that are currently active on the target server, the returned
    // list can be empty if, for example, the server is currently down
    var response = this.panelApiClient.getTrains(server.code());
    var activeTrains = response.getEntries();
    if (!response.isSuccess() || activeTrains == null || activeTrains.isEmpty()) {
      LOGGER.warn("SimRail api returned no active trains for server {}", server.code());
      return;
    }

    var activeTrainRuns = activeTrains.stream().map(SimRailPanelTrain::getRunId).toList();
    var journeysByRunId = this.journeyService.resolveCachedJourneysOfServer(server.id(), activeTrainRuns)
      .stream()
      .collect(Collectors.toMap(JourneyEntity::getForeignRunId, Function.identity()));
    LOGGER.debug("Got {} trains for {} (cache size: {})", activeTrains.size(), server.code(), activeJourneys.size());
    for (var activeTrain : activeTrains) {
      // find the journey that is associated with the train run
      var journey = journeysByRunId.remove(activeTrain.getRunId());
      if (journey == null) {
        LOGGER.warn("Found train with no associated journey {} on {}", activeTrain.getRunId(), server.code());
        continue;
      }

      // set the first seen time and foreign id if this is the first encounter of the journey
      var dirtyRecorder = dirtyRecorderFactory.apply(journey);
      dirtyRecorder.updateForeignId(activeTrain.getId());

      // update the driver steam id
      var currentDriverId = activeTrain.getDetailData().getDriverSteamId();
      dirtyRecorder.updateDriverSteamId(currentDriverId);

      // update the information about the next signal of the train
      var currentNextSignal = this.constructNextSignal(activeTrain.getDetailData());
      dirtyRecorder.updateNextSignal(currentNextSignal);

      // update the speed which the train currently has
      var currentSpeed = Math.max(0, (int) Math.round(activeTrain.getDetailData().getCurrentSpeed()));
      dirtyRecorder.updateSpeed(currentSpeed);

      // update the position where the journey currently is
      var currentPositionLat = activeTrain.getDetailData().getPositionLatitude();
      var currentPositionLng = activeTrain.getDetailData().getPositionLongitude();
      var currentPosition = currentPositionLat == null || currentPositionLng == null
        ? null
        : new GeoPositionEntity(currentPositionLat, currentPositionLng);
      dirtyRecorder.updatePosition(currentPosition);
    }

    // all journeys that are remaining in the map were active before but are no
    // longer on the server (were removed) - update that state
    for (var journey : journeysByRunId.values()) {
      var dirtyRecorder = dirtyRecorderFactory.apply(journey);
      dirtyRecorder.markRemoved();
      activeJourneys.remove(journey.getId()); // also updates the cache
    }
  }

  private void fetchTrainPositionInformation(
    @Nonnull SimRailServerDescriptor server,
    @Nonnull Map<UUID, JourneyEntity> activeJourneys,
    @Nonnull Function<JourneyEntity, JourneyDirtyStateRecorder> dirtyRecorderFactory
  ) {
    // get the trains that are currently active on the target server, the returned
    // list can be empty if, for example, the server is currently down
    var response = this.panelApiClient.getTrainPositions(server.code());
    var trainPositions = response.getEntries();
    if (!response.isSuccess() || trainPositions == null || trainPositions.isEmpty()) {
      LOGGER.warn("SimRail api returned no train positions for server {}", server.code());
      return;
    }

    //
    var journeysByForeignId = activeJourneys.values()
      .stream()
      .filter(journey -> journey.getForeignId() != null)
      .collect(Collectors.toMap(JourneyEntity::getForeignId, Function.identity()));
    for (var trainPosition : trainPositions) {
      // get the journey associated with the train position
      var journey = journeysByForeignId.get(trainPosition.getId());
      if (journey == null) {
        LOGGER.debug("Position data {} has no associated active journey", trainPosition.getId());
        continue;
      }

      // update the speed which the train currently has
      var dirtyRecorder = dirtyRecorderFactory.apply(journey);
      var currentSpeed = Math.max(0, (int) Math.round(trainPosition.getCurrentSpeed()));
      dirtyRecorder.updateSpeed(currentSpeed);

      // update the position where the journey currently is
      var currentPositionLat = trainPosition.getPositionLatitude();
      var currentPositionLng = trainPosition.getPositionLongitude();
      var currentPosition = currentPositionLat == null || currentPositionLng == null
        ? null
        : new GeoPositionEntity(currentPositionLat, currentPositionLng);
      dirtyRecorder.updatePosition(currentPosition);
    }
  }

  private void updateJourneyEvents(
    @Nonnull SimRailServerDescriptor server,
    @Nonnull Collection<JourneyDirtyStateRecorder> journeys,
    @Nonnull Map<UUID, List<JourneyEventEntity>> eventsByJourney
  ) {
    var updatedJourneys = journeys.stream()
      .filter(recorder -> eventsByJourney.containsKey(recorder.getOriginal().getId()))
      .flatMap(recorder -> {
        var journey = recorder.getOriginal();
        var events = eventsByJourney.get(journey.getId());
        var updater = this.journeyEventRealtimeUpdaterFactory.create(recorder.wasFirstSeen(), journey, server, events);
        if (recorder.wasRemoved()) {
          updater.updateEventsDueToRemoval();
        } else {
          updater.updateEventsDueToPositionChange();
        }

        return updater.getUpdatedJourneyEvents().stream();
      })
      .toList();
    this.journeyEventRepository.saveAll(updatedJourneys);
  }

  private @Nullable JourneySignalInfo constructNextSignal(@Nonnull SimRailPanelTrain.DetailData detailData) {
    // check if the signal is too far away
    var signalId = detailData.getNextSignalId();
    var signalDistance = detailData.getNextSignalDistance();
    if (signalId == null || signalDistance == null) {
      return null;
    }

    // remove unnecessary information from the signal id
    var signalSeparatorIndex = signalId.indexOf('@');
    if (signalSeparatorIndex != -1) {
      signalId = signalId.substring(0, signalSeparatorIndex);
    }

    // normalize the distance and max speed of the signal info
    var maxSpeed = detailData.getNextSignalSpeed();
    var normalizedMaxSpeed = maxSpeed == Short.MAX_VALUE ? null : maxSpeed;
    var roundedSignalDistance = (int) Math.round(signalDistance / 10.0) * 10;
    return new JourneySignalInfo(signalId, roundedSignalDistance, normalizedMaxSpeed);
  }
}
