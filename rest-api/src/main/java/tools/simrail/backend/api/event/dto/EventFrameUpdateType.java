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

package tools.simrail.backend.api.event.dto;

import jakarta.annotation.Nonnull;
import tools.simrail.backend.common.rpc.UpdateType;

/**
 * The different types of updates that can be delivered in an update frame.
 */
public enum EventFrameUpdateType {

  ADD,
  REMOVE,
  UPDATE;

  /**
   * Maps the internal rpc update type to this update type enum.
   *
   * @param updateType the internal rpc update type variant of this enum.
   * @return the remapped update type of the internal rpc update type.
   */
  public static @Nonnull EventFrameUpdateType fromInternalType(@Nonnull UpdateType updateType) {
    return switch (updateType) {
      case ADD -> EventFrameUpdateType.ADD;
      case REMOVE -> EventFrameUpdateType.REMOVE;
      case UPDATE -> EventFrameUpdateType.UPDATE;
      default -> throw new IllegalStateException("Unexpected frame update type " + updateType);
    };
  }
}
