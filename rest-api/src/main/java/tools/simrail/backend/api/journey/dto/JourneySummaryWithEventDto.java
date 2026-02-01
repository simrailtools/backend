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
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO for summary information about a journey.
 */
public record JourneySummaryWithEventDto(
  @Schema(description = "The identifier of the journey")
  @NotNull UUID journeyId,
  @Schema(description = "The identifier of the server where the journey takes place")
  @NotNull UUID serverId,
  @Schema(description = "The instant (ISO-8601) when the journey was first seen, null if the journey wasn't active yet", types = {"null"})
  @Nullable Instant firstSeenTime,
  @Schema(description = "The instant (ISO-8601) when the journey was last seen, null if the journey is still or wasn't active", types = {"null"})
  @Nullable Instant lastSeenTime,
  @Schema(description = "Indicates if the journey was cancelled")
  @NotNull boolean journeyCancelled,
  @Schema(description = "The origin (first) event of the journey")
  @NotNull JourneyTerminalEventDto originEvent,
  @Schema(description = "The destination (last) event of the journey")
  @NotNull JourneyTerminalEventDto destinationEvent,
  @Schema(description = "An event of the journey. The exact nature of this event depends on the context it's returned in")
  @NotNull JourneyEventDescriptorDto event
) {

}
