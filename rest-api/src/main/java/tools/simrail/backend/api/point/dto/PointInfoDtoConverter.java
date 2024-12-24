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

package tools.simrail.backend.api.point.dto;

import jakarta.annotation.Nonnull;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.BiFunction;
import org.springframework.stereotype.Component;
import tools.simrail.backend.common.point.SimRailPoint;
import tools.simrail.backend.common.signal.PlatformSignal;

/**
 * Converts a single SimRail point to the DTO.
 */
@Component
public final class PointInfoDtoConverter implements BiFunction<SimRailPoint, Collection<PlatformSignal>, PointInfoDto> {

  private static final Comparator<PointPlatformInfoDto> PLATFORM_COMPARATOR =
    Comparator.comparingInt(PointPlatformInfoDto::track);

  @Override
  public @Nonnull PointInfoDto apply(@Nonnull SimRailPoint point, @Nonnull Collection<PlatformSignal> platformSignals) {
    // convert and sort the platforms at the point
    var convertedPlatforms = platformSignals.stream()
      .map(signal -> new PointPlatformInfoDto(signal.getTrack(), signal.getPlatform()))
      .distinct()
      .sorted(PLATFORM_COMPARATOR)
      .toList();

    // convert the point position
    var pointPosition = point.getPosition();
    var convertedPosition = new PointGeoPositionDto(pointPosition.getLatitude(), pointPosition.getLongitude());

    // construct the final point dto
    return new PointInfoDto(
      point.getId(),
      point.getName(),
      point.getCountry(),
      convertedPosition,
      point.getUicRef(),
      point.getOsmNodeId(),
      point.getMaxSpeed(),
      convertedPlatforms);
  }
}
