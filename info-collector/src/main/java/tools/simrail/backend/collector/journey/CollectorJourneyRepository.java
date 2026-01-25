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

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tools.simrail.backend.common.journey.JourneyRepository;

/**
 * Extension of the default journey repository with collector-specific methods.
 */
interface CollectorJourneyRepository extends JourneyRepository {

  /**
   * Finds all journeys on the specified server whose first playable event was not reached before the given time.
   *
   * @param serverId   the id of the server to return the journeys of.
   * @param cutoffTime the deadline to find journeys that didn't spawn before.
   */
  @NonNull
  @Query(value = """
    WITH first_playable AS (
      SELECT
        e.journey_id,
        e.id AS event_id,
        e.event_index,
        e.scheduled_time,
        e.cancelled,
        e.realtime_time_type
      FROM (
        SELECT e.*, ROW_NUMBER() OVER (PARTITION BY e.journey_id ORDER BY e.event_index) AS rn
        FROM sit_journey_event e
        WHERE e.in_playable_border = TRUE
      ) e
      WHERE e.rn = 1
    )
    SELECT j.id AS journey_id
    FROM sit_journey j
    JOIN first_playable fp ON fp.journey_id = j.id
    WHERE j.server_id = :serverId
      AND j.cancelled = FALSE
      AND fp.cancelled = FALSE
      AND fp.realtime_time_type <> 'REAL'
      AND fp.scheduled_time < :cutoff
    """, nativeQuery = true)
  List<UUID> findJourneysThatDidNotSpawn(@Param("serverId") UUID serverId, @Param("cutoff") LocalDateTime cutoffTime);

  /**
   * Deletes all journeys from the database whose foreign run id is in the given collection and whose first seen time is
   * not yet set to a value.
   *
   * @param runIds the ids of the runs to possibly delete from the database.
   */
  @Modifying
  @Query(value = "DELETE FROM sit_journey j WHERE j.foreign_run_id IN :runIds AND j.first_seen_time IS NULL", nativeQuery = true)
  void deleteUnstartedJourneysByRunIds(@Param("runIds") Collection<UUID> runIds);

  /**
   * Finds the subset of the given foreign run ids that are not stored in the database.
   *
   * @param foreignRunIds the ids to find the subset of not stored ids of.
   * @return a subset of the given collection holding all ids that are not currently stored in the database.
   */
  // "Condition 'sj.foreign_run_id IS NULL' is always 'false'" is actually wrong, don't listen to it ¯\_(ツ)_/¯
  @Query(value = """
    SELECT j.foreign_run_id
    FROM unnest(cast(:runIds as uuid[])) as j(foreign_run_id)
    LEFT JOIN sit_journey sj ON sj.foreign_run_id = j.foreign_run_id
    WHERE sj.foreign_run_id IS NULL
    """, nativeQuery = true)
  List<UUID> findMissingRunIds(@Param("runIds") UUID[] foreignRunIds);
}
