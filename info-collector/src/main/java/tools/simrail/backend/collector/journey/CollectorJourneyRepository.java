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

package tools.simrail.backend.collector.journey;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import tools.simrail.backend.common.journey.JourneyRepository;

/**
 * Extension of the default journey repository with collector-specific methods.
 */
interface CollectorJourneyRepository extends JourneyRepository {

  /**
   * Finds all journeys on the specified server whose first playable event was not reached before the given time.
   *
   * @param serverTime the time of the server to check against.
   * @param serverId   the id of the server to return the journeys of.
   */
  @Query(value = """
    WITH second_departures_in_border AS (
      SELECT
        e.journey_id
      FROM sit_journey_event e
      WHERE
        e.event_index = (
          SELECT e2.event_index
          FROM sit_journey_event e2
          WHERE e2.journey_id = e.journey_id
            AND e2.event_type = 1 -- departure event
            AND e2.point_playable = TRUE
          ORDER BY event_index
          OFFSET 1
          LIMIT 1
        )
        AND e.cancelled = false
        AND e.scheduled_time < :time
    )
    SELECT j.id
    FROM sit_journey j
    JOIN second_departures_in_border ib ON ib.journey_id = j.id
    WHERE j.server_id = :serverId AND j.first_seen_time IS NULL
    """, nativeQuery = true)
  List<UUID> findJourneysThatDidNotSpawn(@Param("time") OffsetDateTime serverTime, @Param("serverId") UUID serverId);

  @Modifying
  @Query(value = "DELETE FROM sit_journey j WHERE j.foreign_run_id IN :runIds AND j.first_seen_time IS NULL", nativeQuery = true)
  void deleteUnstartedJourneysByRunIds(@Param("runIds") Collection<UUID> runIds);

  /**
   *
   * @param foreignRunIds
   * @return
   */
  @NonNull
  @Query(value = "SELECT DISTINCT j.foreign_run_id FROM sit_journey j WHERE j.foreign_run_id IN :runIds", nativeQuery = true)
  Set<UUID> findAllRunIdsWhereRunIdIn(@Param("runIds") Collection<UUID> foreignRunIds);

  /**
   * Marks the journeys with the given journey ids as canceled.
   *
   * @param currentTime the current time to set as the last updated time of the journey.
   * @param journeyIds  the ids of the journeys to mark as canceled.
   */
  @Modifying
  @Transactional
  @Query("UPDATE sit_journey j SET j.cancelled = TRUE, j.updateTime = :time WHERE j.id IN :journeyIds")
  void markJourneysAsCancelled(
    @Param("time") OffsetDateTime currentTime,
    @Param("journeyIds") Collection<UUID> journeyIds);

  /**
   * Marks the journey events associated with one of the given journeys as canceled.
   *
   * @param journeyIds the ids of the journeys whose events should be marked as canceled.
   */
  @Modifying
  @Transactional
  @Query("UPDATE sit_journey_event je SET je.cancelled = TRUE WHERE je.journey.id IN :journeyIds")
  void markJourneyEventsAsCancelled(@Param("journeyIds") Collection<UUID> journeyIds);
}
