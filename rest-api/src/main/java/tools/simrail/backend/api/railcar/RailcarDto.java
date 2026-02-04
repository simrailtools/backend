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

package tools.simrail.backend.api.railcar;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import tools.simrail.backend.common.railcar.RailcarType;

/**
 * DTO for information about a railcar.
 */
public record RailcarDto(
  @Schema(description = "The unique identifier of the railcar")
  @NotNull UUID id,
  @Schema(description = "The display name of the railcar")
  @NotNull @NotBlank String displayName,
  @Schema(description = "The baptismal name of the locomotive / wagon, null if none", types = "null")
  @Nullable String name,
  @Schema(description = "The grouping type of the railcar")
  @NotNull RailcarType type,
  @Schema(description = "The type identifier of the railcar")
  @NotNull @NotBlank String typeIdentifier,
  @Schema(description = "The id of the DLC that is required for the railcar, null if included in the base game", types = "null")
  @Nullable String requiredDlcId,
  @Schema(description = "The designation of the railcar")
  @NotNull @NotBlank String designation,
  @Schema(description = "The producer of the railcar")
  @NotNull @NotBlank String producer,
  @Schema(description = "The years in which the railcar was produced, can be range or single year")
  @NotNull @NotBlank String productionYears,
  @Schema(description = "The weight of the railcar in tons")
  @NotNull @Min(0) double weight,
  @Schema(description = "The width of the railcar in meters")
  @NotNull @Min(0) double width,
  @Schema(description = "The length of the railcar in meters")
  @NotNull @Min(0) double length,
  @Schema(description = "The maximum allowed speed of the railcar")
  @NotNull @Min(0) int maxSpeed
) {

}
