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

package tools.simrail.backend.collector.dispatchpost;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Timer;
import io.nats.client.Connection;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import tools.simrail.backend.collector.server.SimRailServerDescriptor;
import tools.simrail.backend.collector.server.SimRailServerService;
import tools.simrail.backend.collector.util.PerServerGauge;
import tools.simrail.backend.collector.util.UserFactory;
import tools.simrail.backend.common.cache.DataCache;
import tools.simrail.backend.common.dispatchpost.SimRailDispatchPostEntity;
import tools.simrail.backend.common.event.EventSubjectFactory;
import tools.simrail.backend.common.proto.EventBusProto;
import tools.simrail.backend.common.util.MonotonicInstantProvider;
import tools.simrail.backend.common.util.UuidV5Factory;
import tools.simrail.backend.external.srpanel.SimRailPanelApiClient;
import tools.simrail.backend.external.srpanel.model.SimRailPanelDispatchPost;

@Component
final class SimRailDispatchPostCollector {

  private static final Logger LOGGER = LoggerFactory.getLogger(SimRailDispatchPostCollector.class);

  private final Connection connection;
  private final SimRailServerService serverService;
  private final UuidV5Factory dispatchPostIdFactory;
  private final SimRailPanelApiClient panelApiClient;
  private final CollectorDispatchPostService dispatchPostService;

  private final Map<UUID, ServerCollectorData> serverCollectorData;
  private final DataCache<EventBusProto.DispatchPostUpdateFrame> dispatchPostDataCache;

  private final PerServerGauge collectedDispatchPostCounter;
  private final Meter.MeterProvider<Timer> collectionDurationTimer;

  @Autowired
  public SimRailDispatchPostCollector(
    @NonNull Connection connection,
    @NonNull SimRailServerService serverService,
    @NonNull SimRailPanelApiClient panelApiClient,
    @NonNull CollectorDispatchPostService dispatchPostService,
    @NonNull @Qualifier("dispatch_post_cache") DataCache<EventBusProto.DispatchPostUpdateFrame> dispatchPostDataCache,
    @NonNull @Qualifier("dispatch_post_collected_total") PerServerGauge collectedDispatchPostCounter,
    @Qualifier("dispatch_post_collection_duration") Meter.@NonNull MeterProvider<Timer> collectionDurationTimer
  ) {
    this.connection = connection;
    this.serverService = serverService;
    this.panelApiClient = panelApiClient;
    this.dispatchPostService = dispatchPostService;
    this.dispatchPostIdFactory = new UuidV5Factory(SimRailDispatchPostEntity.ID_NAMESPACE);

    this.dispatchPostDataCache = dispatchPostDataCache;
    this.serverCollectorData = new ConcurrentHashMap<>(20, 0.9f, 1);

    this.collectionDurationTimer = collectionDurationTimer;
    this.collectedDispatchPostCounter = collectedDispatchPostCounter;
  }

  @PostConstruct
  public void reconstructUpdateHoldersFromCache() {
    this.dispatchPostDataCache.pullCacheFromStorage(); // re-init data cache from underlying storage
    var cachedValues = this.dispatchPostDataCache.cachedValuesSnapshot();
    for (var cachedValue : cachedValues) {
      // ensure a collector data holder is present for the server
      var idHolder = cachedValue.getIds();
      var sid = UUID.fromString(idHolder.getServerId());
      var serverDataHolder = this.serverCollectorData.computeIfAbsent(sid, _ -> new ServerCollectorData());

      // register an update holder for the dispatch post
      var postId = UUID.fromString(idHolder.getDataId());
      var cacheKey = DispatchPostUpdateHolder.createSecondaryCacheKey(idHolder.getServerId(), idHolder.getForeignId());
      var updateHolder = new DispatchPostUpdateHolder(postId, idHolder.getForeignId(), cacheKey);
      serverDataHolder.updateHoldersByForeignId.put(idHolder.getForeignId(), updateHolder);

      // reconstruct the update fields state from the dispatch post data
      var data = cachedValue.getDispatchPostData();
      if (data.hasDispatcher()) {
        updateHolder.dispatcher.forceUpdateValue(data.getDispatcher());
      }
    }
  }

  @Scheduled(
    initialDelay = 0,
    fixedDelay = 2,
    timeUnit = TimeUnit.SECONDS,
    scheduler = "dispatch_post_collect_scheduler"
  )
  public void collectDispatchPostInformation() {
    var servers = this.serverService.getServers();
    for (var server : servers) {
      var sample = Timer.start();
      try {
        var collectedDispatchPostCount = this.collectServerDispatchPosts(server);
        if (collectedDispatchPostCount > 0) {
          this.collectedDispatchPostCounter.setValue(server, collectedDispatchPostCount);
        }
      } catch (Exception exception) {
        LOGGER.error("Caught exception while collecting dispatch posts", exception);
      } finally {
        var timer = this.collectionDurationTimer.withTag("server_code", server.code());
        sample.stop(timer);
      }
    }
  }

