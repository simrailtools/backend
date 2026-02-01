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

import java.util.function.BiFunction;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.simrail.backend.api.journey.dto.JourneyDto;
import tools.simrail.backend.common.journey.JourneyEntity;
import tools.simrail.backend.common.proto.EventBusProto;

/**
 * Converter for journey entities to DTOs.
 */
@Component
public final class JourneyDtoConverter
  implements BiFunction<JourneyEntity, EventBusProto.JourneyUpdateFrame, JourneyDto> {

  private final JourneyLiveDataDtoConverter liveDataDtoConverter;
  private final JourneyEventDtoConverter journeyEventDtoConverter;

  @Autowired
  public JourneyDtoConverter(
    @NonNull JourneyLiveDataDtoConverter liveDataDtoConverter,
    @NonNull JourneyEventDtoConverter journeyEventDtoConverter
  ) {
    this.liveDataDtoConverter = liveDataDtoConverter;
    this.journeyEventDtoConverter = journeyEventDtoConverter;
  }

  @Override
  public @NonNull JourneyDto apply(
    @NonNull JourneyEntity journey,
    EventBusProto.@Nullable JourneyUpdateFrame liveData
  ) {
    var events = journey.getEvents().stream().map(this.journeyEventDtoConverter).toList();
    var liveDataDto = liveData != null ? this.liveDataDtoConverter.apply(liveData.getJourneyData()) : null;
    return new JourneyDto(
      journey.getId(),
      journey.getServerId(),
      journey.getUpdateTime(),
      journey.getFirstSeenTime(),
      journey.getLastSeenTime(),
      journey.isCancelled(),
      liveDataDto,
      events
    );
  }
}
