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

package tools.simrail.backend.collector.vehicle;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.simrail.backend.common.vehicle.JourneyVehicleEntity;

/**
 * Service for handling collection-relevant vehicle data for journeys.
 */
@Service
class CollectorJourneyVehicleService {

  private final CollectorVehicleRepository vehicleRepository;

  @Autowired
  public CollectorJourneyVehicleService(@NonNull CollectorVehicleRepository vehicleRepository) {
    this.vehicleRepository = vehicleRepository;
  }

  /**
   * Returns a mapping of run ids to journey ids without a stored vehicle composition.
   *
   * @param serverId the id of the server find the runs of.
   * @param runIds   the ids of the runs to search for missing vehicle compositions.
   * @return a mapping of run ids to journeys ids without a stored vehicle composition.
   */
  public @NonNull Map<UUID, UUID> findRunsWithoutComposition(@NonNull UUID serverId, @NonNull List<UUID> runIds) {
    var journeysWithoutComposition = this.vehicleRepository
      .findJourneyRunsWithoutVehicleComposition(serverId, runIds);
    return journeysWithoutComposition
      .stream()
      .collect(Collectors.toMap(entry -> (UUID) entry[1], entry -> (UUID) entry[0]));
  }

  /**
   * Returns a mapping of run ids to journey ids without a confirmed vehicle composition.
   *
   * @param serverId the id of the server find the runs of.
   * @param runIds   the ids of the runs to search for missing vehicle compositions.
   * @return a mapping of run ids to journeys ids without a confirmed vehicle composition.
   */
  public @NonNull Map<UUID, UUID> findRunsWithoutConfirmedComposition(
    @NonNull UUID serverId,
    @NonNull List<UUID> runIds
  ) {
    var journeysWithoutComposition = this.vehicleRepository
      .findJourneyRunsWithoutConfirmedVehicleComposition(serverId, runIds);
    return journeysWithoutComposition
      .stream()
      .collect(Collectors.toMap(entry -> (UUID) entry[1], entry -> (UUID) entry[0]));
  }

  /**
   * Saves the vehicles for the given journey, deleting the previously stored vehicle data.
   *
   * @param journeyId the id of the journey for which the vehicles should be stored.
   * @param vehicles  the new vehicle information for the journey.
   */
  @Transactional
  public void saveJourneyVehicles(@NonNull UUID journeyId, @NonNull List<JourneyVehicleEntity> vehicles) {
    this.vehicleRepository.deleteAllByJourneyId(journeyId);
    this.vehicleRepository.saveAll(vehicles);
  }
}
