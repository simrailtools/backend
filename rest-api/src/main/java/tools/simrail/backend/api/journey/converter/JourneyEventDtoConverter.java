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
import jakarta.annotation.Nullable;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.simrail.backend.api.journey.dto.JourneyEventDto;
import tools.simrail.backend.api.journey.dto.JourneyStopInfoDto;
import tools.simrail.backend.common.journey.JourneyEventEntity;
import tools.simrail.backend.common.journey.JourneyPassengerStopInfo;

/**
 * Converter for journey event entities to DTOs.
 */
@Component
public final class JourneyEventDtoConverter implements Function<JourneyEventEntity, JourneyEventDto> {

  private final JourneyStopInfoDtoConverter stopInfoDtoConverter;
  private final JourneyTransportDtoConverter transportDtoConverter;
  private final JourneyStopPlaceDtoConverter stopPlaceDtoConverter;

  @Autowired
  public JourneyEventDtoConverter(
    @Nonnull JourneyStopInfoDtoConverter stopInfoDtoConverter,
    @Nonnull JourneyTransportDtoConverter transportDtoConverter,
    @Nonnull JourneyStopPlaceDtoConverter stopPlaceDtoConverter
  ) {
    this.stopInfoDtoConverter = stopInfoDtoConverter;
    this.transportDtoConverter = transportDtoConverter;
    this.stopPlaceDtoConverter = stopPlaceDtoConverter;
  }

  @Override
  public @Nonnull JourneyEventDto apply(@Nonnull JourneyEventEntity event) {
    var stopPlace = this.stopPlaceDtoConverter.apply(event.getStopDescriptor());
    var scheduledStopInfo = this.convertStopInfo(event.getScheduledPassengerStopInfo());
    var realtimeStopInfo = this.convertStopInfo(event.getActualPassengerStopInfo());
    var transport = this.transportDtoConverter.apply(event.getTransport());
    return new JourneyEventDto(
      event.getId(),
      event.getEventType(),
      event.isCancelled(),
      event.isAdditional(),
      stopPlace,
      event.getScheduledTime(),
      event.getActualTime(),
      event.getRealtimeTimeType(),
      event.getStopType(),
      scheduledStopInfo,
      realtimeStopInfo,
      transport
    );
  }

  private @Nullable JourneyStopInfoDto convertStopInfo(@Nullable JourneyPassengerStopInfo stopInfo) {
    return stopInfo == null ? null : this.stopInfoDtoConverter.apply(stopInfo);
  }
}
