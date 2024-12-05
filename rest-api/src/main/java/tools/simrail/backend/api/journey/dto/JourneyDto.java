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

package tools.simrail.backend.api.journey.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for information about a journey.
 */
public record JourneyDto(
  @Schema(description = "The identifier of the journey")
  @Nonnull UUID journeyId,
  @Schema(description = "The identifier of the server where the journey takes place")
  @Nonnull UUID serverId,
  @Schema(description = "The time (ISO-8601 with offset) when the journey data was last updated")
  @Nonnull OffsetDateTime lastUpdated,
  @Schema(description = "The time (ISO-8601 with offset) when the journey was first seen, null if the journey wasn't active yet")
  @Nullable OffsetDateTime firstSeenTime,
  @Schema(description = "The time (ISO-8601 with offset) when the journey was last seen, null if the journey is still active or wasn't active")
  @Nullable OffsetDateTime lastSeenTime,
  @Schema(description = "Indicates if the journey was cancelled")
  boolean journeyCancelled,
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(description = "Current live information about the journey, omitted from the response if the journey is not active")
  @Nullable JourneyLiveDataDto liveData,
  @Schema(description = "The events along the route of the journey")
  @Nonnull List<JourneyEventDto> events
) {

}