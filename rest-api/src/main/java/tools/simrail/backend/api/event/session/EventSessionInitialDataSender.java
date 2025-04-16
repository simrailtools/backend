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

package tools.simrail.backend.api.event.session;

import jakarta.annotation.Nonnull;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.simrail.backend.api.event.registration.EventFrameRegistrationRequest;
import tools.simrail.backend.api.eventbus.cache.SitSnapshotCache;
import tools.simrail.backend.api.eventbus.dto.EventbusDispatchPostSnapshotDto;
import tools.simrail.backend.api.eventbus.dto.EventbusJourneySnapshotDto;
import tools.simrail.backend.api.eventbus.dto.EventbusServerSnapshotDto;
import tools.simrail.backend.common.rpc.DispatchPostUpdateFrame;
import tools.simrail.backend.common.rpc.JourneyUpdateFrame;
import tools.simrail.backend.common.rpc.ServerUpdateFrame;
import tools.simrail.backend.common.rpc.UpdateType;

/**
 * Sender that sends initial frame data to clients after they successfully subscribed.
 */
@Component
final class EventSessionInitialDataSender {

  private final SitSnapshotCache snapshotCache;

  @Autowired
  public EventSessionInitialDataSender(SitSnapshotCache snapshotCache) {
    this.snapshotCache = snapshotCache;
  }

  /**
   * Sends out the initial cached frames for a newly added subscription based on the given request.
   *
   * @param session the session that subscribed and needs the initial snapshots.
   * @param request the request that was sent for subscribing to updates.
   */
  public void sendInitialDataFrames(
    @Nonnull EventWebsocketSession session,
    @Nonnull EventFrameRegistrationRequest request
  ) {
    var dataId = request.dataId();
    if (dataId != null) {
      Object snapshot = switch (request.frameType()) {
        case SERVER -> this.snapshotCache.findCachedServer(dataId.toString()).orElse(null);
        case DISPATCH_POST -> this.snapshotCache.findCachedDispatchPost(dataId.toString()).orElse(null);
        case JOURNEY_POSITION -> this.snapshotCache.findCachedJourney(dataId.toString()).orElse(null);
        case JOURNEY_DETAILS -> null; // don't send initial data for journey details, only updates
      };
      if (snapshot != null) {
        this.sendInitialDataFrame(session, snapshot);
      }
    } else {
      var serverIdFilter = request.serverId();
      Stream<?> snapshots = switch (request.frameType()) {
        case SERVER -> this.snapshotCache.getCachedServerSnapshots()
          .stream()
          .filter(snapshot -> snapshot.getServerId().equals(serverIdFilter));
        case DISPATCH_POST -> this.snapshotCache.getCachedDispatchPostSnapshots()
          .stream()
          .filter(snapshot -> snapshot.getServerId().equals(serverIdFilter));
        case JOURNEY_POSITION -> this.snapshotCache.getCachedJourneySnapshots()
          .stream()
          .filter(snapshot -> snapshot.getServerId().equals(serverIdFilter));
        case JOURNEY_DETAILS -> null; // don't send initial data for journey details, only updates
      };
      if (snapshots != null) {
        snapshots.forEach(snapshot -> this.sendInitialDataFrame(session, snapshot));
      }
    }
  }

  /**
   * Sends out an initial data frame for the given snapshot to the given session.
   *
   * @param session  the session that subscribed and needs the initial snapshot.
   * @param snapshot the locally cached snapshot for the subscribed data to send to the client.
   */
  private void sendInitialDataFrame(@Nonnull EventWebsocketSession session, @Nonnull Object snapshot) {
    Object updateFrame = switch (snapshot) {
      case EventbusServerSnapshotDto serverSnapshot -> ServerUpdateFrame.newBuilder()
        .setUpdateType(UpdateType.ADD)
        .setServerId(serverSnapshot.getServerId().toString())
        .buildPartial();
      case EventbusDispatchPostSnapshotDto postSnapshot -> DispatchPostUpdateFrame.newBuilder()
        .setUpdateType(UpdateType.ADD)
        .setPostId(postSnapshot.getPostId().toString())
        .setServerId(postSnapshot.getServerId().toString())
        .buildPartial();
      case EventbusJourneySnapshotDto journeySnapshot -> JourneyUpdateFrame.newBuilder()
        .setUpdateType(UpdateType.ADD)
        .setServerId(journeySnapshot.getServerId().toString())
        .setJourneyId(journeySnapshot.getJourneyId().toString())
        .buildPartial();
      default -> null; // unknown snapshot, cannot build update frame
    };
    if (updateFrame != null) {
      session.publishUpdateFrame(snapshot, updateFrame);
    }
  }
}
