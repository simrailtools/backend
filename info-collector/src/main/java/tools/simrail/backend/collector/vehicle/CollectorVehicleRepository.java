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

package tools.simrail.backend.collector.vehicle;

import jakarta.annotation.Nonnull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tools.simrail.backend.common.vehicle.JourneyVehicle;
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
  @Query("""
    SELECT j.id, j.foreignRunId
    FROM sit_journey j
    LEFT JOIN sit_vehicle jv
      ON jv.journeyId = j.id AND jv.indexInGroup = 0
    WHERE
      jv.journeyId IS NULL
      AND j.serverId = :serverId
      AND j.foreignRunId IN :runIds
    """)
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
  @Query("""
    SELECT j.id, j.foreignRunId
    FROM sit_journey j
    LEFT JOIN sit_vehicle jv
      ON jv.journeyId = j.id AND jv.indexInGroup = 0
    WHERE
      j.serverId = :serverId
      AND j.foreignRunId IN :runIds
      AND (jv.journeyId IS NULL OR jv.status != tools.simrail.backend.common.vehicle.JourneyVehicleStatus.REAL)
    """)
  List<Object[]> findJourneyRunsWithoutConfirmedVehicleComposition(
    @Param("serverId") UUID serverId,
    @Param("runIds") List<UUID> runIds);

  /**
   * Finds the journey vehicles of a journey on the given server with the given category, number and on the specified
   * date.
   *
   * @param serverId      the id of the server to select the journey on.
   * @param trainCategory the category of the journey to find.
   * @param trainNumber   the number of the journey to find.
   * @param date          the date on which the journey to find.
   * @return the vehicles of the requested journey.
   */
  @Query(value = """
    SELECT jv.*
    FROM sit_vehicle jv
    JOIN sit_journey j ON jv.journey_id = j.id
    JOIN sit_journey_event je ON je.journey_id = j.id AND je.event_index = 0
    WHERE
      j.server_id = :serverId
      AND je.transport_category = :category
      AND je.transport_number= :number
      AND (je.scheduled_time >= CAST(:date AS TIMESTAMP) AND
        je.scheduled_time < CAST(:date AS TIMESTAMP) + INTERVAL '1 day')
    ORDER BY jv.index_in_group
    """, nativeQuery = true)
  List<JourneyVehicle> findJourneyVehiclesByJourneyOnSpecificDate(
    @Param("serverId") UUID serverId,
    @Param("category") String trainCategory,
    @Param("number") String trainNumber,
    @Param("date") LocalDate date);

  /**
   * Deletes all vehicle entries for the journey with the given id.
   *
   * @param journeyId the id of the journey to delete the vehicles of.
   */
  void deleteAllByJourneyId(@Nonnull UUID journeyId);
}
