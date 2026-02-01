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

import java.util.function.Function;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.simrail.backend.api.journey.dto.JourneyEventDto;
import tools.simrail.backend.api.journey.dto.JourneyStopInfoDto;
import tools.simrail.backend.api.journey.dto.JourneyStopPlaceDto;
import tools.simrail.backend.api.shared.GeoPositionDtoConverter;
import tools.simrail.backend.common.journey.JourneyEventEntity;
import tools.simrail.backend.common.journey.JourneyPassengerStopInfo;
import tools.simrail.backend.common.point.SimRailPointProvider;

/**
 * Converter for journey event entities to DTOs.
 */
@Component
public final class JourneyEventDtoConverter implements Function<JourneyEventEntity, JourneyEventDto> {

  private final SimRailPointProvider pointProvider;
  private final GeoPositionDtoConverter geoPositionDtoConverter;
  private final JourneyStopInfoDtoConverter stopInfoDtoConverter;
  private final JourneyTransportDtoConverter transportDtoConverter;

  @Autowired
  public JourneyEventDtoConverter(
    @NonNull SimRailPointProvider pointProvider,
    @NonNull GeoPositionDtoConverter geoPositionDtoConverter,
    @NonNull JourneyStopInfoDtoConverter stopInfoDtoConverter,
    @NonNull JourneyTransportDtoConverter transportDtoConverter
  ) {
    this.pointProvider = pointProvider;
    this.geoPositionDtoConverter = geoPositionDtoConverter;
    this.stopInfoDtoConverter = stopInfoDtoConverter;
    this.transportDtoConverter = transportDtoConverter;
  }

  @Override
  public @NonNull JourneyEventDto apply(@NonNull JourneyEventEntity event) {
    var point = this.pointProvider.findPointByIntId(event.getPointId()).orElseThrow(); // must be present
    var pointPosition = this.geoPositionDtoConverter.convert(point.getPosition());
    var stopPlace = new JourneyStopPlaceDto(
      point.getId(),
      point.getName(),
      pointPosition,
      point.isStopPlace(),
      event.isInPlayableBorder());

    var scheduledStopInfo = this.convertStopInfo(event.getScheduledPassengerStopInfo());
    var realtimeStopInfo = this.convertStopInfo(event.getRealtimePassengerStopInfo());
    var transport = this.transportDtoConverter.apply(event.getTransport());
    return new JourneyEventDto(
      event.getId(),
      event.getEventType(),
      event.isCancelled(),
      event.isAdditional(),
      stopPlace,
      event.getScheduledTime(),
      event.getRealtimeTime(),
      event.getRealtimeTimeType(),
      event.getStopType(),
      scheduledStopInfo,
      realtimeStopInfo,
      transport
    );
  }

  /**
   * Converts the given stop info into a dto.
   */
  private @Nullable JourneyStopInfoDto convertStopInfo(@Nullable JourneyPassengerStopInfo stopInfo) {
    return stopInfo == null ? null : this.stopInfoDtoConverter.apply(stopInfo);
  }
}
