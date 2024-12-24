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

package tools.simrail.backend.api.eventbus.cache;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.simrail.backend.api.eventbus.dto.EventbusDispatchPostSnapshotDto;
import tools.simrail.backend.api.eventbus.dto.EventbusJourneySnapshotDto;
import tools.simrail.backend.api.eventbus.dto.EventbusServerSnapshotDto;
import tools.simrail.backend.common.rpc.DispatchPostUpdateFrame;
import tools.simrail.backend.common.rpc.JourneyUpdateFrame;
import tools.simrail.backend.common.rpc.ServerUpdateFrame;
import tools.simrail.backend.common.rpc.UpdateType;

/**
 * Cache for snapshot data being updated from the internal event bus.
 */
@Component
public final class SitSnapshotCache {

  private final EventbusServerRepository serverRepository;
  private final EventbusJourneyRepository journeyRepository;
  private final EventbusDispatchPostRepository dispatchPostRepository;

  private final Map<String, EventbusServerSnapshotDto> serverSnapshots;
  private final Map<String, EventbusJourneySnapshotDto> journeySnapshots;
  private final Map<String, EventbusDispatchPostSnapshotDto> dispatchPostSnapshots;

  @Autowired
  SitSnapshotCache(
    @Nonnull EventbusServerRepository serverRepository,
    @Nonnull EventbusJourneyRepository journeyRepository,
    @Nonnull EventbusDispatchPostRepository dispatchPostRepository
  ) {
    this.serverRepository = serverRepository;
    this.journeyRepository = journeyRepository;
    this.dispatchPostRepository = dispatchPostRepository;

    // cache active servers
    var activeServers = serverRepository.findSnapshotsOfAllActiveServers();
    this.serverSnapshots = activeServers.stream().collect(Collectors.toMap(
      server -> server.getId().toString(),
      Function.identity(),
      (left, _) -> left,
      ConcurrentHashMap::new));

    // cache the active journeys
    var activeJourneys = journeyRepository.findSnapshotsOfAllActiveJourneys();
    this.journeySnapshots = activeJourneys.stream().collect(Collectors.toMap(
      journey -> journey.getJourneyId().toString(),
      Function.identity(),
      (left, _) -> left,
      ConcurrentHashMap::new));

    // cache active dispatch posts
    var activeDispatchPosts = dispatchPostRepository.findSnapshotsOfAllActiveDispatchPosts();
    this.dispatchPostSnapshots = activeDispatchPosts.stream().collect(Collectors.toMap(
      post -> post.getId().toString(),
      Function.identity(),
      (left, _) -> left,
      ConcurrentHashMap::new));
  }

  /**
   * Handles the update of the server represented with the given update frame.
   *
   * @param frame the update frame to apply to a server.
   * @return the locally cached snapshot of the server that was updated.
   */
  public @Nullable EventbusServerSnapshotDto handleServerUpdateFrame(@Nonnull ServerUpdateFrame frame) {
    // handle the remove of a server
    var updateType = frame.getUpdateType();
    if (updateType == UpdateType.REMOVE) {
      return this.serverSnapshots.remove(frame.getServerId());
    }

    // resolve the server with the provided server id and cache it
    // apply the frame as an update in case the server was updated
    var serverToUpdate = this.serverSnapshots.computeIfAbsent(frame.getServerId(), _ -> {
      var serverId = UUID.fromString(frame.getServerId());
      return this.serverRepository.findServerSnapshotById(serverId).orElse(null);
    });
    if (serverToUpdate != null) {
      serverToUpdate.applyUpdateFrame(frame);
    }

    return serverToUpdate;
  }

