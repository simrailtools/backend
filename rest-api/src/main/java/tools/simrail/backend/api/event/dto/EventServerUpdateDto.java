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

package tools.simrail.backend.api.event.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import tools.simrail.backend.common.rpc.ServerUpdateFrame;

/**
 * DTO for updating a server, only contains the fields that can be updated.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EventServerUpdateDto(
  @Nonnull String serverId,
  @Nullable String timezoneId,
  @Nullable Boolean online,
  @Nullable String scenery
) {

  /**
   * Constructs an update DTO instance from the given update frame.
   *
   * @param frame the update frame to construct the update frame from.
   * @return the constructed update dto from the given update frame.
   */
  public static @Nonnull EventServerUpdateDto fromServerUpdateFrame(@Nonnull ServerUpdateFrame frame) {
    var online = frame.hasOnline() ? frame.getOnline() : null;
    var timezoneId = frame.hasZoneOffset() ? frame.getZoneOffset() : null;
    var scenery = frame.hasServerScenery() ? frame.getServerScenery() : null;
    return new EventServerUpdateDto(frame.getServerId(), timezoneId, online, scenery);
  }
}
