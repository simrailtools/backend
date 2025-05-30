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

package tools.simrail.backend.api.journey.data;

import jakarta.annotation.Nonnull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tools.simrail.backend.common.journey.JourneyEntity;
import tools.simrail.backend.common.journey.JourneyRepository;
import tools.simrail.backend.common.journey.JourneyTransportType;

public interface ApiJourneyRepository extends JourneyRepository {

  /**
   * Fetch the full details of a journey from the database, also eagerly fetching the events of the journey.
   *
   * @param uuid the id of the journey to get.
   * @return an optional holding the full journey data associated with the given id, if one exists.
   */
  @EntityGraph(attributePaths = "events", type = EntityGraph.EntityGraphType.LOAD)
  Optional<JourneyEntity> findWithEventsById(@Nonnull UUID uuid);

  /**
   * Get a batch of journeys by the given ids with their event entities loaded as well.
   *
   * @param ids the ids of the journeys to resolve.
   * @return a list containing the journeys that were resolved using the given journey ids.
   */
  @EntityGraph(attributePaths = "events", type = EntityGraph.EntityGraphType.LOAD)
  List<JourneyEntity> findWithEventsByIdIn(@Nonnull Collection<UUID> ids);

  /**
   * Finds the journey summary projections by the matching tails of a journey.
   *
   * @param serverId             the id of the server to return journeys on.
   * @param startTime            the exact time when the journey starts.
   * @param startStationId       the exact id of the point where the journey starts.
   * @param startJourneyNumber   the exact number of the journey at the first point.
   * @param startJourneyCategory the exact category of the journey at the first point.
   * @param endTime              the exact time when the journey ends.
   * @param endStationId         the exact id of the point where the journey ends.
   * @param limit                the maximum amount of journeys to return.
   * @param offset               the offset to start returning item from.
   * @return a summary projection of the journeys matching the given filter parameters.
   */
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
      j.server_id = :serverId
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
  List<JourneySummaryProjection> findJourneySummariesByTails(
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

  /**
   * Finds the journey summary projections by one matching event along the route of the journey.
   *
   * @param serverId        the id of the server to return journeys on.
   * @param date            the date that one event is happening on.
   * @param line            the line that must match at one event along the journey route.
   * @param journeyNumber   the number that must be used at one event along the journey route.
   * @param journeyCategory the category of the journey at one event along the journey route.
   * @param transportTypes  the accepted transport types that the journey must have along the route.
   * @param limit           the maximum amount of journeys to return.
   * @param offset          the offset to start returning item from.
   * @return a summary projection of the journeys matching the given filter parameters.
   */
  @Query(value = """
    WITH distinct_journeys AS (
      SELECT DISTINCT ON (j.id)
        j.id, j.server_id, j.first_seen_time, j.last_seen_time, j.cancelled, je.scheduled_time
      FROM sit_journey j
      JOIN sit_journey_event je ON je.journey_id = j.id
      WHERE
        j.server_id = :serverId
        AND (je.scheduled_time >= CAST(:date AS TIMESTAMP) AND
          je.scheduled_time < CAST(:date AS TIMESTAMP) + INTERVAL '1 day')
        AND (:line IS NULL OR je.transport_line = :line)
        AND (:journeyNumber IS NULL OR je.transport_number = :journeyNumber)
        AND (:journeyCategory IS NULL OR je.transport_category = :journeyCategory)
        AND (je.transport_type IN :transportTypes)
    )
    SELECT *
    FROM distinct_journeys
    ORDER BY scheduled_time
    LIMIT :limit
    OFFSET :offset
    """, nativeQuery = true)
  List<JourneySummaryProjection> findJourneySummariesByMatchingEvent(
    @Param("serverId") UUID serverId,
    @Param("date") LocalDate date,
    @Param("line") String line,
    @Param("journeyNumber") String journeyNumber,
    @Param("journeyCategory") String journeyCategory,
    @Param("transportTypes") List<JourneyTransportType> transportTypes,
    @Param("limit") int limit,
    @Param("offset") int offset
  );

  /**
   * Finds the journey summary projections whose first playable event is within the given time range.
   *
   * @param serverId        the id of the server to return journeys on.
   * @param line            the line that must match at one event along the journey route.
   * @param journeyCategory the category of the journey at one event along the journey route.
   * @param transportTypes  the accepted transport types that the journey must have along the route.
   * @param rangeStart      the time to start returning journeys from.
   * @param rangeEnd        the time to end returning journeys from.
   * @param limit           the maximum amount of journeys to return.
   * @param offset          the offset to start returning item from.
   * @return a summary projection of the journeys matching the given filter parameters.
   */
  @Query(value = """
    WITH first_playable_events AS (
      SELECT
        e.journey_id,
        e.scheduled_time,
        e.transport_line,
        e.transport_type,
        e.transport_category,
        e.point_id,
        e.point_name,
        e.cancelled
      FROM sit_journey_event e
      WHERE e.event_index = (
        SELECT MIN(e2.event_index)
        FROM sit_journey_event e2
        WHERE e2.journey_id = e.journey_id AND e2.point_playable = TRUE
      )
    )
    SELECT
      j.id,
      j.server_id,
      j.first_seen_time,
      j.last_seen_time,
      j.cancelled,
      fe.point_id AS fe_point_id,
      fe.point_name AS fe_point_name,
      fe.scheduled_time AS fe_scheduled_time,
      fe.cancelled AS fe_cancelled
    FROM sit_journey j
    JOIN first_playable_events fe ON fe.journey_id = j.id
    WHERE
      j.server_id = :serverId
      AND (fe.scheduled_time BETWEEN :start AND :end)
      AND (:line IS NULL OR fe.transport_line = :line)
      AND (:journeyCategory IS NULL OR fe.transport_category = :journeyCategory)
      AND (fe.transport_type IN :transportTypes)
    ORDER BY fe.scheduled_time
    LIMIT :limit
    OFFSET :offset
    """, nativeQuery = true)
  List<JourneyWithEventSummaryProjection> findJourneySummariesByTimeAtFirstPlayableEvent(
    @Param("serverId") UUID serverId,
    @Param("line") String line,
    @Param("journeyCategory") String journeyCategory,
    @Param("transportTypes") List<JourneyTransportType> transportTypes,
    @Param("start") OffsetDateTime rangeStart,
    @Param("end") OffsetDateTime rangeEnd,
    @Param("limit") int limit,
    @Param("offset") int offset
  );

  /**
   * Finds journeys that use the given railcar in their vehicle composition on the given date.
   *
   * @param serverId  the id of the server to return journeys on.
   * @param date      the date of one of the events of the journey to return journeys of.
   * @param railcarId the id of the railcar that must be included in the vehicle composition of a journey.
   * @param limit     the maximum amount of journeys to return.
   * @param offset    the offset to start returning item from.
   * @return a summary projection of the journeys matching the given filter parameters.
   */
  @Query(value = """
    WITH distinct_journeys AS (
      SELECT DISTINCT ON (j.id)
        j.id, j.server_id, j.first_seen_time, j.last_seen_time, j.cancelled, je.scheduled_time
      FROM sit_journey j
      JOIN sit_journey_event je ON j.id = je.journey_id
      JOIN sit_vehicle v ON j.id = v.journey_id
      WHERE
        j.server_id = :serverId
        AND (je.scheduled_time >= CAST(:date AS TIMESTAMP) AND
             je.scheduled_time < CAST(:date AS TIMESTAMP) + INTERVAL '1 day')
        AND v.railcar_id = :railcarId
    )
    SELECT *
    FROM distinct_journeys
    ORDER BY scheduled_time
    LIMIT :limit
    OFFSET :offset
    """, nativeQuery = true)
  List<JourneySummaryProjection> findJourneySummariesByRailcar(
    @Param("serverId") UUID serverId,
    @Param("date") LocalDate date,
    @Param("railcarId") UUID railcarId,
    @Param("limit") int limit,
    @Param("offset") int offset
  );
}
