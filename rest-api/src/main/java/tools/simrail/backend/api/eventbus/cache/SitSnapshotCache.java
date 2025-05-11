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

package tools.simrail.backend.api.eventbus.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
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

  // the grace period in seconds before a snapshot becomes eligible for cache purging
  private static final int CLEANUP_GRACE_SECONDS = 15;

  private final EventbusServerRepository serverRepository;
  private final EventbusJourneyRepository journeyRepository;
  private final EventbusDispatchPostRepository dispatchPostRepository;

  private final Map<String, SnapshotRegistration<EventbusServerSnapshotDto>> serverSnapshots;
  private final Map<String, SnapshotRegistration<EventbusJourneySnapshotDto>> journeySnapshots;
  private final Map<String, SnapshotRegistration<EventbusDispatchPostSnapshotDto>> dispatchPostSnapshots;

  private final Cache<String, Boolean> removedIdsCache;

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
      server -> server.getServerId().toString(),
      SnapshotRegistration::new,
      (left, _) -> left,
      ConcurrentHashMap::new));

    // cache the active journeys
    var activeJourneys = journeyRepository.findSnapshotsOfAllActiveJourneys();
    this.journeySnapshots = activeJourneys.stream().collect(Collectors.toMap(
      journey -> journey.getJourneyId().toString(),
      SnapshotRegistration::new,
      (left, _) -> left,
      ConcurrentHashMap::new));

    // cache active dispatch posts
    var activeDispatchPosts = dispatchPostRepository.findSnapshotsOfAllActiveDispatchPosts();
    this.dispatchPostSnapshots = activeDispatchPosts.stream().collect(Collectors.toMap(
      post -> post.getPostId().toString(),
      SnapshotRegistration::new,
      (left, _) -> left,
      ConcurrentHashMap::new));

    // construct the cache for the ids that were removed to not apply any updates anymore
    // once a remove frame for them was received. after 2 minutes in the cache they are removed
    // from the cache and the caching maps are cleaned up one more time to ensure that the id
    // is actually no longer cached due to some race condition
    this.removedIdsCache = Caffeine.newBuilder()
      .expireAfterWrite(2, TimeUnit.MINUTES)
      .evictionListener((key, _, cause) -> {
        if (cause == RemovalCause.EXPIRED) {
          var removedId = (String) key;
          this.serverSnapshots.remove(removedId);
          this.journeySnapshots.remove(removedId);
          this.dispatchPostSnapshots.remove(removedId);
        }
      })
      .build();
  }

  /**
   * Marks the given id as removed for a short amount of time to prevent further updates from being applied.
   *
   * @param id the id to mark as removed.
   */
  private void markIdAsRemoved(@Nonnull String id) {
    this.removedIdsCache.put(id, Boolean.TRUE);
  }

  /**
   * Removes the marking as removed from the given id.
   *
   * @param id the id to no longer mark as removed.
   */
  private void unmarkIdAsRemoved(@Nonnull String id) {
    this.removedIdsCache.invalidate(id);
  }

  /**
   * Get if the given id has been marked as removed previously.
   *
   * @param id the id to check.
   * @return true if the given id has been marked as removed, false otherwise.
   */
  private boolean isIdMarkedAsRemoved(@Nonnull String id) {
    return this.removedIdsCache.getIfPresent(id) != null;
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
      this.markIdAsRemoved(frame.getServerId());
      var registration = this.serverSnapshots.remove(frame.getServerId());
      return registration == null ? null : registration.snapshot();
    }

    // remove the removal marking for the id of the given server if the action is an add
    if (updateType == UpdateType.ADD) {
      this.unmarkIdAsRemoved(frame.getServerId());
    }

    // resolve the server with the provided server id and cache it
    // apply the frame as an update in case the server was updated
    var registration = this.serverSnapshots.computeIfAbsent(frame.getServerId(), _ -> {
      var serverId = UUID.fromString(frame.getServerId());
      return this.serverRepository.findServerSnapshotById(serverId)
        .filter(_ -> !this.isIdMarkedAsRemoved(frame.getServerId()))
        .map(SnapshotRegistration::new)
        .orElse(null);
    });
    var serverToUpdate = registration == null ? null : registration.snapshot();
    if (serverToUpdate != null) {
      serverToUpdate.applyUpdateFrame(frame);
    }

    // check if the id was marked as removed while the update/add was applied
    // to prevent caching something that will never receive an update again
    if (registration != null && this.isIdMarkedAsRemoved(frame.getServerId())) {
      this.serverSnapshots.remove(frame.getServerId());
      return null;
    }

    return serverToUpdate;
  }

  /**
   * Handles the update of the journey represented with the given update frame.
   *
   * @param frame the update frame to apply to a journey.
   * @return the locally cached snapshot of the journey that was updated.
   */
  public @Nullable Pair<JourneyUpdateFrame, EventbusJourneySnapshotDto> handleJourneyUpdateFrame(
    @Nonnull JourneyUpdateFrame frame
  ) {
    // handle the remove of a journey
    var updateType = frame.getUpdateType();
    if (updateType == UpdateType.REMOVE) {
      this.markIdAsRemoved(frame.getJourneyId());
      var registration = this.journeySnapshots.remove(frame.getJourneyId());
      return registration == null ? null : Pair.of(frame, registration.snapshot());
    }

    if (updateType == UpdateType.ADD) {
      // remove the removal marking for the id of the given journey is added newly
      this.unmarkIdAsRemoved(frame.getJourneyId());
    } else if (this.isIdMarkedAsRemoved(frame.getJourneyId())) {
      // in all other cases: if the id is marked as removed, there is nothing to do
      return null;
    }

    // resolve the journey with the provided journey id and cache it
    // apply the frame as an update in case the journey was updated
    var registration = this.journeySnapshots.computeIfAbsent(frame.getJourneyId(), jid -> {
      var journeyId = UUID.fromString(jid);
      return this.journeyRepository.findJourneySnapshotById(journeyId)
        .filter(_ -> !this.isIdMarkedAsRemoved(jid))
        .map(SnapshotRegistration::new)
        .orElse(null);
    });
    if (registration == null) {
      return null;
    }

    // if the registration was computed for the first time, this frame should be sent as
    // an ADD frame rather than an update to ensure that all clients have the relevant data
    var expectedUpdateType = registration.isFirstUse() ? UpdateType.ADD : UpdateType.UPDATE;
    if (frame.getUpdateType() != expectedUpdateType) {
      frame = frame.toBuilder().setUpdateType(expectedUpdateType).build();
    }

    // apply the received frame to the stored journey snapshot
    var journeyToUpdate = registration.snapshot();
    journeyToUpdate.applyUpdateFrame(frame);

    // check if the id was marked as removed while the update/add was applied
    // to prevent caching something that will never receive an update again
    if (this.isIdMarkedAsRemoved(frame.getJourneyId())) {
      this.journeySnapshots.remove(frame.getJourneyId());
      return null;
    }

    return Pair.of(frame, journeyToUpdate);
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
      this.markIdAsRemoved(frame.getPostId());
      var registration = this.dispatchPostSnapshots.remove(frame.getPostId());
      return registration == null ? null : registration.snapshot();
    }

    // remove the removal marking for the id of the given dispatch post if the action is an add
    if (updateType == UpdateType.ADD) {
      this.unmarkIdAsRemoved(frame.getPostId());
    }

    // resolve the dispatch post with the provided dispatch post id and cache it
    // apply the frame as an update in case the dispatch post was updated
    var registration = this.dispatchPostSnapshots.computeIfAbsent(frame.getPostId(), _ -> {
      var postId = UUID.fromString(frame.getPostId());
      return this.dispatchPostRepository.findDispatchPostSnapshotById(postId)
        .filter(_ -> !this.isIdMarkedAsRemoved(frame.getPostId()))
        .map(SnapshotRegistration::new)
        .orElse(null);
    });
    var postToUpdate = registration == null ? null : registration.snapshot();
    if (postToUpdate != null) {
      postToUpdate.applyUpdateFrame(frame);
    }

    // check if the id was marked as removed while the update/add was applied
    // to prevent caching something that will never receive an update again
    if (registration != null && this.isIdMarkedAsRemoved(frame.getPostId())) {
      this.dispatchPostSnapshots.remove(frame.getPostId());
      return null;
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
    return Optional.ofNullable(this.serverSnapshots.get(id)).map(SnapshotRegistration::snapshot);
  }

  /**
   * Get the cached journey snapshot by the given id, if one is cached locally.
   *
   * @param id the id of the journey to get.
   * @return an optional holding the cached journey snapshot, if one exists.
   */
  public @Nonnull Optional<EventbusJourneySnapshotDto> findCachedJourney(@Nonnull String id) {
    return Optional.ofNullable(this.journeySnapshots.get(id)).map(SnapshotRegistration::snapshot);
  }

  /**
   * Get the cached dispatch post snapshot by the given id, if one is cached locally.
   *
   * @param id the id of the dispatch post to get.
   * @return an optional holding the cached dispatch post snapshot, if one exists.
   */
  public @Nonnull Optional<EventbusDispatchPostSnapshotDto> findCachedDispatchPost(@Nonnull String id) {
    return Optional.ofNullable(this.dispatchPostSnapshots.get(id)).map(SnapshotRegistration::snapshot);
  }

  /**
   * Get the server snapshots that are cached locally.
   *
   * @return the server snapshots that are cached locally.
   */
  public @Nonnull Stream<EventbusServerSnapshotDto> getCachedServerSnapshots() {
    return this.serverSnapshots.values().stream().map(SnapshotRegistration::snapshot);
  }

  /**
   * Get the journey snapshots that are cached locally.
   *
   * @return the journey snapshots that are cached locally.
   */
  public @Nonnull Stream<EventbusJourneySnapshotDto> getCachedJourneySnapshots() {
    return this.journeySnapshots.values().stream().map(SnapshotRegistration::snapshot);
  }

  /**
   * Get the dispatch post snapshots that are cached locally.
   *
   * @return the dispatch post snapshots that are cached locally.
   */
  public @Nonnull Stream<EventbusDispatchPostSnapshotDto> getCachedDispatchPostSnapshots() {
    return this.dispatchPostSnapshots.values().stream().map(SnapshotRegistration::snapshot);
  }

  /**
   * Cleans up all journey snapshots from the cache that are no longer active.
   *
   * @param activeJourneyIds the ids of the journeys that are still active.
   */
  @Nonnull
  Collection<EventbusJourneySnapshotDto> cleanupJourneySnapshots(@Nonnull Collection<UUID> activeJourneyIds) {
    var now = Instant.now();
    var removed = new ArrayList<EventbusJourneySnapshotDto>();

    var journeyRegistrationIterator = this.journeySnapshots.values().iterator();
    while (journeyRegistrationIterator.hasNext()) {
      var registration = journeyRegistrationIterator.next();
      var snapshot = registration.snapshot();
      var registeredSeconds = Duration.between(registration.registeredAt(), now).toSeconds();
      if (!activeJourneyIds.contains(snapshot.getJourneyId()) && registeredSeconds > CLEANUP_GRACE_SECONDS) {
        removed.add(registration.snapshot());
        journeyRegistrationIterator.remove();
      }
    }

    return removed;
  }

  /**
   * A registration of a snapshot, associated with the time when the snapshot was registered.
   *
   * @param registeredAt the time when the snapshot was registered (ADD frame received by collector)
   * @param snapshot     the snapshot.
   * @param <S>          the type of the snapshot.
   */
  private record SnapshotRegistration<S>(
    @Nonnull Instant registeredAt,
    @Nonnull S snapshot,
    @Nonnull AtomicBoolean isNew
  ) {

    /**
     * Creates a new snapshot registration instance with the current timestamp.
     *
     * @param snapshot the snapshot.
     */
    private SnapshotRegistration(@Nonnull S snapshot) {
      this(Instant.now(), snapshot, new AtomicBoolean(true));
    }

    /**
     * Get if this registration was used before, changing the value to false.
     *
     * @return true if this is the first call to the method, false otherwise.
     */
    public boolean isFirstUse() {
      return this.isNew.compareAndSet(true, false);
    }
  }
}