  private int collectServerDispatchPosts(@NonNull SimRailServerDescriptor server) {
    // get the post data from upstream api, don't do anything if data didn't change
    var collectorData = this.serverCollectorData.computeIfAbsent(server.id(), _ -> new ServerCollectorData());
    var responseTuple = this.panelApiClient.getDispatchPosts(server.code(), collectorData.getDispatchPostEtag());
    collectorData.updateDispatchPostEtag(responseTuple);
    if (responseTuple.response().status() == HttpStatus.NOT_MODIFIED.value()) {
      return 0;
    }

    // get the posts that are currently active on the target server, the returned
    // list can be empty if, for example, the server is currently down
    var response = responseTuple.body();
    var dispatchPosts = response == null ? null : response.getEntries();
    if (dispatchPosts == null || dispatchPosts.isEmpty()) {
      return 0;
    }

    var shouldUpdateDatabase = collectorData.shouldUpdateDatabase();
    for (var dispatchPost : dispatchPosts) {
      var updateHolder = collectorData.updateHoldersByForeignId.get(dispatchPost.getId());
      if (updateHolder == null) {
        var postId = this.dispatchPostIdFactory.create(server.code() + dispatchPost.getId());
        var cacheKey = DispatchPostUpdateHolder.createSecondaryCacheKey(server.id().toString(), dispatchPost.getId());
        updateHolder = new DispatchPostUpdateHolder(postId, dispatchPost.getId(), cacheKey);
        collectorData.updateHoldersByForeignId.put(dispatchPost.getId(), updateHolder);
      }

      // extract the dispatching user
      var dispatcher = CollectionUtils.firstElement(dispatchPost.getDispatchers());
      var dispatchingUser = switch (dispatcher) {
        case null -> null;
        case SimRailPanelDispatchPost.Dispatcher _ -> UserFactory.constructUser(
          EventBusProto.UserPlatform.STEAM, dispatcher.getSteamId(),
          EventBusProto.UserPlatform.XBOX, dispatcher.getXboxId());
      };
      updateHolder.dispatcher.updateValue(dispatchingUser);

      // update the cached dispatch post data if any field is dirty
      var prevUpdateFrame = this.dispatchPostDataCache.findBySecondaryKey(updateHolder.secondaryCacheKey);
      var mightBeNewDispatchPost = prevUpdateFrame == null;
      if (updateHolder.fieldGroup.consumeAnyDirty()) {
        var updateFrameBuilder = switch (prevUpdateFrame) {
          case EventBusProto.DispatchPostUpdateFrame data -> data.toBuilder();
          case null -> {
            var ids = EventBusProto.IdHolder.newBuilder()
              .setDataId(updateHolder.id.toString())
              .setServerId(server.id().toString())
              .setForeignId(updateHolder.foreignId)
              .build();
            yield EventBusProto.DispatchPostUpdateFrame.newBuilder().setIds(ids);
          }
        };

        // update the dispatch post data
        var dispatchPostDataBuilder = updateFrameBuilder.getDispatchPostData().toBuilder();
        updateHolder.dispatcher.ifDirty(user -> {
          if (user == null) {
            dispatchPostDataBuilder.clearDispatcher();
          } else {
            dispatchPostDataBuilder.setDispatcher(user);
          }
        });

        // insert the dispatch post data into the cache
        var baseFrameData = EventBusProto.BaseFrameData.newBuilder()
          .setTimestamp(MonotonicInstantProvider.monotonicTimeMillis())
          .build();
        var updateFrame = updateFrameBuilder
          .setBaseData(baseFrameData)
          .setDispatchPostData(dispatchPostDataBuilder.build())
          .build();
        this.dispatchPostDataCache.setCachedValue(updateFrame);

        // send out journey update frame
        var subject = EventSubjectFactory.createDispatchPostUpdateSubjectV1(
          updateFrame.getIds().getServerId(),
          updateFrame.getIds().getDataId());
        this.connection.publish(subject, updateFrame.toByteArray());
      }

      // update the dispatch post in the database if required
      if (shouldUpdateDatabase || mightBeNewDispatchPost) {
        this.dispatchPostService.saveDispatchPost(server, dispatchPost, updateHolder);
      }
    }

    // send out deletion frames for all deleted servers
    var serverIdString = server.id().toString();
    var allCollectedCacheKeys = dispatchPosts.stream()
      .map(post -> DispatchPostUpdateHolder.createSecondaryCacheKey(server.id().toString(), post.getId()))
      .collect(Collectors.toSet());
    var removedPosts = this.dispatchPostDataCache.findBySecondaryKeyNotIn(allCollectedCacheKeys);
    removedPosts
      .filter(post -> post.getIds().getServerId().equals(serverIdString))
      .forEach(post -> {
        this.dispatchPostDataCache.removeByPrimaryKey(post.getIds().getDataId());

        // send out dispatch post removal frame
        var baseFrameData = EventBusProto.BaseFrameData.newBuilder()
          .setTimestamp(MonotonicInstantProvider.monotonicTimeMillis())
          .build();
        var dispatchPostRemoveFrame = EventBusProto.DispatchPostRemoveFrame.newBuilder()
          .setBaseData(baseFrameData)
          .setPostId(post.getIds().getServerId())
          .build();
        var subject = EventSubjectFactory.createDispatchPostRemoveSubjectV1(
          post.getIds().getServerId(),
          post.getIds().getDataId());
        this.connection.publish(subject, dispatchPostRemoveFrame.toByteArray());
      });

    // remove the removed posts from the server data holder lookup
    var existingPostIds = dispatchPosts.stream().map(SimRailPanelDispatchPost::getId).collect(Collectors.toSet());
    for (var holder : collectorData.updateHoldersByForeignId.values()) {
      if (!existingPostIds.contains(holder.foreignId)) {
        collectorData.updateHoldersByForeignId.remove(holder.foreignId);
      }
    }

    // mark all removed dispatch posts as deleted
    if (shouldUpdateDatabase) {
      this.dispatchPostService.markUncontainedDispatchPostsAsDeleted(server.id(), existingPostIds);
    }

    return dispatchPosts.size();
  }
}
