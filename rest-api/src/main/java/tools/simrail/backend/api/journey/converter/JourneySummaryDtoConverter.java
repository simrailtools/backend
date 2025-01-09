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

package tools.simrail.backend.api.journey.converter;

import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.simrail.backend.api.journey.data.JourneyEventSummaryProjection;
import tools.simrail.backend.api.journey.data.JourneySummaryProjection;
import tools.simrail.backend.api.journey.dto.JourneySummaryDto;
import tools.simrail.backend.api.journey.dto.JourneyTerminalEventDto;

/**
 * Converter for journey summaries to a DTO.
 */
@Component
public final class JourneySummaryDtoConverter {

  private final JourneyStopPlaceSummaryDtoConverter stopPlaceDtoConverter;
  private final JourneyTransportSummaryDtoConverter transportDtoConverter;

  @Autowired
  public JourneySummaryDtoConverter(
    @Nonnull JourneyStopPlaceSummaryDtoConverter stopPlaceDtoConverter,
    @Nonnull JourneyTransportSummaryDtoConverter transportDtoConverter
  ) {
    this.stopPlaceDtoConverter = stopPlaceDtoConverter;
    this.transportDtoConverter = transportDtoConverter;
  }

  /**
   * Converts a journey and its associated first/last event into a DTO.
   */
  public @Nonnull JourneySummaryDto convert(
    @Nonnull JourneySummaryProjection journey,
    @Nonnull JourneyEventSummaryProjection firstEvent,
    @Nonnull JourneyEventSummaryProjection lastEvent
  ) {
    var originEvent = this.convertSummaryEvent(firstEvent);
    var destinationEvent = this.convertSummaryEvent(lastEvent);
    return new JourneySummaryDto(
      journey.getId(),
      journey.getServerId(),
      journey.getFirstSeenTime(),
      journey.getLastSeenTime(),
      journey.isCancelled(),
      originEvent,
      destinationEvent);
  }

  /**
   * Converts a single event summary projection into a terminal event DTO.
   */
  private @Nonnull JourneyTerminalEventDto convertSummaryEvent(@Nonnull JourneyEventSummaryProjection summary) {
    var transport = this.transportDtoConverter.apply(summary.getTransport());
    var stopPlace = this.stopPlaceDtoConverter.apply(summary.getStopDescriptor());
    return new JourneyTerminalEventDto(stopPlace, summary.getScheduledTime(), transport, summary.isCancelled());
  }
}
