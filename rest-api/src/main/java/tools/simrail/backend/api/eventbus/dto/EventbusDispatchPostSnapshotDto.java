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

package tools.simrail.backend.api.eventbus.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import tools.simrail.backend.common.dispatchpost.SimRailDispatchPostEntity;
import tools.simrail.backend.common.rpc.DispatchPostUpdateFrame;

/**
 * DTO with changing field for dispatch post updates.
 */
@Data
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class EventbusDispatchPostSnapshotDto {

  // static fields
  private final UUID postId;
  private final UUID pointId;
  private final UUID serverId;

  private final String name;
  private final int difficultyLevel;

  private double latitude;
  private double longitude;

  // changing fields
  private List<String> dispatcherSteamIds;

  public EventbusDispatchPostSnapshotDto(@Nonnull SimRailDispatchPostEntity entity) {
    this.postId = entity.getId();
    this.pointId = entity.getPointId();
    this.serverId = entity.getServerId();
    this.name = entity.getName();
    this.difficultyLevel = entity.getDifficultyLevel();
    this.dispatcherSteamIds = List.copyOf(entity.getDispatcherSteamIds());

    var position = entity.getPosition();
    this.latitude = position.getLatitude();
    this.longitude = position.getLongitude();
  }

  /**
   * Applies the given dispatch post update frame to this snapshot.
   *
   * @param frame the update frame of the dispatch post to apply.
   */
  public synchronized void applyUpdateFrame(@Nonnull DispatchPostUpdateFrame frame) {
    this.dispatcherSteamIds = frame.getDispatcherSteamIdsList();
  }
}
