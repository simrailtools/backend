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

package tools.simrail.backend.api.point.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * DTO for a point in SimRail.
 */
public record PointInfoDto(
  @Schema(description = "The id of the point")
  @Nonnull UUID id,
  @Schema(description = "The name of the point")
  @Nonnull String name,
  @Schema(description = "The ISO 3166-1 alpha-3 country code where the point is located")
  @Nonnull String country,
  @Schema(description = "The position where the point located")
  @Nonnull PointGeoPositionDto position,
  @Schema(description = "The UIC reference of the point, might not null in case it is unknown")
  @Nullable String uicRef,
  @Schema(description = "The OSM node id of the point")
  long osmNodeId,
  @Schema(description = "The maximum speed any journey can drive at the point", minimum = "0")
  int maxSpeed,
  @Schema(description = "The platforms at the point at which a passenger change can happen")
  @Nonnull List<PointPlatformInfoDto> platforms
) {

}
