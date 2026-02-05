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

package tools.simrail.backend.api.vehicle.dto;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.simrail.backend.api.railcar.RailcarDtoConverter;
import tools.simrail.backend.common.railcar.RailcarProvider;
import tools.simrail.backend.common.vehicle.JourneyVehicleSequenceEntity;

/**
 * Converter for vehicle information to DTO.
 */
@Component
public final class VehicleSequenceDtoConverter implements Function<JourneyVehicleSequenceEntity, VehicleSequenceDto> {

  private static final Comparator<VehicleDto> VEHICLE_INDEX_COMPARATOR =
    Comparator.comparingInt(VehicleDto::indexInGroup);

  private final RailcarProvider railcarProvider;
  private final RailcarDtoConverter railcarDtoConverter;

  @Autowired
  public VehicleSequenceDtoConverter(
    @NonNull RailcarProvider railcarProvider,
    @NonNull RailcarDtoConverter railcarDtoConverter
  ) {
    this.railcarProvider = railcarProvider;
    this.railcarDtoConverter = railcarDtoConverter;
  }

  @Override
  public @NonNull VehicleSequenceDto apply(@NonNull JourneyVehicleSequenceEntity entity) {
    var mappedVehicles = entity.getVehicles().stream()
      .map(vehicle -> this.railcarProvider.findRailcarById(vehicle.getRailcarId())
        .map(railcar -> {
          var railcarDto = this.railcarDtoConverter.apply(railcar);
          return new VehicleDto(vehicle.getIndexInSequence(), vehicle.getLoadWeight(), vehicle.getLoad(), railcarDto);
        })
        .orElse(null))
      .filter(Objects::nonNull)
      .sorted(VEHICLE_INDEX_COMPARATOR)
      .toList();
    return new VehicleSequenceDto(entity.getJourneyId(), entity.getStatus(), entity.getUpdateTime(), mappedVehicles);
  }
}
