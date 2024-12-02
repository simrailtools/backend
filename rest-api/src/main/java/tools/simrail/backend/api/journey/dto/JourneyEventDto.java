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
import java.util.UUID;
import tools.simrail.backend.common.journey.JourneyEventType;
import tools.simrail.backend.common.journey.JourneyStopType;
import tools.simrail.backend.common.journey.JourneyTimeType;

/**
 * DTO for a single journey event.
 */
public record JourneyEventDto(
  @Schema(description = "The id of the event")
  @Nonnull UUID id,
  @Schema(description = "The type of the event")
  @Nonnull JourneyEventType type,
  @Schema(description = "Indicates if the event was cancelled, note that events can be additional and cancelled")
  boolean cancelled,
  @Schema(description = "Indicates if the event is additional (not part of the scheduled route)")
  boolean additional,
  @Schema(description = "Information about the stop place where the event takes place")
  @Nonnull JourneyStopPlaceWithPosDto stopPlace,
  @Schema(description = "Scheduled time of the event (ISO-8601 with offset)")
  @Nonnull OffsetDateTime scheduledTime,
  @Schema(description = "Best known time of the event (ISO-8601 with offset), precision is described by the time type field")
  @Nonnull OffsetDateTime realtimeTime,
  @Schema(description = "The precision of the realtime time of the event")
  @Nonnull JourneyTimeType realtimeTimeType,
  @Schema(description = "The scheduled stop type for the journey at the event")
  @Nonnull JourneyStopType stopType,
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(description = "Information about the scheduled passenger stop, omitted if no passenger stop is scheduled")
  @Nullable JourneyStopInfoDto scheduledPassengerStop,
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(description = "Realtime information about the passenger stop, omitted if the event didn't happen / didn't stop at a platform")
  @Nullable JourneyStopInfoDto realtimePassengerStop,
  @Schema(description = "Information about the transport at the event")
  @Nonnull JourneyTransportDto transport
) {

}
