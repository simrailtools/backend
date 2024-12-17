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

package tools.simrail.backend.api.vehicle.dto;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.simrail.backend.common.railcar.RailcarProvider;
import tools.simrail.backend.common.vehicle.JourneyVehicle;
import tools.simrail.backend.common.vehicle.JourneyVehicleStatus;

/**
 * Converter for vehicle information to DTO.
 */
@Component
public final class VehicleCompositionDtoConverter implements Function<List<JourneyVehicle>, VehicleCompositionDto> {

  private static final Comparator<VehicleDto> VEHICLE_INDEX_COMPARATOR =
    Comparator.comparingInt(VehicleDto::indexInGroup);

  private final RailcarProvider railcarProvider;

  @Autowired
  public VehicleCompositionDtoConverter(@Nonnull RailcarProvider railcarProvider) {
    this.railcarProvider = railcarProvider;
  }

  @Override
  public @Nullable VehicleCompositionDto apply(@Nonnull List<JourneyVehicle> vehicles) {
    // get the base information from the first vehicle in the group, return null if
    // the vehicle status is completely unknown and no data can be provided
    var firstVehicle = vehicles.getFirst();
    var firstVehicleStatus = firstVehicle.getStatus();
    if (firstVehicleStatus == JourneyVehicleStatus.UNKNOWN) {
      return null;
    }

    var mappedVehicles = vehicles.stream()
      .map(vehicle -> {
        var railcar = this.railcarProvider.findRailcarById(vehicle.getRailcarId()).orElseThrow();
        var railcarDto = new VehicleRailcarSummaryDto(
          railcar.getId(),
          railcar.getDisplayName(),
          railcar.getRailcarType(),
          railcar.getWeight(),
          railcar.getWidth(),
          railcar.getLength());
        return new VehicleDto(vehicle.getIndexInGroup(), vehicle.getLoadWeight(), vehicle.getLoad(), railcarDto);
      })
      .sorted(VEHICLE_INDEX_COMPARATOR)
      .toList();
    return new VehicleCompositionDto(
      firstVehicle.getJourneyId(),
      firstVehicleStatus,
      firstVehicle.getUpdateTime(),
      mappedVehicles);
  }
}
