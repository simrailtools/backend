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

package tools.simrail.backend.api.journey.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.UUID;

/**
 * DTO for a journey that is currently active on a server.
 */
public record JourneyActiveDto(
  @Schema(description = "The identifier of the journey")
  @Nonnull UUID journeyId,
  @Schema(description = "The identifier of the server where the journey is active")
  @Nonnull UUID serverId,
  @Schema(description = "The category of the journey")
  @Nonnull String category,
  @Schema(description = "The number of the journey")
  @Nonnull String number,
  @Schema(description = "The line of the journey")
  @Nullable String line,
  @Schema(description = "The label of the journey")
  @Nullable String label,
  @Schema(description = "The steam identifier of the player that is currently driving the train")
  @Nullable String driverSteamId,
  @Schema(description = "The latitude where the journey is currently located")
  double positionLatitude,
  @Schema(description = "The longitude where the journey is currently located")
  double positionLongitude,
  @Schema(description = "The current speed of the journey")
  int speed
) {

}