  /**
   * Handles the update of the journey represented with the given update frame.
   *
   * @param frame the update frame to apply to a journey.
   * @return the locally cached snapshot of the journey that was updated.
   */
  public @Nullable EventbusJourneySnapshotDto handleJourneyUpdateFrame(@Nonnull JourneyUpdateFrame frame) {
    // handle the remove of a journey
    var updateType = frame.getUpdateType();
    if (updateType == UpdateType.REMOVE) {
      return this.journeySnapshots.remove(frame.getJourneyId());
    }

    // resolve the journey with the provided journey id and cache it
    // apply the frame as an update in case the journey was updated
    var journeyToUpdate = this.journeySnapshots.computeIfAbsent(frame.getJourneyId(), _ -> {
      var journeyId = UUID.fromString(frame.getJourneyId());
      return this.journeyRepository.findJourneySnapshotById(journeyId).orElse(null);
    });
    if (journeyToUpdate != null) {
      journeyToUpdate.applyUpdateFrame(frame);
    }

    return journeyToUpdate;
  }

  /**
   * Handles the update of the dispatch post represented with the given update frame.
   *
   * @param frame the update frame to apply to a dispatch post.
   * @return the locally cached snapshot of the dispatch post that was updated.
   */
  public @Nullable EventbusDispatchPostSnapshotDto handleDispatchPostUpdateFrame(
    @Nonnull DispatchPostUpdateFrame frame
  ) {
    // handle the remove of a dispatch post
    var updateType = frame.getUpdateType();
    if (updateType == UpdateType.REMOVE) {
      return this.dispatchPostSnapshots.remove(frame.getPostId());
    }

    // resolve the dispatch post with the provided dispatch post id and cache it
    // apply the frame as an update in case the dispatch post was updated
    var postToUpdate = this.dispatchPostSnapshots.computeIfAbsent(frame.getPostId(), _ -> {
      var postId = UUID.fromString(frame.getPostId());
      return this.dispatchPostRepository.findDispatchPostSnapshotById(postId).orElse(null);
    });
    if (postToUpdate != null) {
      postToUpdate.applyUpdateFrame(frame);
    }

    return postToUpdate;
  }

  /**
   * Get the cached server snapshot by the given id, if one is cached locally.
   *
   * @param id the id of the server to get.
   * @return an optional holding the cached server snapshot, if one exists.
   */
  public @Nonnull Optional<EventbusServerSnapshotDto> findCachedServer(@Nonnull String id) {
    return Optional.ofNullable(this.serverSnapshots.get(id));
  }

  /**
   * Get the cached journey snapshot by the given id, if one is cached locally.
   *
   * @param id the id of the journey to get.
   * @return an optional holding the cached journey snapshot, if one exists.
   */
  public @Nonnull Optional<EventbusJourneySnapshotDto> findCachedJourney(@Nonnull String id) {
    return Optional.ofNullable(this.journeySnapshots.get(id));
  }

  /**
   * Get the cached dispatch post snapshot by the given id, if one is cached locally.
   *
   * @param id the id of the dispatch post to get.
   * @return an optional holding the cached dispatch post snapshot, if one exists.
   */
  public @Nonnull Optional<EventbusDispatchPostSnapshotDto> findCachedDispatchPost(@Nonnull String id) {
    return Optional.ofNullable(this.dispatchPostSnapshots.get(id));
  }

  /**
   * Get the server snapshots that are cached locally.
   *
   * @return the server snapshots that are cached locally.
   */
  public @Nonnull Collection<EventbusServerSnapshotDto> getCachedServerSnapshots() {
    return this.serverSnapshots.values();
  }

  /**
   * Get the journey snapshots that are cached locally.
   *
   * @return the journey snapshots that are cached locally.
   */
  public @Nonnull Collection<EventbusJourneySnapshotDto> getCachedJourneySnapshots() {
    return this.journeySnapshots.values();
  }

  /**
   * Get the dispatch post snapshots that are cached locally.
   *
   * @return the dispatch post snapshots that are cached locally.
   */
  public @Nonnull Collection<EventbusDispatchPostSnapshotDto> getCachedDispatchPostSnapshots() {
    return this.dispatchPostSnapshots.values();
  }
}