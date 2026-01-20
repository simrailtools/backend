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
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.simrail.backend.collector.metric.PerServerGauge;
import tools.simrail.backend.collector.server.SimRailServerDescriptor;
import tools.simrail.backend.collector.server.SimRailServerService;
import tools.simrail.backend.collector.util.CancelOnRejectedExecutionPolicy;
import tools.simrail.backend.common.cache.DataCache;
import tools.simrail.backend.common.point.SimRailPointProvider;
import tools.simrail.backend.common.proto.CacheProto;
import tools.simrail.backend.common.proto.EventBusProto;
import tools.simrail.backend.external.srpanel.SimRailPanelApiClient;
import tools.simrail.backend.external.srpanel.model.SimRailPanelTrain;

@Component
class SimRailServerTrainCollector {

  private static final Logger LOGGER = LoggerFactory.getLogger(SimRailServerTrainCollector.class);

  private final SimRailPointProvider pointProvider;
  private final SimRailServerService serverService;
  private final SimRailPanelApiClient panelApiClient;
  private final CollectorJourneyService journeyService;
  private final JourneyEventRealtimeUpdater eventRealtimeUpdater;
  private final SimRailServerTimetableCollector timetableCollector;

  private final ExecutorService trainCollectExecutor;
  private final Map<UUID, ServerCollectorData> serverCollectorData;
  private final DataCache<CacheProto.ExtendedJourneyData> journeyDataCache;

  private final PerServerGauge updatedJourneysCounter;
  private final Meter.MeterProvider<Timer> collectionDurationTimer;

  @Autowired
  public SimRailServerTrainCollector(
    @NonNull SimRailPointProvider pointProvider,
    @NonNull SimRailServerService serverService,
    @NonNull SimRailPanelApiClient panelApiClient,
    @NonNull CollectorJourneyService journeyService,
    @NonNull JourneyEventRealtimeUpdater eventRealtimeUpdater,
    @NonNull SimRailServerTimetableCollector timetableCollector,
    @NonNull @Qualifier("active_journey_data") DataCache<CacheProto.ExtendedJourneyData> journeyDataCache,
    @NonNull @Qualifier("active_journeys_updated_total") PerServerGauge updatedJourneysCounter,
    @Qualifier("active_journey_collect_duration") Meter.@NonNull MeterProvider<Timer> collectionDurationTimer
  ) {
    this.pointProvider = pointProvider;
    this.serverService = serverService;
    this.panelApiClient = panelApiClient;
    this.journeyService = journeyService;
    this.timetableCollector = timetableCollector;
    this.eventRealtimeUpdater = eventRealtimeUpdater;

    this.updatedJourneysCounter = updatedJourneysCounter;
    this.collectionDurationTimer = collectionDurationTimer;

    this.trainCollectExecutor = new ThreadPoolExecutor(
      15,
      30,
      60L,
      TimeUnit.SECONDS,
      new SynchronousQueue<>(),
      new CancelOnRejectedExecutionPolicy());
    this.serverCollectorData = new ConcurrentHashMap<>(20, 0.75f, 1);
    this.journeyDataCache = journeyDataCache;
  }

