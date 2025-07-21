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

package tools.simrail.backend.collector.server;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.simrail.backend.collector.rpc.InternalRpcEventBusService;
import tools.simrail.backend.common.rpc.ServerUpdateFrame;
import tools.simrail.backend.common.rpc.UpdateType;
import tools.simrail.backend.common.server.SimRailServerEntity;

/**
 * Handles updates of servers and sends update frames to the registered listeners.
 */
@Component
final class ServerUpdateHandler {

  private final InternalRpcEventBusService internalRpcEventBusService;

  @Autowired
  public ServerUpdateHandler(@Nonnull InternalRpcEventBusService internalRpcEventBusService) {
    this.internalRpcEventBusService = internalRpcEventBusService;
  }

  /**
   * Handles the update of a single server. It is not guaranteed that the provided server entity actually contains
   * changés, therefore the original values of the values that can change must be provided as well for dirty checking.
   *
   * @param originalOnline    the original online state of the given updated server entity.
   * @param originalUtcOffset the original utc offset hours id of the given updated server entity.
   * @param originalScenery   the original scenery of the given updated server entity.
   * @param server            the updated server entity.
   */
  public void handleServerUpdate(
    boolean originalOnline,
    int originalUtcOffset,
    @Nullable String originalScenery,
    @Nonnull SimRailServerEntity server
  ) {
    // check if the given server entity is actually dirty
    var newOnline = server.isOnline();
    var newUtcOffset = server.getUtcOffsetHours();
    var newScenery = server.getScenery().name();
    var dirty = newOnline != originalOnline || originalUtcOffset != newUtcOffset || !newScenery.equals(originalScenery);
    if (dirty) {
      var frameBuilder = ServerUpdateFrame.newBuilder()
        .setUpdateType(UpdateType.UPDATE)
        .setServerId(server.getId().toString());

      // apply the new online state and new timezone id to the builder
      if (newOnline != originalOnline) {
        frameBuilder.setOnline(newOnline);
      }
      if (originalUtcOffset != newUtcOffset) {
        frameBuilder.setUtcOffsetHours(newUtcOffset);
        frameBuilder.setZoneOffset(server.getTimezone());
      }
      if (!newScenery.equals(originalScenery)) {
        frameBuilder.setServerScenery(newScenery);
      }

      this.internalRpcEventBusService.publishServerUpdate(frameBuilder.build());
    }
  }

  /**
   * Handles the add of a single server, sending an update frame to all registered listeners.
   *
   * @param server the server that was added.
   */
  public void handleServerAdd(@Nonnull SimRailServerEntity server) {
    var serverUpdateFrame = ServerUpdateFrame.newBuilder()
      .setUpdateType(UpdateType.ADD)
      .setServerId(server.getId().toString())
      .build();
    this.internalRpcEventBusService.publishServerUpdate(serverUpdateFrame);
  }

  /**
   * Handles the remove of a single server, sending an update frame to all registered listeners.
   *
   * @param server the server that was removed.
   */
  public void handleServerRemove(@Nonnull SimRailServerEntity server) {
    var serverUpdateFrame = ServerUpdateFrame.newBuilder()
      .setUpdateType(UpdateType.REMOVE)
      .setServerId(server.getId().toString())
      .build();
    this.internalRpcEventBusService.publishServerUpdate(serverUpdateFrame);
  }
}
