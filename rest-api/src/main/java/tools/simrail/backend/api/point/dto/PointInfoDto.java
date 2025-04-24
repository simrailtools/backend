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

package tools.simrail.backend.api.point.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * DTO for a point in SimRail.
 */
public record PointInfoDto(
  @Schema(description = "The id of the point")
  @NotNull UUID id,
  @Schema(description = "The name of the point")
  @NotNull @NotBlank String name,
  @Schema(description = "The ISO 3166-1 alpha-3 country code where the point is located")
  @NotNull @Size(min = 3, max = 3) String country,
  @Schema(description = "The position where the point located")
  @NotNull PointGeoPositionDto position,
  @Schema(description = "The UIC reference of the point, might not null in case it is unknown", nullable = true)
  @Nullable String uicRef,
  @Schema(description = "The OSM node id of the point")
  @NotNull long osmNodeId,
  @Schema(description = "The maximum speed any journey can drive at the point", minimum = "0")
  @NotNull @Min(0) int maxSpeed,
  @Schema(description = "Indicates if the point is a stop place (without switches) or a full station")
  @NotNull boolean stopPlace,
  @Schema(description = "The platforms at the point at which a passenger change can happen")
  @NotNull List<PointPlatformInfoDto> platforms
) {

}