  private void executeCollectionTask(@NonNull CountDownLatch taskLatch, @NonNull Timer timer, @NonNull Runnable task) {
    var future = this.trainCollectExecutor.submit(() -> {
      var span = Timer.start();
      try {
        task.run();
      } catch (Exception exception) {
        LOGGER.error("Caught exception while executing journey collection task", exception);
      } finally {
        span.stop(timer);
        taskLatch.countDown();
      }
    });

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
      var collectionTimer = this.collectionDurationTimer.withTag("server_code", server.code());
      this.executeCollectionTask(taskCountLatch, collectionTimer, () -> {
        var collectorData = this.serverCollectorData.computeIfAbsent(server.id(), _ -> new ServerCollectorData());

        // fetch the train data from api
        var activeRunIds = this.fetchFullTrainInformation(server, collectorData);
        this.fetchTrainPositionInformation(server, collectorData);

        // filter out all journeys that were not updated at all
        var updatedTrains = collectorData.updateHoldersByRunId.values().stream()
          .filter(updateHolder -> updateHolder.fieldGroup.consumeAnyDirty())
          .toList();

        // process the updated journeys
        for (var updatedTrain : updatedTrains) {
          var cachedData = this.journeyDataCache.findBySecondaryKey(updatedTrain.runIdString);
          var isNewlyActive = cachedData == null;
          var cacheDataBuilder = switch (cachedData) {
            case CacheProto.ExtendedJourneyData data -> data.toBuilder();
            case null -> {
              var journeyId = this.timetableCollector.generateJourneyId(server.id(), updatedTrain.runId);
              yield CacheProto.ExtendedJourneyData.newBuilder()
                .setId(journeyId.toString())
                .setForeignRunId(updatedTrain.runIdString)
                .setServerId(server.id().toString());
            }
          };

          // apply the changed fields to the journey data builder
          var hasNewPosition = updatedTrain.position.consumeDirty();
          var journeyDataBuilder = cacheDataBuilder.getJourneyData().toBuilder();
          if (hasNewPosition) {
            journeyDataBuilder.setPosition(updatedTrain.position.currentValue());
          }
          if (updatedTrain.speed.consumeDirty()) {
            journeyDataBuilder.setSpeed(updatedTrain.speed.currentValue());
          }
          if (updatedTrain.driver.consumeDirty()) {
            var newDriver = updatedTrain.driver.currentValue();
            if (newDriver == null) {
              journeyDataBuilder.clearDriver();
            } else {
              journeyDataBuilder.setDriver(newDriver);
            }
          }
          if (updatedTrain.nextSignal.consumeDirty()) {
            var nextSignal = updatedTrain.nextSignal.currentValue();
            if (nextSignal == null) {
              journeyDataBuilder.clearNextSignal();
            } else {
              journeyDataBuilder.setNextSignal(nextSignal);
            }
          }

          // update the events of the journey, if a new position for the train is known
          if (hasNewPosition) {
            var pos = updatedTrain.position.currentValue();
            var currPoint = this.pointProvider
              .findPointWherePosInBounds(pos.getLongitude(), pos.getLatitude())
              .orElse(null);
            var currPointId = currPoint == null ? null : currPoint.getId().toString();
            var prevPointId = journeyDataBuilder.hasCurrentPointId() ? journeyDataBuilder.getCurrentPointId() : null;
            if (!Objects.equals(prevPointId, currPointId)) {
              // set the new point in the cache data (or remove it)
              if (currPointId != null) {
                journeyDataBuilder.setCurrentPointId(currPointId);
              } else {
                journeyDataBuilder.clearCurrentPointId();
              }

              // update the journey events accordingly
              var jid = UUID.fromString(cacheDataBuilder.getId());
              var ppid = prevPointId == null ? null : UUID.fromString(prevPointId);
              var nextSignal = journeyDataBuilder.hasNextSignal() ? journeyDataBuilder.getNextSignal() : null;
              var updateRequest = new JourneyEventUpdateRequest(jid, server, ppid, currPoint, nextSignal);
              this.eventRealtimeUpdater.requestEventUpdate(updateRequest);
            }
          }

          // mark the first seen time of the journey if it was newly seen
          if (isNewlyActive) {
            var parsedJourneyId = UUID.fromString(cacheDataBuilder.getId());
            this.journeyService.markJourneyAsFirstSeen(parsedJourneyId);
          }

          // insert the journey data into the cache
          var journeyData = journeyDataBuilder.build();
          var journeyCacheData = cacheDataBuilder.setJourneyData(journeyData).build();
          this.journeyDataCache.setCachedValue(journeyCacheData);
          // TODO: send update frame
        }

        // mark the journey as removed that are no longer active
        var updatedJourneyCount = updatedTrains.size();
        if (!activeRunIds.isEmpty()) {
          var runIdStrings = activeRunIds.stream().map(UUID::toString).collect(Collectors.toSet());
          var removedJourneysOnServer = this.journeyDataCache.findBySecondaryKeyNotIn(runIdStrings)
            .filter(data -> data.getServerId().equals(server.id().toString()))
            .map(data -> UUID.fromString(data.getId()))
            .collect(Collectors.toSet());
          this.journeyService.markJourneyAsLastSeen(removedJourneysOnServer);
          updatedJourneyCount += removedJourneysOnServer.size();
          // TODO: send remove frame

          for (var journeyId : removedJourneysOnServer) {
            var updateRequest = new JourneyEventUpdateRequest(journeyId, server, null, null, null);
            this.eventRealtimeUpdater.requestEventUpdate(updateRequest);
            this.journeyDataCache.removeByPrimaryKey(journeyId.toString());
          }
        }

        this.updatedJourneysCounter.setValue(server, updatedJourneyCount);
      });
    }

