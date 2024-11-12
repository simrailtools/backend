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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
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
import tools.simrail.backend.common.journey.JourneySignalInfo;
import tools.simrail.backend.external.srpanel.SimRailPanelApiClient;
import tools.simrail.backend.external.srpanel.model.SimRailPanelTrain;

@Component
class SimRailServerTrainCollector {

  private static final Logger LOGGER = LoggerFactory.getLogger(SimRailServerTrainCollector.class);

  private final SimRailServerService serverService;
  private final SimRailPanelApiClient panelApiClient;
  private final CollectorJourneyService journeyService;
  private final TransactionalFailShutdownTaskScopeFactory transactionalTaskScopeFactory;

  // last time when the full train information was fetched
  private transient Instant lastTrainsFetch;

  @Autowired
  public SimRailServerTrainCollector(
    @Nonnull SimRailServerService serverService,
    @Nonnull CollectorJourneyService journeyService,
    @Nonnull TransactionalFailShutdownTaskScopeFactory transactionalTaskScopeFactory
  ) {
    this.serverService = serverService;
    this.journeyService = journeyService;
    this.transactionalTaskScopeFactory = transactionalTaskScopeFactory;
    this.panelApiClient = SimRailPanelApiClient.create();
  }

  @Scheduled(initialDelay = 0, fixedDelay = 2, timeUnit = TimeUnit.SECONDS, scheduler = "train_collect_scheduler")
  public void collectActiveTrains() throws InterruptedException {
    try (var scope = this.transactionalTaskScopeFactory.get()) {
      var servers = this.serverService.getServers();
      for (var server : servers) {
        scope.fork(() -> {
          var startTime = Instant.now();
          var activeServerJourneys = this.journeyService.resolveCachedActiveJourneysOfServer(server.id());

          // keeps track of the dirty journeys without retaining duplicates
          var dirtyJourneys = new HashMap<UUID, JourneyEntity>();
          Consumer<JourneyEntity> journeyDirtyMarker = journey -> dirtyJourneys.putIfAbsent(journey.getId(), journey);

          // fetch all train information
          var lastFullTrainsFetch = this.lastTrainsFetch;
          if (lastFullTrainsFetch == null || Duration.between(lastFullTrainsFetch, Instant.now()).toSeconds() >= 7) {
            this.lastTrainsFetch = Instant.now();
            this.fetchFullTrainInformation(server, activeServerJourneys, journeyDirtyMarker);
          }

          // if some journeys were changed persist them now
          if (!dirtyJourneys.isEmpty()) {
            this.journeyService.persistJourneysAndPopulateCache(server.id(), dirtyJourneys.values());
            var elapsedTime = Duration.between(startTime, Instant.now()).toSeconds();
            LOGGER.info("Stored updates of {} trains on {} in {}s", dirtyJourneys.size(), server.code(), elapsedTime);
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
    @Nonnull Consumer<JourneyEntity> journeyDirtyMarker
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
    for (var activeTrain : activeTrains) {
      // find the journey that is associated with the train run
      var journey = journeysByRunId.remove(activeTrain.getRunId());
      if (journey == null) {
        LOGGER.warn("Found train with no associated journey {} on {}", activeTrain.getRunId(), server.code());
        continue;
      }

      // set the first seen time and foreign id if this is the first encounter of the journey
      var dirty = false;
      if (journey.getFirstSeenTime() == null) {
        dirty = true;
        journey.setForeignId(activeTrain.getId());
        journey.setFirstSeenTime(OffsetDateTime.now(server.timezoneOffset()));
      }

      // update the driver steam id
      var storedDriverId = journey.getDriverSteamId();
      var currentDriverId = activeTrain.getDetailData().getDriverSteamId();
      if (!Objects.equals(storedDriverId, currentDriverId)) {
        dirty = true;
        journey.setDriverSteamId(currentDriverId);
      }

      // update the information about the next signal of the train
      var storedNextSignal = journey.getNextSignal();
      var currentNextSignal = this.constructNextSignal(activeTrain.getDetailData());
      if (!Objects.equals(storedNextSignal, currentNextSignal)) {
        dirty = true;
        journey.setNextSignal(currentNextSignal);
      }

      // if the journey was marked as dirty add it to the dirty journey collection
      if (dirty) {
        journeyDirtyMarker.accept(journey);
      }
    }

    // all journeys that are remaining in the map were active before but are no
    // longer on the server (were removed) - update that state
    for (var journey : journeysByRunId.values()) {
      journey.setSpeed(null);
      journey.setPosition(null);
      journey.setNextSignal(null);
      journey.setDriverSteamId(null);
      journey.setLastSeenTime(OffsetDateTime.now(server.timezoneOffset()));
      journeyDirtyMarker.accept(journey);
      activeJourneys.remove(journey.getId()); // also updates the cache
    }
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
