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

package tools.simrail.backend.api.journey.converter;

import jakarta.annotation.Nonnull;
import java.util.NoSuchElementException;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.simrail.backend.api.journey.dto.JourneyGeoPositionDto;
import tools.simrail.backend.api.journey.dto.JourneyStopPlaceWithPosDto;
import tools.simrail.backend.common.journey.JourneyStopDescriptor;
import tools.simrail.backend.common.point.SimRailPointProvider;

/**
 * Converter for stop descriptors to DTOs with position information.
 */
@Component
public final class JourneyStopPlaceWithPosDtoConverter
  implements Function<JourneyStopDescriptor, JourneyStopPlaceWithPosDto> {

  private final SimRailPointProvider pointProvider;

  @Autowired
  public JourneyStopPlaceWithPosDtoConverter(@Nonnull SimRailPointProvider pointProvider) {
    this.pointProvider = pointProvider;
  }

  @Override
  public @Nonnull JourneyStopPlaceWithPosDto apply(@Nonnull JourneyStopDescriptor stop) {
    var point = this.pointProvider
      .findPointByIntId(stop.getId())
      .orElseThrow(() -> new NoSuchElementException("missing point for stop " + stop.getId()));
    var pointPosition = point.getPosition();
    var stopPosition = new JourneyGeoPositionDto(pointPosition.getLatitude(), pointPosition.getLongitude());
    return new JourneyStopPlaceWithPosDto(
      stop.getId(),
      stop.getName(),
      stopPosition,
      stop.isPlayable());
  }
}