/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024-2026 Pasqual Koschmieder and contributors
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

package tools.simrail.backend.api.shared;

import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import tools.simrail.backend.common.point.SimRailPointPosition;
import tools.simrail.backend.common.proto.EventBusProto;
import tools.simrail.backend.common.shared.GeoPositionEntity;

/**
 * Converter for a geo position to a DTO.
 */
@Component
public final class GeoPositionDtoConverter {

  /**
   * Converts a geo position from an event bus message.
   *
   * @param position the event bus position message to convert.
   * @return the converted event bus position message as a dto.
   */
  public @NonNull GeoPositionDto convert(EventBusProto.@NonNull GeoPosition position) {
    return new GeoPositionDto(position.getLatitude(), position.getLongitude());
  }

  /**
   * Converts a geo position of a point to a dto.
   *
   * @param position the point position to convert.
   * @return the converted point position as a dto.
   */
  public @NonNull GeoPositionDto convert(@NonNull SimRailPointPosition position) {
    return new GeoPositionDto(position.getLatitude(), position.getLongitude());
  }

  /**
   * Converts a geo position entity to a dto.
   *
   * @param position the position entity to convert.
   * @return the converted position entity as a dto.
   */
  public @NonNull GeoPositionDto convert(@NonNull GeoPositionEntity position) {
    return new GeoPositionDto(position.getLatitude(), position.getLongitude());
  }
}
