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
import java.util.function.Function;
import org.springframework.stereotype.Component;
import tools.simrail.backend.api.eventbus.dto.EventbusJourneySnapshotDto;
import tools.simrail.backend.api.journey.dto.JourneyActiveDto;
import tools.simrail.backend.api.journey.dto.JourneyActiveTransportDto;
import tools.simrail.backend.api.journey.dto.JourneyGeoPositionDto;

/**
 * Converter for locally cached journey snapshots to DTOs.
 */
@Component
public final class JourneyActiveDtoConverter implements Function<EventbusJourneySnapshotDto, JourneyActiveDto> {

  @Override
  public @Nonnull JourneyActiveDto apply(@Nonnull EventbusJourneySnapshotDto snapshot) {
    var position = new JourneyGeoPositionDto(snapshot.getPositionLat(), snapshot.getPositionLng());
    var transport = new JourneyActiveTransportDto(
      snapshot.getCategory(),
      snapshot.getNumber(),
      snapshot.getLine(),
      snapshot.getLabel());
    return new JourneyActiveDto(
      snapshot.getJourneyId(),
      snapshot.getServerId(),
      transport,
      snapshot.getDriverSteamId(),
      snapshot.getSpeed(),
      position);
  }
}