    var allDidComplete = taskCountLatch.await(20, TimeUnit.SECONDS);
    if (!allDidComplete) {
      LOGGER.warn("At least one journey collect task did not complete within 20 seconds");
    }
  }

  private @NonNull Set<UUID> fetchFullTrainInformation(
    @NonNull SimRailServerDescriptor server,
    @NonNull ServerCollectorData collectorData
  ) {
    // get the train data from upstream api, don't do anything if data didn't change
    var responseTuple = this.panelApiClient.getTrains(server.code(), collectorData.getTrainsEtag());
    collectorData.updateTrainsEtag(responseTuple);
    if (responseTuple.response().status() == HttpStatus.NOT_MODIFIED.value()) {
      return Set.of();
    }

    // get the trains that are currently active on the target server, the returned
    // list can be empty if, for example, the server is currently down
    var response = responseTuple.body();
    var activeTrains = response == null ? null : response.getEntries();
    if (activeTrains == null || activeTrains.isEmpty()) {
      return Set.of();
    }

    var seenRunIds = new HashSet<UUID>();
    for (var activeTrain : activeTrains) {
      if (!seenRunIds.add(activeTrain.getRunId())) {
        // somehow we already processed journey in this run?
        continue;
      }

      var trainData = activeTrain.getDetailData();
      var journeyUpdateHolder = collectorData.updateHoldersByRunId.get(activeTrain.getRunId());
      if (journeyUpdateHolder == null) {
        var speed = roundCurrentSpeed(trainData.getCurrentSpeed());
        var position = constructGeoPosition(trainData.getPositionLatitude(), trainData.getPositionLongitude());
        if (position == null) {
          // can happen if the api is broken... ignore the journey until it's fixed
          continue;
        }

        // construct a new update holder for the journey, set the required base values
        journeyUpdateHolder = new JourneyUpdateHolder(activeTrain.getRunId(), activeTrain.getId());
        journeyUpdateHolder.speed.updateValue(speed);
        journeyUpdateHolder.position.updateValue(position);

        // map the train run id to the
        collectorData.foreignIdToRunId.put(activeTrain.getId(), activeTrain.getRunId());
        collectorData.updateHoldersByRunId.put(activeTrain.getRunId(), journeyUpdateHolder);
      }

      // update the full train data (stuff we don't get from the train position api)
      // otherwise, it could result in the train having outdated data compared to the
      // output of the position api
      var driver = constructUserInfo(trainData);
      journeyUpdateHolder.driver.updateValue(driver);

      var nextSignal = constructNextSignal(trainData);
      journeyUpdateHolder.nextSignal.updateValue(nextSignal);
    }

    return seenRunIds;
  }

  private void fetchTrainPositionInformation(
    @NonNull SimRailServerDescriptor server,
    @NonNull ServerCollectorData collectorData
  ) {
    // get the train positions from upstream api, don't do anything if data didn't change
    var responseTuple = this.panelApiClient.getTrainPositions(server.code(), collectorData.getTrainPositionsEtag());
    collectorData.updateTrainPositionsEtag(responseTuple);
    if (responseTuple.response().status() == HttpStatus.NOT_MODIFIED.value()) {
      return;
    }

    // get the trains that are currently active on the target server, the returned
    // list can be empty if, for example, the server is currently down
    var response = responseTuple.body();
    var trainPositions = response == null ? null : response.getEntries();
    if (trainPositions == null || trainPositions.isEmpty()) {
      return;
    }

    for (var trainPosition : trainPositions) {
      var runId = collectorData.foreignIdToRunId.get(trainPosition.getId());
      if (runId == null) {
        // id is not yet mapped to run, ignore the train for now
        continue;
      }

      var journeyUpdateHolder = collectorData.updateHoldersByRunId.get(runId);
      if (journeyUpdateHolder == null) {
        // usually an update holder must be registered if the run id mapped
        // just to be sure we don't run into weird null issues later
        continue;
      }

      var speed = roundCurrentSpeed(trainPosition.getCurrentSpeed());
      journeyUpdateHolder.speed.updateValue(speed);

      var position = constructGeoPosition(trainPosition.getPositionLatitude(), trainPosition.getPositionLongitude());
      journeyUpdateHolder.position.updateValue(position);
    }
  }

  private int roundCurrentSpeed(double speed) {
    return Math.max(0, (int) Math.round(speed));
  }

  private EventBusProto.@Nullable GeoPosition constructGeoPosition(@Nullable Double lat, @Nullable Double lon) {
    if (lat == null || lon == null) {
      return null;
    }

    return EventBusProto.GeoPosition.newBuilder().setLatitude(lat).setLongitude(lon).build();
  }

  private EventBusProto.@Nullable User constructUserInfo(SimRailPanelTrain.@NonNull DetailData data) {
    if (data.getDriverSteamId() != null) {
      return EventBusProto.User.newBuilder()
        .setId(data.getDriverSteamId())
        .setPlatform(EventBusProto.UserPlatform.STEAM)
        .build();
    }

    if (data.getDiverXBoxId() != null) {
      return EventBusProto.User.newBuilder()
        .setId(data.getDiverXBoxId())
        .setPlatform(EventBusProto.UserPlatform.XBOX)
        .build();
    }

    return null;
  }

  private EventBusProto.@Nullable SignalInfo constructNextSignal(SimRailPanelTrain.@NonNull DetailData data) {
    var signalId = data.getNextSignalId();
    var signalDistance = data.getNextSignalDistance();
    if (signalId == null || signalDistance == null) {
      return null;
    }

    // remove unnecessary information from the signal id
    var signalSeparatorIndex = signalId.indexOf('@');
    if (signalSeparatorIndex != -1) {
      signalId = signalId.substring(0, signalSeparatorIndex);
    }

    // normalize the signal distance to 10 meter accuracy
    var signalInfoBuilder = EventBusProto.SignalInfo.newBuilder().setName(signalId);
    var roundedSignalDistance = (int) Math.round(signalDistance / 10.0) * 10;
    signalInfoBuilder.setDistanceMeters(roundedSignalDistance);

    // sets the speed limitation indicated by the signal, unless it indicates more
    // than 500 km/h (which is quite a lot of buffer, the current max speed of a
    // switch in game is 130 km/h). this check is needed because signals not showing
    // a speed reduction return a value of 32767 for this field
    var maxSpeed = data.getNextSignalSpeed();
    if (maxSpeed < 500) {
      signalInfoBuilder.setMaxSpeedKmh(maxSpeed);
    }

    return signalInfoBuilder.build();
  }
}
