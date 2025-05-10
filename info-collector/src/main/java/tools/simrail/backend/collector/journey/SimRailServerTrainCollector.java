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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.simrail.backend.collector.metric.PerServerGauge;
import tools.simrail.backend.collector.server.SimRailServerDescriptor;
import tools.simrail.backend.collector.server.SimRailServerService;
import tools.simrail.backend.collector.util.CancelOnRejectedExecutionPolicy;
import tools.simrail.backend.common.journey.JourneyEntity;
import tools.simrail.backend.common.journey.JourneyEventEntity;
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
  private final JourneyEventRealtimeUpdater.Factory journeyEventRealtimeUpdaterFactory;

  private final ExecutorService trainCollectExecutor;
  private final TransactionTemplate transactionTemplate;

  // storage for collect information per server
  private final Map<UUID, CollectorRequestDataStorage> serversDataStorage;

  private final PerServerGauge updatedJourneysCounter;
  private final PerServerGauge runsWithoutJourneyCounter;
  private final Meter.MeterProvider<Timer> collectionDurationTimer;

  @Autowired
  public SimRailServerTrainCollector(
    @Nonnull SimRailServerService serverService,
    @Nonnull SimRailPanelApiClient panelApiClient,
    @Nonnull CollectorJourneyService journeyService,
    @Nonnull JourneyUpdateHandler journeyUpdateHandler,
    @Nonnull TransactionTemplate transactionTemplate,
    @Nonnull JourneyEventRealtimeUpdater.Factory journeyEventRealtimeUpdaterFactory,
    @Nonnull @Qualifier("active_journeys_updated_total") PerServerGauge updatedJourneysCounter,
    @Nonnull @Qualifier("active_trains_without_journey_total") PerServerGauge runsWithoutJourneyCounter,
    @Nonnull @Qualifier("active_journey_collect_duration") Meter.MeterProvider<Timer> collectionDurationTimer
  ) {
    this.serverService = serverService;
    this.panelApiClient = panelApiClient;
    this.journeyService = journeyService;
    this.journeyUpdateHandler = journeyUpdateHandler;
    this.journeyEventRealtimeUpdaterFactory = journeyEventRealtimeUpdaterFactory;

    this.updatedJourneysCounter = updatedJourneysCounter;
    this.collectionDurationTimer = collectionDurationTimer;
    this.runsWithoutJourneyCounter = runsWithoutJourneyCounter;

    this.transactionTemplate = transactionTemplate;
    this.trainCollectExecutor = new ThreadPoolExecutor(
      15,
      30,
      60L,
      TimeUnit.SECONDS,
      new SynchronousQueue<>(),
      new CancelOnRejectedExecutionPolicy());
    this.serversDataStorage = new ConcurrentHashMap<>(20, 0.75f, 1);
  }

  /**
   * Executes the given task in a transaction, catching all exceptions thrown by it and counting down the given latch
   * when the task did complete in any way.
   *
   * @param taskLatch the task latch to count down once the given task runnable completed.
   * @param task      the task runnable to execute transactional in the train collector executor.
   */
  private void executeCollectionTask(@Nonnull CountDownLatch taskLatch, @Nonnull Runnable task) {
    var future = this.trainCollectExecutor.submit(() -> this.transactionTemplate.execute((_) -> {
      try {
        task.run();
      } catch (Exception exception) {
        LOGGER.error("Caught exception while executing journey collection task", exception);
      } finally {
        taskLatch.countDown();
      }

      return null;
    }));

    // the returned future might get canceled immediately if it is rejected by the executor
    // ensure that the task count is decreased to prevent unnecessary waits for dead tasks
    if (future.isCancelled()) {
      taskLatch.countDown();
    }
  }

  @Scheduled(initialDelay = 0, fixedDelay = 2, timeUnit = TimeUnit.SECONDS, scheduler = "train_collect_scheduler")
  public void collectActiveTrains() throws InterruptedException {
    var servers = this.serverService.getServers();
    var taskCountLatch = new CountDownLatch(servers.size());
    for (var server : servers) {
      this.executeCollectionTask(taskCountLatch, () -> {
        var span = Timer.start();
        var now = Instant.now();
        var activeServerJourneys = this.journeyService.resolveCachedActiveJourneysOfServer(server.id());

        var dataStorage = this.serversDataStorage.computeIfAbsent(server.id(), _ -> new CollectorRequestDataStorage());
        var fullCollection = dataStorage.shouldDoTrainsCollection(now);

        // keeps track of the dirty journeys without retaining duplicates
        var dirtyRecorders = new HashMap<UUID, JourneyDirtyStateRecorder>();
        Function<JourneyEntity, JourneyDirtyStateRecorder> dirtyRecorderFactory =
          journey -> dirtyRecorders.computeIfAbsent(
            journey.getId(),
            _ -> new JourneyDirtyStateRecorder(journey, server));

        if (fullCollection) {
          // fetch full train details in case we're doing a full collection,
          // don't do a full collection if the train data didn't change
          fullCollection = this.fetchFullTrainInformation(
            server,
            dataStorage,
            activeServerJourneys,
            dirtyRecorderFactory);
          if (fullCollection) {
            dataStorage.updateLastTrainCollect(now);
          }
        }

        // fetch and update train positions and speed
        this.fetchTrainPositionInformation(server, dataStorage, activeServerJourneys, dirtyRecorderFactory);

        // check if any journeys changed, apply changed data to cached snapshot
        // (data is only persisted into the database during full collections in a later step)
        var updatedJourneys = dirtyRecorders.values().stream()
          .filter(JourneyDirtyStateRecorder::isDirty)
          .map(JourneyDirtyStateRecorder::applyChangesToOriginal)
          .toList();
        if (!updatedJourneys.isEmpty()) {
          if (fullCollection) {
            // if this is a full collection run, persist the changed journeys into the database as well
            var updatedJourneyEntities = updatedJourneys.stream()
              .map(JourneyDirtyStateRecorder::getOriginal)
              .toList();
            this.journeyService.persistJourneysAndPopulateCache(server.id(), updatedJourneyEntities);
          }

          // update journey events that are associated with journeys that got relevant updates
          var relevantJourneys = updatedJourneys.stream()
            .filter(journey -> journey.hasPositionChanged() || journey.wasRemoved())
            .collect(Collectors.toMap(recorder -> recorder.getOriginal().getId(), Function.identity()));
          if (!relevantJourneys.isEmpty()) {
            var relevantJourneyIds = relevantJourneys.keySet();
            var journeyEvents = this.journeyService.resolveJourneyEvents(relevantJourneyIds);
            this.updateJourneyEvents(server, relevantJourneys.values(), journeyEvents);
          }

          // publish the updated journey information to listeners
          this.journeyUpdateHandler.publishJourneyUpdates(updatedJourneys);

          this.updatedJourneysCounter.setValue(server, updatedJourneys.size());
          span.stop(this.collectionDurationTimer.withTag("server_code", server.code()));
        }
      });
    }

    var allDidComplete = taskCountLatch.await(20, TimeUnit.SECONDS);
    if (!allDidComplete) {
      LOGGER.warn("At least one journey collect task did not complete within 20 seconds");
    }
  }

  private boolean fetchFullTrainInformation(
    @Nonnull SimRailServerDescriptor server,
    @Nonnull CollectorRequestDataStorage eTagStorage,
    @Nonnull Map<UUID, JourneyEntity> activeJourneys,
    @Nonnull Function<JourneyEntity, JourneyDirtyStateRecorder> dirtyRecorderFactory
  ) {
    // get the train positions from upstream api, don't do anything if data didn't change
    var responseTuple = this.panelApiClient.getTrains(server.code(), eTagStorage.getTrainsEtag());
    eTagStorage.updateTrainsEtag(responseTuple);
    if (responseTuple.response().status() == HttpStatus.NOT_MODIFIED.value()) {
      return false;
    }

    // get the trains that are currently active on the target server, the returned
    // list can be empty if, for example, the server is currently down
    var response = responseTuple.body();
    var activeTrains = response == null ? null : response.getEntries();
    if (activeTrains == null || activeTrains.isEmpty()) {
      LOGGER.warn("SimRail api returned no active trains for server {}", server.code());
      return false;
    }

    var trainsWithoutJourney = 0;
    var activeTrainRuns = activeTrains.stream().map(SimRailPanelTrain::getRunId).toList();
    var journeysByRunId = this.journeyService.resolveCachedJourneysOfServer(server.id(), activeTrainRuns)
      .stream()
      .collect(Collectors.toMap(JourneyEntity::getForeignRunId, Function.identity()));
    for (var activeTrain : activeTrains) {
      // find the journey that is associated with the train run
      var journey = journeysByRunId.remove(activeTrain.getRunId());
      if (journey == null) {
        trainsWithoutJourney++;
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
    }

    // all journeys that are remaining in the map were active before but are no
    // longer on the server (were removed) - update that state
    for (var journey : journeysByRunId.values()) {
      var dirtyRecorder = dirtyRecorderFactory.apply(journey);
      dirtyRecorder.markRemoved();
      activeJourneys.remove(journey.getId()); // also updates the cache
    }

    this.runsWithoutJourneyCounter.setValue(server, trainsWithoutJourney);
    return true;
  }

  private void fetchTrainPositionInformation(
    @Nonnull SimRailServerDescriptor server,
    @Nonnull CollectorRequestDataStorage eTagStorage,
    @Nonnull Map<UUID, JourneyEntity> activeJourneys,
    @Nonnull Function<JourneyEntity, JourneyDirtyStateRecorder> dirtyRecorderFactory
  ) {
    // get the train positions from upstream api, don't do anything if data didn't change
    var responseTuple = this.panelApiClient.getTrainPositions(server.code(), eTagStorage.getTrainPositionsEtag());
    eTagStorage.updateTrainPositionsEtag(responseTuple);
    if (responseTuple.response().status() == HttpStatus.NOT_MODIFIED.value()) {
      return;
    }

    // get the trains that are currently active on the target server, the returned
    // list can be empty if, for example, the server is currently down
    var response = responseTuple.body();
    var trainPositions = response == null ? null : response.getEntries();
    if (trainPositions == null || trainPositions.isEmpty()) {
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
        var updater = this.journeyEventRealtimeUpdaterFactory.create(journey, server, events);
        if (recorder.wasRemoved()) {
          updater.updateEventsDueToRemoval();
          eventsByJourney.remove(journey.getId()); // removes all unnecessary events from the cache
        } else if (recorder.hasPositionChanged()) {
          updater.updateEventsDueToPositionChange();
        }

        // mark that one event of the journey was updated if we updated at least one event
        var updatedEvents = updater.getUpdatedJourneyEvents();
        if (!updatedEvents.isEmpty()) {
          recorder.markEventUpdated();
        }

        return updatedEvents.stream();
      })
      .toList();
    this.journeyService.persistJourneyEvents(updatedJourneys);
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
