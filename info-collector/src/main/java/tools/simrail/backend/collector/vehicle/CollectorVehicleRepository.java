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
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tools.simrail.backend.common.vehicle.JourneyVehicleRepository;

/**
 * Repository with specialized information for the collector.
 */
interface CollectorVehicleRepository extends JourneyVehicleRepository {

  /**
   * Finds all journeys (mapping of [journey id, run id]) that don't have a stored vehicle composition yet.
   *
   * @param serverId the id of the server to select the journeys on.
   * @param runIds   the run ids to include in the result.
   * @return the journey ids and run ids of the journey without a stored vehicle composition.
   */
  @Query(value = """
    SELECT DISTINCT ON (j.id)
      j.id, j.foreign_run_id
    FROM sit_journey j
    LEFT JOIN sit_vehicle jv
      ON jv.journey_id = j.id AND jv.index_in_group = 0
    WHERE
      jv.journey_id IS NULL
      AND j.server_id = :serverId
      AND j.foreign_run_id IN :runIds
    """, nativeQuery = true)
  List<Object[]> findJourneyRunsWithoutVehicleComposition(
    @Param("serverId") UUID serverId,
    @Param("runIds") List<UUID> runIds);

  /**
   * Finds all journeys (mapping of [journey id, run id]) that don't have a stored, confirmed vehicle composition yet.
   *
   * @param serverId the id of the server to select the journeys on.
   * @param runIds   the run ids to include in the result.
   * @return the journey ids and run ids of the journey without a stored, confirmed vehicle composition.
   */
  @Query(value = """
    SELECT DISTINCT ON (j.id)
      j.id, j.foreign_run_id
    FROM sit_journey j
    LEFT JOIN sit_vehicle jv
      ON jv.journey_id = j.id AND jv.index_in_group = 0
    WHERE
      j.server_id = :serverId
      AND j.foreign_run_id IN :runIds
      AND (jv.journey_id IS NULL OR jv.status != 0)
    """, nativeQuery = true)
  List<Object[]> findJourneyRunsWithoutConfirmedVehicleComposition(
    @Param("serverId") UUID serverId,
    @Param("runIds") List<UUID> runIds);

  /**
   * Deletes all vehicle entries for the journey with the given id.
   *
   * @param journeyId the id of the journey to delete the vehicles of.
   */
  void deleteAllByJourneyId(@NonNull UUID journeyId);
}
