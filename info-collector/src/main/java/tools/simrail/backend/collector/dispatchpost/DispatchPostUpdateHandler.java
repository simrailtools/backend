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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.simrail.backend.collector.rpc.InternalRpcEventBusService;
import tools.simrail.backend.common.dispatchpost.SimRailDispatchPostEntity;
import tools.simrail.backend.common.rpc.DispatchPostUpdateFrame;
import tools.simrail.backend.common.rpc.UpdateType;

/**
 * Handles updates of dispatch posts and sends update frames to the registered listeners.
 */
@Component
final class DispatchPostUpdateHandler {

  private final InternalRpcEventBusService internalRpcEventBusService;

  @Autowired
  public DispatchPostUpdateHandler(@Nonnull InternalRpcEventBusService internalRpcEventBusService) {
    this.internalRpcEventBusService = internalRpcEventBusService;
  }

  /**
   * Handles the update of a dispatch post, sending out an update frame for the post based on the dirty fields.
   *
   * @param post the post that was updated.
   */
  public void handleDispatchPostUpdate(@Nonnull SimRailDispatchPostEntity post) {
    var updateFrame = DispatchPostUpdateFrame.newBuilder()
      .setUpdateType(UpdateType.UPDATE)
      .setPostId(post.getId().toString())
      .addAllDispatcherSteamIds(post.getDispatcherSteamIds())
      .build();
    this.internalRpcEventBusService.publishDispatchPostUpdate(updateFrame);
  }

  /**
   * Handles the add of a new dispatch post, sending out an update frame to add the new post.
   *
   * @param post the post that was registered.
   */
  public void handleDispatchPostAdd(@Nonnull SimRailDispatchPostEntity post) {
    var updateFrame = DispatchPostUpdateFrame.newBuilder()
      .setUpdateType(UpdateType.ADD)
      .setPostId(post.getId().toString())
      .build();
    this.internalRpcEventBusService.publishDispatchPostUpdate(updateFrame);
  }

  /**
   * Handles the deletion of a dispatch post, sending out an update frame to remove the post.
   *
   * @param post the post that was removed.
   */
  public void handleDispatchPostRemove(@Nonnull SimRailDispatchPostEntity post) {
    var updateFrame = DispatchPostUpdateFrame.newBuilder()
      .setUpdateType(UpdateType.REMOVE)
      .setPostId(post.getId().toString())
      .build();
    this.internalRpcEventBusService.publishDispatchPostUpdate(updateFrame);
  }
}
