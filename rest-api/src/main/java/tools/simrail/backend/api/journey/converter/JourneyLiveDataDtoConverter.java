/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024-2026 Pasqual Koschmieder and contributors
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.simrail.backend.api.journey.dto.JourneyLiveDataDto;
import tools.simrail.backend.api.journey.dto.JourneySignalDto;
import tools.simrail.backend.api.shared.GeoPositionDtoConverter;
import tools.simrail.backend.api.shared.UserDtoConverter;
import tools.simrail.backend.common.proto.EventBusProto;

/**
 * Converter for journey live data into a DTO.
 */
@Component
public final class JourneyLiveDataDtoConverter implements Function<EventBusProto.JourneyData, JourneyLiveDataDto> {

  private final UserDtoConverter userDtoConverter;
  private final GeoPositionDtoConverter geoPositionDtoConverter;

  @Autowired
  public JourneyLiveDataDtoConverter(
    @NonNull UserDtoConverter userDtoConverter,
    @NonNull GeoPositionDtoConverter geoPositionDtoConverter
  ) {
    this.userDtoConverter = userDtoConverter;
    this.geoPositionDtoConverter = geoPositionDtoConverter;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull JourneyLiveDataDto apply(EventBusProto.@NonNull JourneyData data) {
    var speed = data.getSpeed();
    var position = this.geoPositionDtoConverter.convert(data.getPosition());
    var driver = data.hasDriver() ? this.userDtoConverter.apply(data.getDriver()) : null;
    var nextSignal = data.hasNextSignal() ? this.convertSignal(data.getNextSignal()) : null;
    return new JourneyLiveDataDto(speed, position, driver, nextSignal);
  }

  /**
   * Converts the given signal info message into a dto.
   *
   * @param data the signal message to convert.
   * @return the signal info dto based on the given signal message.
   */
  private @NonNull JourneySignalDto convertSignal(EventBusProto.@NonNull SignalInfo data) {
    var signalMaxSpeed = data.hasMaxSpeedKmh() ? (short) data.getMaxSpeedKmh() : null;
    return new JourneySignalDto(data.getName(), signalMaxSpeed, data.getDistanceMeters());
  }
}
