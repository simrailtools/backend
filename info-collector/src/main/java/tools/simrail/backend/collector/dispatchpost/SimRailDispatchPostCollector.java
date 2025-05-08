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
import jakarta.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.simrail.backend.collector.metric.PerServerGauge;
import tools.simrail.backend.collector.server.SimRailServerService;
import tools.simrail.backend.common.dispatchpost.SimRailDispatchPostEntity;
import tools.simrail.backend.common.dispatchpost.SimRailDispatchPostRepository;
import tools.simrail.backend.common.point.SimRailPointProvider;
import tools.simrail.backend.common.shared.GeoPositionEntity;
import tools.simrail.backend.common.util.MongoIdDecodeUtil;
import tools.simrail.backend.common.util.UuidV5Factory;
import tools.simrail.backend.external.srpanel.SimRailPanelApiClient;
import tools.simrail.backend.external.srpanel.model.SimRailPanelDispatchPost;

@Component
final class SimRailDispatchPostCollector {

  private static final Logger LOGGER = LoggerFactory.getLogger(SimRailDispatchPostCollector.class);

  private final SimRailPointProvider pointProvider;
  private final SimRailServerService serverService;
  private final UuidV5Factory dispatchPostIdFactory;
  private final SimRailPanelApiClient panelApiClient;
  private final Map<UUID, String> postDataEtagByServer;
  private final DispatchPostUpdateHandler dispatchPostUpdateHandler;
  private final SimRailDispatchPostRepository dispatchPostRepository;

  private final PerServerGauge collectedDispatchPostCounter;
  private final Meter.MeterProvider<Timer> collectionDurationTimer;

