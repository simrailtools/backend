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

package tools.simrail.backend.api.vehicle;

import jakarta.annotation.Nonnull;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import tools.simrail.backend.api.vehicle.dto.VehicleCompositionDto;
import tools.simrail.backend.api.vehicle.dto.VehicleCompositionDtoConverter;

@Service
class VehicleService {

  private final ApiVehicleRepository vehicleRepository;
  private final VehicleCompositionDtoConverter vehicleCompositionConverter;

  @Autowired
  public VehicleService(
    @Nonnull ApiVehicleRepository vehicleRepository,
    @Nonnull VehicleCompositionDtoConverter vehicleCompositionConverter
  ) {
    this.vehicleRepository = vehicleRepository;
    this.vehicleCompositionConverter = vehicleCompositionConverter;
  }

  /**
   * Gets the vehicle sequence for a journey, either from database or from cache.
   *
   * @param journeyId the id of the journey to get the vehicle sequence of.
   * @return the vehicle composition of the journey with the given id, if one exists.
   */
  @Cacheable(cacheNames = "vehicle_sequence_cache", key = "'by_jid_' + #journeyId")
  public @Nonnull Optional<VehicleCompositionDto> findByJourneyId(@Nonnull UUID journeyId) {
    var fetchedVehicles = this.vehicleRepository.findJourneyVehiclesByJourneyIdOrderByIndexInGroup(journeyId);
    return Optional.of(fetchedVehicles)
      .filter(vehicles -> !vehicles.isEmpty())
      .map(this.vehicleCompositionConverter);
  }
}
