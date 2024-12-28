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
import tools.simrail.backend.common.rpc.ServerUpdateFrame;
import tools.simrail.backend.common.server.SimRailServerEntity;
import tools.simrail.backend.common.server.SimRailServerRegion;

/**
 * DTO with changing field for server updates.
 */
@Data
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class EventbusServerSnapshotDto {

  // static fields
  private final UUID serverId;
  private final String code;
  private final List<String> tags;
  private final String spokenLanguage;
  private final SimRailServerRegion region;

  // changing fields
  private boolean online;
  private String timezoneId;

  public EventbusServerSnapshotDto(@Nonnull SimRailServerEntity entity) {
    this.serverId = entity.getId();
    this.code = entity.getCode();
    this.tags = entity.getTags();
    this.spokenLanguage = entity.getSpokenLanguage();
    this.region = entity.getRegion();
    this.online = entity.isOnline();
    this.timezoneId = entity.getTimezone();
  }

  /**
   * Applies the given server update frame to this snapshot.
   *
   * @param frame the update frame of the server to apply.
   */
  public synchronized void applyUpdateFrame(@Nonnull ServerUpdateFrame frame) {
    if (frame.hasOnline()) {
      this.online = frame.getOnline();
    }

    if (frame.hasZoneOffset()) {
      this.timezoneId = frame.getZoneOffset();
    }
  }
}
