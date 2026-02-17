/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024-present Pasqual Koschmieder and contributors
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

package tools.simrail.backend.api.board.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import tools.simrail.backend.common.journey.JourneyStopType;
import tools.simrail.backend.common.journey.JourneyTimeType;

/**
 * DTO for a board entry.
 */
public record BoardEntryDto(
  @Schema(description = "The id of the journey arriving/departing at/from the requested point")
  @NotNull UUID journeyId,
  @Schema(description = "The id of the arrival/departure event at the requested point")
  @NotNull UUID eventId,
  @Schema(description = "Indicates if the event was cancelled, note that events can be additional and cancelled")
  @NotNull boolean cancelled,
  @Schema(description = "Indicates if the event is additional (not part of the scheduled route)")
  @NotNull boolean additional,
  @Schema(description = "Scheduled time of the event (ISO-8601) in local server time")
  @NotNull LocalDateTime scheduledTime,
  @Schema(description = "Best known time of the event (ISO-8601) in local server time, precision is described by the time type field")
  @NotNull LocalDateTime realtimeTime,
  @Schema(description = "The precision of the realtime time of the event")
  @NotNull JourneyTimeType realtimeTimeType,
  @Schema(description = "The scheduled stop type for the journey at the event")
  @NotNull JourneyStopType stopType,
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(description = "Information about the scheduled passenger stop, omitted if no passenger stop is scheduled")
  @Nullable BoardStopInfoDto scheduledPassengerStop,
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Schema(description = "Realtime information about the passenger stop, omitted if the event didn't happen / didn't stop at a platform")
  @Nullable BoardStopInfoDto realtimePassengerStop,
  @Schema(description = "Information about the transport at the requested point")
  @NotNull BoardTransportDto transport,
  @Schema(description = """
    The events this journey goes via. For arrivals these are the events before the requested point, for departures the
    events after the requested point. Events are always sorted by their index along the journey route. This means for
    arrivals the events are ordered in [initial -> current] while for departures they are sorted [current -> final].
    """)
  @NotNull List<BoardViaEventDto> via
) {

}
