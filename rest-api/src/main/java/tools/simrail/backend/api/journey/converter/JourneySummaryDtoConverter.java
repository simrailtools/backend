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

package tools.simrail.backend.api.journey.converter;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.simrail.backend.api.journey.data.JourneyEventSummaryProjection;
import tools.simrail.backend.api.journey.data.JourneySummaryProjection;
import tools.simrail.backend.api.journey.data.JourneyWithEventSummaryProjection;
import tools.simrail.backend.api.journey.dto.JourneyEventDescriptorDto;
import tools.simrail.backend.api.journey.dto.JourneyStopPlaceSummaryDto;
import tools.simrail.backend.api.journey.dto.JourneySummaryDto;
import tools.simrail.backend.api.journey.dto.JourneySummaryWithEventDto;
import tools.simrail.backend.api.journey.dto.JourneySummaryWithLiveDataDto;
import tools.simrail.backend.api.journey.dto.JourneyTerminalEventDto;
import tools.simrail.backend.api.journey.dto.JourneyTransportSummaryDto;
import tools.simrail.backend.common.point.SimRailPointProvider;
import tools.simrail.backend.common.proto.EventBusProto;

/**
 * Converter for journey summaries to a DTO.
 */
@Component
public final class JourneySummaryDtoConverter {

  private final SimRailPointProvider pointProvider;
  private final JourneyLiveDataDtoConverter liveDataDtoConverter;

  @Autowired
  public JourneySummaryDtoConverter(
    @NonNull SimRailPointProvider pointProvider,
    @NonNull JourneyLiveDataDtoConverter liveDataDtoConverter
  ) {
    this.pointProvider = pointProvider;
    this.liveDataDtoConverter = liveDataDtoConverter;
  }

  /**
   * Converts a journey and its associated first/last event into a DTO.
   */
  public @NonNull JourneySummaryDto convert(
    @NonNull JourneySummaryProjection journey,
    @NonNull JourneyEventSummaryProjection firstEvent,
    @NonNull JourneyEventSummaryProjection lastEvent
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
   * Converts a journey and its associated first/last event into a DTO.
   */
  public @NonNull JourneySummaryWithEventDto convert(
    @NonNull JourneyWithEventSummaryProjection journey,
    @NonNull JourneyEventSummaryProjection firstEvent,
    @NonNull JourneyEventSummaryProjection lastEvent
  ) {
    var originEvent = this.convertSummaryEvent(firstEvent);
    var destinationEvent = this.convertSummaryEvent(lastEvent);
    var firstPlayableEvent = this.convertFirstEvent(journey);
    return new JourneySummaryWithEventDto(
      journey.getId(),
      journey.getServerId(),
      journey.getFirstSeenTime(),
      journey.getLastSeenTime(),
      journey.isEventCancelled(),
      originEvent,
      destinationEvent,
      firstPlayableEvent);
  }

  /**
   * Converts a journey, its associated first/last event and the live data into a DTO.
   */
  public @NonNull JourneySummaryWithLiveDataDto convert(
    @NonNull JourneySummaryProjection journey,
    @NonNull JourneyEventSummaryProjection firstEvent,
    @NonNull JourneyEventSummaryProjection lastEvent,
    EventBusProto.@NonNull JourneyData liveData
  ) {
    var originEvent = this.convertSummaryEvent(firstEvent);
    var destinationEvent = this.convertSummaryEvent(lastEvent);
    var liveDataDto = this.liveDataDtoConverter.apply(liveData);
    return new JourneySummaryWithLiveDataDto(
      journey.getId(),
      journey.getServerId(),
      journey.getFirstSeenTime(),
      journey.getLastSeenTime(),
      journey.isCancelled(),
      originEvent,
      destinationEvent,
      liveDataDto);
  }

  /**
   * Converts a single event summary projection into a terminal event DTO.
   */
  private @NonNull JourneyTerminalEventDto convertSummaryEvent(@NonNull JourneyEventSummaryProjection summary) {
    var transport = this.convertTransportFromEventSummary(summary);
    var stopPlace = this.convertStopPlaceFromEventSummary(summary);
    return new JourneyTerminalEventDto(stopPlace, summary.getScheduledTime(), transport, summary.isCancelled());
  }

  /**
   * Converts the first playable event that is encapsulated in the given journey projection.
   */
  private @NonNull JourneyEventDescriptorDto convertFirstEvent(@NonNull JourneyWithEventSummaryProjection summary) {
    var point = this.pointProvider.findPointByIntId(summary.getEventPointId()).orElseThrow(); // must be present
    var stopPlace = new JourneyStopPlaceSummaryDto(
      summary.getEventPointId(),
      point.getName(),
      summary.isEventPointPlayable());
    return new JourneyEventDescriptorDto(stopPlace, summary.getEventScheduledTime(), summary.isEventCancelled());
  }

  /**
   * Converts the stop place info from the given event summary into a stop place summary dto.
   */
  private @NonNull JourneyStopPlaceSummaryDto convertStopPlaceFromEventSummary(
    @NonNull JourneyEventSummaryProjection summary
  ) {
    var point = this.pointProvider.findPointByIntId(summary.getPointId()).orElseThrow(); // must be present
    return new JourneyStopPlaceSummaryDto(point.getId(), point.getName(), summary.isPointPlayable());
  }

  /**
   * Converts an event summary into a transport dto.
   */
  private @NonNull JourneyTransportSummaryDto convertTransportFromEventSummary(
    @NonNull JourneyEventSummaryProjection summary
  ) {
    return new JourneyTransportSummaryDto(
      summary.getTransportCategory(),
      summary.getTransportNumber(),
      summary.getTransportLine(),
      summary.getTransportLabel(),
      summary.getTransportType());
  }
}
