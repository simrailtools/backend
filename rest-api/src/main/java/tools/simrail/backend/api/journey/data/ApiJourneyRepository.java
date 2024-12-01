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

package tools.simrail.backend.api.journey.data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tools.simrail.backend.common.journey.JourneyEntity;
import tools.simrail.backend.common.journey.JourneyRepository;

public interface ApiJourneyRepository extends JourneyRepository {

  /**
   *
   * @param uuid
   * @return
   */
  @EntityGraph(attributePaths = "events", type = EntityGraph.EntityGraphType.LOAD)
  Optional<JourneyEntity> findWithEventsById(UUID uuid);

  @Query(value = """
    WITH first_events AS (
      SELECT
        e.journey_id,
        e.scheduled_time as time,
        e.point_id AS station_id,
        e.transport_number AS journey_number,
        e.transport_category AS journey_category
      FROM sit_journey_event e
      WHERE e.event_index = 0
    ),
    last_events AS (
      SELECT
        e.journey_id,
        e.scheduled_time AS time,
        e.point_id AS station_id
      FROM sit_journey_event e
      WHERE e.event_index = (SELECT MAX(e2.event_index) FROM sit_journey_event e2 WHERE e2.journey_id = e.journey_id)
    )
    SELECT j.id, j.server_id, j.first_seen_time, j.last_seen_time, j.cancelled
    FROM sit_journey j
    JOIN first_events fe ON fe.journey_id = j.id
    JOIN last_events le ON le.journey_id = j.id
    WHERE
      (:serverId IS NULL OR j.server_id = :serverId)
      AND (TRUE = :#{#startTime == null} OR fe.time = :startTime)
      AND (:startStationId IS NULL OR fe.station_id = :startStationId)
      AND (:startJourneyNumber IS NULL OR fe.journey_number = :startJourneyNumber)
      AND (:startJourneyCategory IS NULL OR fe.journey_category = :startJourneyCategory)
      AND (TRUE = :#{#endTime == null} OR le.time = :endTime)
      AND (:endStationId IS NULL OR le.station_id = :endStationId)
    ORDER BY fe.time
    LIMIT :limit
    OFFSET :offset
    """, nativeQuery = true)
  List<JourneySummaryProjection> findMatchingJourneySummaries(
    @Param("serverId") UUID serverId,
    @Param("startTime") OffsetDateTime startTime,
    @Param("startStationId") UUID startStationId,
    @Param("startJourneyNumber") String startJourneyNumber,
    @Param("startJourneyCategory") String startJourneyCategory,
    @Param("endTime") OffsetDateTime endTime,
    @Param("endStationId") UUID endStationId,
    @Param("limit") int limit,
    @Param("offset") int offset
  );
}
