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

package tools.simrail.backend.api.event.eventbus;

import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.simrail.backend.api.event.session.EventSessionManager;
import tools.simrail.backend.api.eventbus.SitEventbusListener;
import tools.simrail.backend.api.eventbus.dto.EventbusDispatchPostSnapshotDto;
import tools.simrail.backend.api.eventbus.dto.EventbusJourneySnapshotDto;
import tools.simrail.backend.api.eventbus.dto.EventbusServerSnapshotDto;
import tools.simrail.backend.common.rpc.DispatchPostUpdateFrame;
import tools.simrail.backend.common.rpc.JourneyUpdateFrame;
import tools.simrail.backend.common.rpc.ServerUpdateFrame;

/**
 * Eventbus lister that passes updates to all connected websocket clients.
 */
@Component
final class EventEventbusListener implements SitEventbusListener {

  private final EventSessionManager clientSessionManager;

  @Autowired
  public EventEventbusListener(@Nonnull EventSessionManager clientSessionManager) {
    this.clientSessionManager = clientSessionManager;
  }

  @Override
  public void handleServerUpdate(@Nonnull ServerUpdateFrame frame, @Nonnull EventbusServerSnapshotDto snapshot) {
    this.clientSessionManager.handleServerUpdate(frame, snapshot);
  }

  @Override
  public void handleJourneyUpdate(@Nonnull JourneyUpdateFrame frame, @Nonnull EventbusJourneySnapshotDto snapshot) {
    this.clientSessionManager.handleJourneyUpdate(frame, snapshot);
  }

  @Override
  public void handleDispatchPostUpdate(
    @Nonnull DispatchPostUpdateFrame frame,
    @Nonnull EventbusDispatchPostSnapshotDto snapshot
  ) {
    this.clientSessionManager.handleDispatchPostUpdate(frame, snapshot);
  }
}
