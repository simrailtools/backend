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
import io.nats.client.Connection;
import jakarta.annotation.PostConstruct;
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
import tools.simrail.backend.collector.server.SimRailServerDescriptor;
import tools.simrail.backend.collector.server.SimRailServerService;
import tools.simrail.backend.collector.util.CancelOnRejectedExecutionPolicy;
import tools.simrail.backend.collector.util.PerServerGauge;
import tools.simrail.backend.collector.util.UserFactory;
import tools.simrail.backend.common.cache.DataCache;
import tools.simrail.backend.common.event.EventSubjectFactory;
import tools.simrail.backend.common.point.SimRailPointProvider;
import tools.simrail.backend.common.proto.EventBusProto;
import tools.simrail.backend.common.util.MonotonicInstantProvider;
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

  private final Connection connection;
  private final ExecutorService trainCollectExecutor;
  private final Map<UUID, ServerCollectorData> serverCollectorData;
  private final DataCache<EventBusProto.JourneyUpdateFrame> journeyDataCache;

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
    @NonNull Connection natsConnection,
    @NonNull @Qualifier("journey_realtime_cache") DataCache<EventBusProto.JourneyUpdateFrame> journeyDataCache,
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
    this.connection = natsConnection;
    this.journeyDataCache = journeyDataCache;
  }

  @PostConstruct
  public void reconstructUpdateHoldersFromCache() {
    this.journeyDataCache.pullCacheFromStorage(); // re-init data cache from underlying storage
    var cachedValues = this.journeyDataCache.cachedValuesSnapshot();
    for (var cachedValue : cachedValues) {
      // ensure a collector data holder is present for the server
      var idHolder = cachedValue.getIds();
      var sid = UUID.fromString(idHolder.getServerId());
      var serverDataHolder = this.serverCollectorData.computeIfAbsent(sid, _ -> new ServerCollectorData());

      // register an update holder for the journey
      var jid = UUID.fromString(idHolder.getDataId());
      var runId = UUID.fromString(idHolder.getForeignId());
      var updateHolder = new JourneyUpdateHolder(runId, jid);
      serverDataHolder.updateHoldersByRunId.put(runId, updateHolder);

      // reconstruct the update fields state from the journey data
      var data = cachedValue.getJourneyData();
      updateHolder.speed.forceUpdateValue(data.getSpeed());
      updateHolder.position.forceUpdateValue(data.getPosition());
      if (data.hasDriver()) {
        updateHolder.driver.forceUpdateValue(data.getDriver());
      }
      if (data.hasNextSignal()) {
        updateHolder.nextSignal.forceUpdateValue(data.getNextSignal());
        updateHolder.nextSignalId.forceUpdateValue(data.getNextSignal().getName());
      }
    }
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
          var prevUpdateFrame = this.journeyDataCache.findBySecondaryKey(updatedTrain.runIdString);
          var isNewlyActive = prevUpdateFrame == null;
          var updateFrameBuilder = switch (prevUpdateFrame) {
            case EventBusProto.JourneyUpdateFrame data -> data.toBuilder();
            case null -> {
              var ids = EventBusProto.IdHolder.newBuilder()
                .setDataId(updatedTrain.journeyId.toString())
                .setServerId(server.id().toString())
                .setForeignId(updatedTrain.runIdString)
                .build();
              yield EventBusProto.JourneyUpdateFrame.newBuilder().setIds(ids);
            }
          };

          // apply the changed fields to the journey data builder
          var journeyDataBuilder = updateFrameBuilder.getJourneyData().toBuilder();
          var hasNewSignalInFront = updatedTrain.nextSignalId.consumeDirty();
          var hasNewPosition = updatedTrain.position.ifDirty(journeyDataBuilder::setPosition);
          updatedTrain.speed.ifDirty(journeyDataBuilder::setSpeed);
          updatedTrain.driver.ifDirty(driver -> {
            if (driver == null) {
              journeyDataBuilder.clearDriver();
            } else {
              journeyDataBuilder.setDriver(driver);
            }
          });
          updatedTrain.nextSignal.ifDirty(signal -> {
            if (signal == null) {
              journeyDataBuilder.clearNextSignal();
            } else {
              journeyDataBuilder.setNextSignal(signal);
            }
          });

          // update the events of the journey, if a new position for the train is known
          var arrivedAtNewPoint = false;
          if (hasNewPosition) {
            var pos = updatedTrain.position.currentValue();
            var currPoint = this.pointProvider
              .findPointWherePosInBounds(pos.getLongitude(), pos.getLatitude())
              .orElse(null);
            var currPointId = currPoint == null ? null : currPoint.getId().toString();
            var prevPointId = journeyDataBuilder.hasCurrentPointId() ? journeyDataBuilder.getCurrentPointId() : null;
            arrivedAtNewPoint = !Objects.equals(prevPointId, currPointId);
            if (arrivedAtNewPoint) {
              // set the new point in the cache data (or remove it)
              if (currPointId != null) {
                journeyDataBuilder.setCurrentPointId(currPointId);
              } else {
                journeyDataBuilder.clearCurrentPointId();
              }

              // update the journey events accordingly
              var jid = UUID.fromString(updateFrameBuilder.getIds().getDataId());
              var ppid = prevPointId == null ? null : UUID.fromString(prevPointId);
              var nextSignal = journeyDataBuilder.hasNextSignal() ? journeyDataBuilder.getNextSignal() : null;
              var updateRequest = JourneyEventUpdateRequest.forEventUpdate(jid, server, ppid, currPoint, nextSignal);
              this.eventRealtimeUpdater.requestEventUpdate(updateRequest);
            }
          }

          // if the journey did not arrive at a new point (this triggers an event update anyway)
          // but has a new signal in front, this could mean that the journey moved to a platform
          // within the station. this is e.g. relevant for trains to Katowice from Katowice Zawodzie,
          // as they move through a part of Katowice that already has signals but no platforms. the
          // arrival at a platform actually happens after passing 2 signals in Katowice
          if (!arrivedAtNewPoint && hasNewSignalInFront) {
            var pos = updatedTrain.position.currentValue();
            this.pointProvider.findPointWherePosInBounds(pos.getLongitude(), pos.getLatitude()).ifPresent(point -> {
              var jid = UUID.fromString(updateFrameBuilder.getIds().getDataId());
              var nextSignalId = updatedTrain.nextSignalId.currentValue();
              var updateRequest = JourneyEventUpdateRequest.forSignalUpdate(jid, server, point, nextSignalId);
              this.eventRealtimeUpdater.requestEventUpdate(updateRequest);
            });
          }

          // mark the first seen time of the journey if it was newly seen
          if (isNewlyActive) {
            var parsedJourneyId = UUID.fromString(updateFrameBuilder.getIds().getDataId());
            this.journeyService.markJourneyAsFirstSeen(parsedJourneyId);
          }

          // insert the journey data into the cache
          var baseFrameData = EventBusProto.BaseFrameData.newBuilder()
            .setTimestamp(MonotonicInstantProvider.monotonicTimeMillis())
            .build();
          var updateFrame = updateFrameBuilder
            .setBaseData(baseFrameData)
            .setJourneyData(journeyDataBuilder.build())
            .build();
          this.journeyDataCache.setCachedValue(updateFrame);

          // send out journey update frame
          var subject = EventSubjectFactory.createJourneyUpdateSubjectV1(
            updateFrame.getIds().getServerId(),
            updateFrame.getIds().getDataId());
          this.connection.publish(subject, updateFrame.toByteArray());
        }

        // mark the journey as removed that are no longer active
        var updatedJourneyCount = updatedTrains.size();
        if (!activeRunIds.isEmpty()) {
          var runIdStrings = activeRunIds.stream().map(UUID::toString).collect(Collectors.toSet());
          var removedJourneysOnServer = this.journeyDataCache.findBySecondaryKeyNotIn(runIdStrings)
            .filter(data -> data.getIds().getServerId().equals(server.id().toString()))
            .map(data -> UUID.fromString(data.getIds().getDataId()))
            .collect(Collectors.toSet());
          this.journeyService.markJourneyAsLastSeen(removedJourneysOnServer);
          updatedJourneyCount += removedJourneysOnServer.size();

          // clear the removed journey journeys from the collector data cache
          for (var entry : collectorData.foreignIdToRunId.entrySet()) {
            var foreignId = entry.getKey();
            var runId = entry.getValue();
            if (!activeRunIds.contains(runId)) {
              collectorData.foreignIdToRunId.remove(foreignId, runId);
              collectorData.updateHoldersByRunId.remove(runId);
            }
          }

          for (var journeyId : removedJourneysOnServer) {
            // request an update of the journey events because of the removal
            var journeyIdString = journeyId.toString();
            var updateRequest = JourneyEventUpdateRequest.forRemoval(journeyId, server);
            this.eventRealtimeUpdater.requestEventUpdate(updateRequest);
            this.journeyDataCache.removeByPrimaryKey(journeyIdString);

            // send out journey removal frame
            var baseFrameData = EventBusProto.BaseFrameData.newBuilder()
              .setTimestamp(MonotonicInstantProvider.monotonicTimeMillis())
              .build();
            var journeyRemoveFrame = EventBusProto.JourneyRemoveFrame.newBuilder()
              .setBaseData(baseFrameData)
              .setJourneyId(journeyIdString)
              .build();
            var subject = EventSubjectFactory.createJourneyRemoveSubjectV1(server.id().toString(), journeyIdString);
            this.connection.publish(subject, journeyRemoveFrame.toByteArray());
          }
        }

        // only update the count of updated journeys if we updated more than one journey to prevent
        // the metric from jumping to 0 if the upstream data did not change (which looks really
        // weird when plotted on a dashboard)
        if (updatedJourneyCount > 0) {
          this.updatedJourneysCounter.setValue(server, updatedJourneyCount);
        }
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
        var journeyId = this.timetableCollector.generateJourneyId(server.id(), activeTrain.getRunId());
        journeyUpdateHolder = new JourneyUpdateHolder(activeTrain.getRunId(), journeyId);
        journeyUpdateHolder.speed.updateValue(speed);
        journeyUpdateHolder.position.updateValue(position);
        collectorData.updateHoldersByRunId.put(activeTrain.getRunId(), journeyUpdateHolder);
      }

      // ensure that the id of the journey is mapped to the foreign run id
      // this is done separate from the previous if block because reconstruction
      // from cache cannot register this information, so it has to be done even
      // if a journey update holder is already registered for the journey
      collectorData.foreignIdToRunId.putIfAbsent(activeTrain.getId(), activeTrain.getRunId());

      // update the full train data (stuff we don't get from the train position api)
      // otherwise, it could result in the train having outdated data compared to the
      // output of the position api
      var driver = UserFactory.constructUser(
        EventBusProto.UserPlatform.STEAM, trainData.getDriverSteamId(),
        EventBusProto.UserPlatform.XBOX, trainData.getDiverXBoxId());
      journeyUpdateHolder.driver.updateValue(driver);

      var nextSignal = constructNextSignal(trainData);
      journeyUpdateHolder.nextSignal.updateValue(nextSignal);
      journeyUpdateHolder.nextSignalId.updateValue(nextSignal == null ? null : nextSignal.getName());
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
