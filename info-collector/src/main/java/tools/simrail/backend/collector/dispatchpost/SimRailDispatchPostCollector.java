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

package tools.simrail.backend.collector.dispatchpost;

import jakarta.annotation.Nonnull;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
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
  private final DispatchPostUpdateHandler dispatchPostUpdateHandler;
  private final SimRailDispatchPostRepository dispatchPostRepository;

  @Autowired
  public SimRailDispatchPostCollector(
    @Nonnull SimRailPointProvider pointProvider,
    @Nonnull SimRailServerService serverService,
    @Nonnull DispatchPostUpdateHandler dispatchPostUpdateHandler,
    @Nonnull SimRailDispatchPostRepository dispatchPostRepository
  ) {
    this.pointProvider = pointProvider;
    this.serverService = serverService;
    this.dispatchPostRepository = dispatchPostRepository;
    this.dispatchPostUpdateHandler = dispatchPostUpdateHandler;
    this.panelApiClient = SimRailPanelApiClient.create();
    this.dispatchPostIdFactory = new UuidV5Factory(SimRailDispatchPostEntity.ID_NAMESPACE);
  }

  @Scheduled(initialDelay = 0, fixedRate = 7, timeUnit = TimeUnit.SECONDS)
  public void collectDispatchPostInformation() {
    var servers = this.serverService.getServers();
    for (var server : servers) {
      var response = this.panelApiClient.getDispatchPosts(server.code());
      var dispatchPosts = response.getEntries();
      if (!response.isSuccess() || dispatchPosts == null || dispatchPosts.isEmpty()) {
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

        // update the base information
        postEntity.setName(dispatchPost.getStationName());
        postEntity.setDifficultyLevel(dispatchPost.getDifficulty());
        postEntity.setPosition(new GeoPositionEntity(
          dispatchPost.getPositionLatitude(),
          dispatchPost.getPositionLongitude()));

        // update the time when the post was initially registered in the SimRail backend
        var registeredSince = MongoIdDecodeUtil.parseMongoId(dispatchPost.getId());
        postEntity.setRegisteredSince(registeredSince);

        // update the associated point id
        var point = this.pointProvider
          .findPointByName(dispatchPost.getStationName())
          .orElseThrow(() -> new NoSuchElementException("Missing point for " + dispatchPost.getStationName()));
        postEntity.setPointId(point.getId());

        // update the image urls of the post (in case they changed)
        var newImageUrls = Set.of(
          dispatchPost.getImage1Url(),
          dispatchPost.getImage2Url(),
          dispatchPost.getImage3Url());
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
        if (postEntity.isNew()) {
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
    }
  }
}
