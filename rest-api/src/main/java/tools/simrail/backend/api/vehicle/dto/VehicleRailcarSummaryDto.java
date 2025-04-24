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

package tools.simrail.backend.api.vehicle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import tools.simrail.backend.common.railcar.RailcarType;

/**
 * Summary information about a railcar.
 */
public record VehicleRailcarSummaryDto(
  @Schema(description = "The unique identifier of the railcar")
  @NotNull UUID id,
  @Schema(description = "The display name of the railcar")
  @NotNull @NotBlank String name,
  @Schema(description = "The grouping type of the railcar")
  @NotNull RailcarType type,
  @Schema(description = "The weight of the railcar in tons")
  @NotNull @Min(0) double weight,
  @Schema(description = "The width of the railcar in meters")
  @NotNull @Min(0) double width,
  @Schema(description = "The length of the railcar in meters")
  @NotNull @Min(0) double length
) {

}
