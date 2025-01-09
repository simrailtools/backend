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

package tools.simrail.backend.api.eventbus.rpc;

import jakarta.annotation.Nonnull;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.simrail.backend.api.eventbus.SitEventbusListener;
import tools.simrail.backend.api.eventbus.cache.SitSnapshotCache;
import tools.simrail.backend.common.rpc.DispatchPostUpdateFrame;
import tools.simrail.backend.common.rpc.JourneyUpdateFrame;
import tools.simrail.backend.common.rpc.ServerUpdateFrame;

/**
 * Bridging handler between the collector backend and the listening clients. The handler receives an update frame
 * published by the backend, passes it to the snapshot cache which resolves, updates and caches a snapshot of the
 * requested object locally. If the cache was able to resolve the snapshot, the value is then passed to the client
 * session manager which sends out and update frame to subscribed clients.
 */
@Component
public final class EventbusRpcStreamFrameHandler {

  private final SitSnapshotCache snapshotCache;
  private final List<SitEventbusListener> eventBusListeners;

  @Autowired
  public EventbusRpcStreamFrameHandler(
    @Nonnull SitSnapshotCache snapshotCache,
    @Nonnull List<SitEventbusListener> eventBusListeners
  ) {
    this.snapshotCache = snapshotCache;
    this.eventBusListeners = eventBusListeners;
  }

  /**
   * Handles the given incoming server update frame as described.
   *
   * @param frame the server update frame that was received.
   */
  public void handleServerUpdate(@Nonnull ServerUpdateFrame frame) {
    var serverSnapshot = this.snapshotCache.handleServerUpdateFrame(frame);
    if (serverSnapshot != null) {
      this.eventBusListeners.forEach(listener -> listener.handleServerUpdate(frame, serverSnapshot));
    }
  }

  /**
   * Handles the given incoming journey update frame as described.
   *
   * @param frame the journey update frame that was received.
   */
  public void handleJourneyUpdate(@Nonnull JourneyUpdateFrame frame) {
    var journeySnapshot = this.snapshotCache.handleJourneyUpdateFrame(frame);
    if (journeySnapshot != null) {
      this.eventBusListeners.forEach(listener -> listener.handleJourneyUpdate(frame, journeySnapshot));
    }
  }

  /**
   * Handles the given incoming dispatch post update frame as described.
   *
   * @param frame the dispatch post update frame that was received.
   */
  public void handleDispatchPostUpdate(@Nonnull DispatchPostUpdateFrame frame) {
    var dispatchPostSnapshot = this.snapshotCache.handleDispatchPostUpdateFrame(frame);
    if (dispatchPostSnapshot != null) {
      this.eventBusListeners.forEach(listener -> listener.handleDispatchPostUpdate(frame, dispatchPostSnapshot));
    }
  }
}
