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

package tools.simrail.backend.api.eventbus;

import jakarta.annotation.Nonnull;
import tools.simrail.backend.api.eventbus.dto.EventbusDispatchPostSnapshotDto;
import tools.simrail.backend.api.eventbus.dto.EventbusJourneySnapshotDto;
import tools.simrail.backend.api.eventbus.dto.EventbusServerSnapshotDto;
import tools.simrail.backend.common.rpc.DispatchPostUpdateFrame;
import tools.simrail.backend.common.rpc.JourneyUpdateFrame;
import tools.simrail.backend.common.rpc.ServerUpdateFrame;

/**
 * Listener for update frames being sent by the collector.
 */
public interface SitEventbusListener {

  /**
   * Handles the update a server.
   *
   * @param frame    the update frame that was sent to the collector.
   * @param snapshot the snapshot of the server that was updated.
   */
  default void handleServerUpdate(@Nonnull ServerUpdateFrame frame, @Nonnull EventbusServerSnapshotDto snapshot) {
  }

  /**
   * Handles the update a journey.
   *
   * @param frame    the update frame that was sent to the collector.
   * @param snapshot the snapshot of the journey that was updated.
   */
  default void handleJourneyUpdate(@Nonnull JourneyUpdateFrame frame, @Nonnull EventbusJourneySnapshotDto snapshot) {
  }

  /**
   * Handles the update a dispatch post.
   *
   * @param frame    the update frame that was sent to the collector.
   * @param snapshot the snapshot of the dispatch post that was updated.
   */
  default void handleDispatchPostUpdate(
    @Nonnull DispatchPostUpdateFrame frame,
    @Nonnull EventbusDispatchPostSnapshotDto snapshot
  ) {
  }
}
