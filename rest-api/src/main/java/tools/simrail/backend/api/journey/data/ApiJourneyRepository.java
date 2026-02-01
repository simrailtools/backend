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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
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
  Optional<JourneyEntity> findWithEventsById(@NonNull UUID uuid);

  /**
   * Get a batch of journeys by the given ids with their event entities loaded as well.
   *
   * @param ids the ids of the journeys to resolve.
   * @return a list containing the journeys that were resolved using the given journey ids.
   */
  @EntityGraph(attributePaths = "events", type = EntityGraph.EntityGraphType.LOAD)
  List<JourneyEntity> findWithEventsByIdIn(@NonNull Collection<UUID> ids);

  /**
   * Get the journey summaries of the journeys with the given ids.
   *
   * @param journeyIds the ids of the journeys to get the summaries of.
   * @return the summaries of the journeys with the given ids.
   */
  @Query(value = """
    SELECT j.id, j.server_id, j.first_seen_time, j.last_seen_time, j.cancelled
    FROM sit_journey j
    WHERE j.id = ANY(:journeyIds)
    """, nativeQuery = true)
  List<JourneySummaryProjection> findJourneySummariesByJourneyIds(@Param("journeyIds") UUID[] journeyIds);

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
    WITH e AS (
      SELECT je.journey_id, min(je.scheduled_time) AS scheduled_time
      FROM sit_journey_event je
      WHERE je.scheduled_time >= :date::date
        AND je.scheduled_time < (:date::date + 1)
        AND (:line IS NULL OR je.transport_line = :line)
        AND (:journeyNumber IS NULL OR je.transport_number = :journeyNumber)
        AND (:journeyCategory IS NULL OR je.transport_category = :journeyCategory)
        AND je.transport_type = ANY(:transportTypes)
      GROUP BY je.journey_id
    )
    SELECT j.id, j.server_id, j.first_seen_time, j.last_seen_time, j.cancelled
    FROM e JOIN sit_journey j ON j.id = e.journey_id
    WHERE j.server_id = :serverId
    ORDER BY e.scheduled_time, j.id
    LIMIT :limit
    OFFSET :offset
    """, nativeQuery = true)
  List<JourneySummaryProjection> findJourneySummariesByMatchingEvent(
    @Param("serverId") UUID serverId,
    @Param("date") LocalDate date,
    @Param("line") String line,
    @Param("journeyNumber") String journeyNumber,
    @Param("journeyCategory") String journeyCategory,
    @Param("transportTypes") JourneyTransportType[] transportTypes,
    @Param("limit") int limit,
    @Param("offset") int offset
  );

  /**
   * Finds the journey summary projections of the journeys that become playable within the given time range.
   *
   * @param serverId        the id of the server to return journeys on.
   * @param journeyCategory the category of the journey at one event along the journey route.
   * @param transportTypes  the accepted transport types that the journey must have along the route.
   * @param rangeStart      the time to start returning journeys from.
   * @param rangeEnd        the time to end returning journeys from.
   * @param limit           the maximum amount of journeys to return.
   * @param offset          the offset to start returning item from.
   * @return a summary projection of the journeys matching the given filter parameters.
   */
  @Query(value = """
    WITH journeys_in_range AS (
      SELECT j.id AS journey_id
      FROM sit_journey j
      WHERE j.server_id = :serverId
        AND EXISTS (
          SELECT 1
          FROM sit_journey_event je
          WHERE je.journey_id = j.id
            AND je.transport_type = ANY(:transportTypes)
            AND je.scheduled_time >= :rangeStart::timestamp
            AND je.scheduled_time < :rangeEnd::timestamp
            AND (:journeyCategory IS NULL OR je.transport_category = :journeyCategory)
        )
    ),
    range_events AS (
      SELECT
        je.journey_id,
        je.event_index,
        je.scheduled_time,
        je.in_playable_border,
        je.point_id,
        je.cancelled
      FROM sit_journey_event je JOIN journeys_in_range jr ON jr.journey_id = je.journey_id
      WHERE je.scheduled_time >= :rangeStart::timestamp
        AND je.scheduled_time < :rangeEnd::timestamp
        AND (:journeyCategory IS NULL OR je.transport_category = :journeyCategory)
        AND je.transport_type = ANY(:transportTypes)
    ),
    prev_event AS (
      SELECT p.journey_id, p.event_index, p.scheduled_time, p.in_playable_border, p.point_id, p.cancelled
      FROM journeys_in_range jr
      JOIN LATERAL (
        SELECT
          je.journey_id,
          je.event_index,
          je.scheduled_time,
          je.in_playable_border,
          je.point_id,
          je.cancelled
        FROM sit_journey_event je
        WHERE je.journey_id = jr.journey_id
          AND je.scheduled_time < :rangeStart::timestamp
          AND (:journeyCategory IS NULL OR je.transport_category = :journeyCategory)
          AND je.transport_type = ANY(:transportTypes)
        ORDER BY je.scheduled_time DESC, je.event_index DESC
        LIMIT 1
      ) p ON true
    ),
    base AS (
      SELECT * FROM range_events
      UNION ALL
      SELECT * FROM prev_event
    ),
    w AS (
      SELECT
        b.*,
        lag(b.in_playable_border) OVER (
          PARTITION BY b.journey_id
          ORDER BY b.event_index
        ) AS prev_in_playable_border
      FROM base b
    )
    SELECT
      j.id,
      j.server_id,
      j.first_seen_time,
      j.last_seen_time,
      j.cancelled,
      w.point_id AS event_point_id,
      w.in_playable_border AS event_point_playable,
      w.scheduled_time AS event_scheduled_time,
      w.cancelled AS event_cancelled
    FROM w JOIN sit_journey j ON j.id = w.journey_id
    WHERE j.server_id = :serverId
      AND w.scheduled_time >= :rangeStart::timestamp
      AND w.scheduled_time < :rangeEnd::timestamp
      AND w.in_playable_border = TRUE
      AND COALESCE(w.prev_in_playable_border, FALSE) = FALSE
    ORDER BY w.scheduled_time, w.journey_id, w.event_index
    LIMIT :limit
    OFFSET :offset
    """, nativeQuery = true)
  List<JourneyWithEventSummaryProjection> findJourneySummariesByTimeAtPlayableBorderEnter(
    @Param("serverId") UUID serverId,
    @Param("journeyCategory") String journeyCategory,
    @Param("transportTypes") JourneyTransportType[] transportTypes,
    @Param("rangeStart") LocalDateTime rangeStart,
    @Param("rangeEnd") LocalDateTime rangeEnd,
    @Param("limit") int limit,
    @Param("offset") int offset
  );

  /**
   * Finds journeys that use all the given railcars (logical AND) in their vehicle composition on the given date.
   *
   * @param serverId        the id of the server to return journeys on.
   * @param date            the date of one of the events of the journey to return journeys of.
   * @param railcarIds      the ids of the railcars that must be in the vehicle composition.
   * @param journeyCategory the category of the journey at one event along the journey route.
   * @param transportTypes  the accepted transport types that the journey must have along the route.
   * @param limit           the maximum amount of journeys to return.
   * @param offset          the offset to start returning item from.
   * @return a summary projection of the journeys matching the given filter parameters.
   */
  @Query(value = """
    WITH required AS (
      SELECT jsonb_agg(jsonb_build_object('railcarId', v::text)) AS vehicles
      FROM (SELECT DISTINCT v FROM unnest(:railcarIds::uuid[]) u(v)) d
    ),
    js AS (
      SELECT id, server_id, first_seen_time, last_seen_time, cancelled
      FROM sit_journey
      WHERE server_id = :serverId
    )
    SELECT
        js.id,
        js.server_id,
        js.first_seen_time,
        js.last_seen_time,
        js.cancelled
    FROM js
    CROSS JOIN required r
    JOIN sit_journey_vehicle_sequence jvs ON jvs.journey_id = js.id AND jvs.vehicles @> r.vehicles
    JOIN LATERAL (
      SELECT je.scheduled_time
      FROM sit_journey_event je
      WHERE je.journey_id = js.id
        AND je.scheduled_time >= :date::date
        AND je.scheduled_time < (:date::date + 1)
        AND (:journeyCategory IS NULL OR je.transport_category = :journeyCategory)
        AND je.transport_type = ANY(:transportTypes)
      ORDER BY je.scheduled_time, je.event_index
      LIMIT 1
    ) e ON TRUE
    ORDER BY e.scheduled_time, js.id
    LIMIT :limit
    OFFSET :offset
    """, nativeQuery = true)
  List<JourneySummaryProjection> findJourneySummariesByRailcar(
    @Param("serverId") UUID serverId,
    @Param("date") LocalDate date,
    @Param("railcarIds") UUID[] railcarIds,
    @Param("journeyCategory") String journeyCategory,
    @Param("transportTypes") JourneyTransportType[] transportTypes,
    @Param("limit") int limit,
    @Param("offset") int offset
  );
}
