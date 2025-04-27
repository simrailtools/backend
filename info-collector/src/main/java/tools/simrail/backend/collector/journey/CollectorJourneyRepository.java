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

import jakarta.annotation.Nonnull;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tools.simrail.backend.common.journey.JourneyEntity;
import tools.simrail.backend.common.journey.JourneyRepository;

/**
 * Extension of the default journey repository with collector-specific methods.
 */
interface CollectorJourneyRepository extends JourneyRepository {

  @Nonnull
  List<JourneyEntity> findAllByFirstSeenTimeIsNotNullAndLastSeenTimeIsNull();

  @Nonnull
  List<JourneyEntity> findAllByServerIdAndFirstSeenTimeIsNotNullAndLastSeenTimeIsNull(@Nonnull UUID serverId);

  @Nonnull
  List<JourneyEntity> findAllByServerIdAndForeignRunIdIn(UUID serverId, Collection<UUID> foreignRunIds);

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
    WHERE
      -- no need to select lastSeenTime here, but this way an index can be used more effectively
      j.server_id = :serverId
      AND j.first_seen_time IS NULL
      AND j.last_seen_time IS NULL
    """, nativeQuery = true)
  List<UUID> findJourneysThatDidNotSpawn(@Param("time") OffsetDateTime serverTime, @Param("serverId") UUID serverId);

  /**
   * Marks the journeys with the given journey ids as canceled.
   *
   * @param serverTime the current server time to set as the last updated time of the journey.
   * @param journeyIds the ids of the journeys to mark as canceled.
   */
  @Modifying
  @Query("UPDATE sit_journey j SET j.cancelled = TRUE, j.updateTime = :time WHERE j.id IN :journeyIds")
  void markJourneysAsCancelled(
    @Param("time") OffsetDateTime serverTime,
    @Param("journeyIds") Collection<UUID> journeyIds);

  /**
   * Marks the journey events associated with one of the given journeys as canceled.
   *
   * @param journeyIds the ids of the journeys whose events should be marked as canceled.
   */
  @Modifying
  @Query("UPDATE sit_journey_event je SET je.cancelled = TRUE WHERE je.journeyId IN :journeyIds")
  void markJourneyEventsAsCancelled(@Param("journeyIds") Collection<UUID> journeyIds);
}