  @Autowired
  public SimRailDispatchPostCollector(
    @Nonnull SimRailPointProvider pointProvider,
    @Nonnull SimRailServerService serverService,
    @Nonnull SimRailPanelApiClient panelApiClient,
    @Nonnull DispatchPostUpdateHandler dispatchPostUpdateHandler,
    @Nonnull SimRailDispatchPostRepository dispatchPostRepository,
    @Nonnull @Qualifier("dispatch_post_collected_total") PerServerGauge collectedDispatchPostCounter,
    @Nonnull @Qualifier("dispatch_post_collection_duration") Meter.MeterProvider<Timer> collectionDurationTimer
  ) {
    this.pointProvider = pointProvider;
    this.serverService = serverService;
    this.panelApiClient = panelApiClient;
    this.postDataEtagByServer = new HashMap<>();
    this.dispatchPostRepository = dispatchPostRepository;
    this.dispatchPostUpdateHandler = dispatchPostUpdateHandler;
    this.dispatchPostIdFactory = new UuidV5Factory(SimRailDispatchPostEntity.ID_NAMESPACE);

    this.collectionDurationTimer = collectionDurationTimer;
    this.collectedDispatchPostCounter = collectedDispatchPostCounter;
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
      // get the train positions from upstream api, don't do anything if data didn't change
      var sample = Timer.start();
      var etag = this.postDataEtagByServer.get(server.id());
      var responseTuple = this.panelApiClient.getDispatchPosts(server.code(), etag);
      responseTuple
        .firstHeaderValue(HttpHeaders.ETAG)
        .ifPresent(responseEtag -> this.postDataEtagByServer.put(server.id(), responseEtag));
      if (responseTuple.response().status() == HttpStatus.NOT_MODIFIED.value()) {
        continue;
      }

      var response = responseTuple.body();
      var dispatchPosts = response == null ? null : response.getEntries();
      if (dispatchPosts == null || dispatchPosts.isEmpty()) {
        LOGGER.warn("API did not return a successful result while getting dispatch post list on {}", server.code());
        continue;
      }

      //
      var registeredDispatchPostsByForeignId = this.dispatchPostRepository.findAllByServerId(server.id())
        .stream()
        .collect(Collectors.toMap(SimRailDispatchPostEntity::getForeignId, Function.identity()));
      for (var dispatchPost : dispatchPosts) {
        var postEntity = registeredDispatchPostsByForeignId.remove(dispatchPost.getId());
        if (postEntity == null) {
          // dispatch post is not yet registered, create a new entity for it
          postEntity = new SimRailDispatchPostEntity();
          postEntity.setNew(true);
          postEntity.setServerId(server.id());
          postEntity.setForeignId(dispatchPost.getId());

          var idName = server.code() + dispatchPost.getStationName() + dispatchPost.getId();
          postEntity.setId(this.dispatchPostIdFactory.create(idName));
        }

        // store if the post was deleted before, and then remove the marking
        // this can happen if a posts gets removed and then re-added
        var postWasDeleted = postEntity.isDeleted();
        postEntity.setDeleted(false);

        // update the base information
        postEntity.setName(dispatchPost.getStationName());
        postEntity.setDifficultyLevel(dispatchPost.getDifficulty());
        postEntity.setPosition(new GeoPositionEntity(
          dispatchPost.getPositionLatitude(),
          dispatchPost.getPositionLongitude()));

        // override position for Miechów as the upstream provided position is way off (in the middle of a field)
        // TODO: remove when position is fixed in upstream
        if (dispatchPost.getId().equals("675330d44337b38ac4027545")) {
          postEntity.setPosition(new GeoPositionEntity(50.354694, 20.011680));
        }

        // update the time when the post was initially registered in the SimRail backend
        var registeredSince = MongoIdDecodeUtil.parseMongoId(dispatchPost.getId());
        postEntity.setRegisteredSince(registeredSince);

        // update the associated point id
        var point = this.pointProvider.findPointByName(dispatchPost.getStationName()).orElse(null);
        if (point == null) {
          LOGGER.warn("Found dispatch post {} with no associated point", dispatchPost.getStationName());
          continue;
        }
        postEntity.setPointId(point.getId());

        // update the image urls of the post (in case they changed)
        var allImages = List.of(
          dispatchPost.getImage1Url(),
          dispatchPost.getImage2Url(),
          dispatchPost.getImage3Url());
        var newImageUrls = new HashSet<>(allImages);
        var currentImageUrls = postEntity.getImageUrls();
        if (!newImageUrls.equals(currentImageUrls)) {
          postEntity.setImageUrls(newImageUrls);
        }

        // update the collection of steam ids that are currently dispatching the post (in case they changed)
        var newDispatcherSteamIds = dispatchPost.getDispatchers().stream()
          .map(SimRailPanelDispatchPost.Dispatcher::getSteamId)
          .collect(Collectors.toSet());
        var currentDispatcherSteamIds = postEntity.getDispatcherSteamIds();
        if (!newDispatcherSteamIds.equals(currentDispatcherSteamIds)) {
          postEntity.setDispatcherSteamIds(newDispatcherSteamIds);
          if (!postEntity.isNew()) {
            // if the post entity is new we will send out an update separately
            this.dispatchPostUpdateHandler.handleDispatchPostUpdate(postEntity);
          }
        }

        // save the updated post entity, send out and info if the post is newly registered
        var savedEntity = this.dispatchPostRepository.save(postEntity);
        if (postEntity.isNew() || postWasDeleted) {
          this.dispatchPostUpdateHandler.handleDispatchPostAdd(savedEntity);
        }
      }

      // mark all dispatch posts that weren't removed in the collection cycle as deleted
      var remainingRegisteredPosts = registeredDispatchPostsByForeignId.values();
      if (!remainingRegisteredPosts.isEmpty()) {
        remainingRegisteredPosts.forEach(post -> {
          post.setDeleted(true);
          this.dispatchPostUpdateHandler.handleDispatchPostRemove(post);
        });
        this.dispatchPostRepository.saveAll(remainingRegisteredPosts);
      }

      this.collectedDispatchPostCounter.setValue(server, dispatchPosts.size());
      sample.stop(this.collectionDurationTimer.withTag("server_code", server.code()));
    }
  }
}
