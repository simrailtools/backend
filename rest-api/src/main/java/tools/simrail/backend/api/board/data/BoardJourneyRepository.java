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

package tools.simrail.backend.api.board.data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tools.simrail.backend.common.journey.JourneyRepository;
import tools.simrail.backend.common.journey.JourneyTransportType;

public interface BoardJourneyRepository extends JourneyRepository {

  /**
   * Get all events for a departure board matching the given filter parameters. The result holds the matched arrival event
   *
   * @param serverId       the id of the server to match the arrival on.
   * @param pointId        the id of the point to get the arrivals of.
   * @param timeSpanStart  the start time of the arrivals to return.
   * @param timeSpanEnd    the end time of the arrivals to return.
   * @param transportTypes the types of transports to filter.
   * @return all arrivals at the given point on the given server that are matching the filter criteria.
   */
  @Query(
    value = """
      WITH matching_arrivals AS (
        -- find scheduled events in range
        SELECT
          je.id,
          je.journey_id,
          je.event_index,
          je.scheduled_time,
          je.realtime_time
        FROM sit_journey_event je
        WHERE je.point_id = :pointId
          AND je.event_type = 'ARRIVAL'
          AND je.transport_type = ANY(:transportTypes)
          AND je.scheduled_time BETWEEN :spanStart AND :spanEnd
          AND EXISTS (SELECT 1 FROM sit_journey j WHERE j.id = je.journey_id AND j.server_id = :serverId)
        UNION ALL
        -- find realtime events in range whose scheduled time is not in range
        SELECT
          je.id,
          je.journey_id,
          je.event_index,
          je.scheduled_time,
          je.realtime_time
        FROM sit_journey_event je
        WHERE je.point_id = :pointId
          AND je.event_type = 'ARRIVAL'
          AND je.transport_type = ANY(:transportTypes)
          AND je.realtime_time BETWEEN :spanStart AND :spanEnd
          AND NOT (je.scheduled_time BETWEEN :spanStart AND :spanEnd)
          AND EXISTS (SELECT 1 FROM sit_journey j WHERE j.id = je.journey_id AND j.server_id = :serverId)
      ),
      initial_matching_events AS (
        SELECT DISTINCT ON (journey_id)
          id,
          journey_id,
          event_index,
          scheduled_time,
          realtime_time
        FROM matching_arrivals
        ORDER BY journey_id, event_index
      )
      SELECT
        e.journey_id,
        e.point_id,
        e.event_index,
        e.cancelled,
        e.additional,
        e.scheduled_platform,
        ie_full.id AS initial_event_id,
        ie_full.additional AS initial_additional,
        ie_full.cancelled AS initial_cancelled,
        ie_full.scheduled_time AS initial_scheduled_time,
        ie_full.realtime_time  AS initial_realtime_time,
        ie_full.realtime_time_type AS initial_realtime_time_type,
        ie_full.scheduled_platform AS initial_scheduled_platform,
        ie_full.scheduled_track AS initial_scheduled_track,
        ie_full.realtime_platform AS initial_realtime_platform,
        ie_full.realtime_track AS initial_realtime_track,
        ie_full.stop_type AS initial_stop_type,
        ie_full.transport_type AS initial_transport_type,
        ie_full.transport_category AS initial_transport_category,
        ie_full.transport_number AS initial_transport_number,
        ie_full.transport_line AS initial_transport_line,
        ie_full.transport_label AS initial_transport_label,
        ie_full.transport_max_speed AS initial_transport_max_speed
      FROM initial_matching_events ie
      JOIN sit_journey_event ie_full ON ie_full.id = ie.id
      JOIN sit_journey_event e
        ON e.journey_id = ie.journey_id
        AND e.event_type = 'DEPARTURE'
        AND e.event_index < ie.event_index
      ORDER BY
        e.journey_id,
        e.event_index
      """, nativeQuery = true
  )
  List<BoardJourneyProjection> getArrivals(
    @Param("serverId") UUID serverId,
    @Param("pointId") UUID pointId,
    @Param("spanStart") OffsetDateTime timeSpanStart,
    @Param("spanEnd") OffsetDateTime timeSpanEnd,
    @Param("transportTypes") List<JourneyTransportType> transportTypes
  );

  /**
   * Get a projection of all journey events that are after the initially matched event.
   *
   * @param serverId       the id of the server to match the departures on.
   * @param pointId        the id of the point to get the departures of.
   * @param timeSpanStart  the start time of the departures to return.
   * @param timeSpanEnd    the end time of the departures to return.
   * @param transportTypes the types of transports to filter.
   * @return all departures at the given point on the given server that are matching the filter criteria.
   */
  @Query(
    value = """
      WITH matching_departures AS (
        -- find scheduled events in range
        SELECT
          je.id,
          je.journey_id,
          je.event_index,
          je.scheduled_time,
          je.realtime_time
        FROM sit_journey_event je
        WHERE je.point_id = :pointId
          AND je.event_type = 'DEPARTURE'
          AND je.transport_type = ANY(:transportTypes)
          AND je.scheduled_time BETWEEN :spanStart AND :spanEnd
          AND EXISTS (SELECT 1 FROM sit_journey j WHERE j.id = je.journey_id AND j.server_id = :serverId)
        UNION ALL
        -- find realtime events in range whose scheduled time is not in range
        SELECT
          je.id,
          je.journey_id,
          je.event_index,
          je.scheduled_time,
          je.realtime_time
        FROM sit_journey_event je
        WHERE je.point_id = :pointId
          AND je.event_type = 'DEPARTURE'
          AND je.transport_type = ANY(:transportTypes)
          AND je.realtime_time BETWEEN :spanStart AND :spanEnd
          AND NOT (je.scheduled_time BETWEEN :spanStart AND :spanEnd)
          AND EXISTS (SELECT 1 FROM sit_journey j WHERE j.id = je.journey_id AND j.server_id = :serverId)
      ),
      initial_matching_events AS (
        SELECT DISTINCT ON (journey_id)
          id,
          journey_id,
          event_index,
          scheduled_time,
          realtime_time
        FROM matching_departures
        ORDER BY journey_id, event_index
      )
      SELECT
        e.journey_id,
        e.point_id,
        e.event_index,
        e.cancelled,
        e.additional,
        e.scheduled_platform,
        ie_full.id AS initial_event_id,
        ie_full.additional AS initial_additional,
        ie_full.cancelled AS initial_cancelled,
        ie_full.scheduled_time AS initial_scheduled_time,
        ie_full.realtime_time  AS initial_realtime_time,
        ie_full.realtime_time_type AS initial_realtime_time_type,
        ie_full.scheduled_platform AS initial_scheduled_platform,
        ie_full.scheduled_track AS initial_scheduled_track,
        ie_full.realtime_platform AS initial_realtime_platform,
        ie_full.realtime_track AS initial_realtime_track,
        ie_full.stop_type AS initial_stop_type,
        ie_full.transport_type AS initial_transport_type,
        ie_full.transport_category AS initial_transport_category,
        ie_full.transport_number AS initial_transport_number,
        ie_full.transport_line AS initial_transport_line,
        ie_full.transport_label AS initial_transport_label,
        ie_full.transport_max_speed AS initial_transport_max_speed
      FROM initial_matching_events ie
      JOIN sit_journey_event ie_full ON ie_full.id = ie.id
      JOIN sit_journey_event e
        ON e.journey_id = ie.journey_id
        AND e.event_type = 'ARRIVAL'
        AND e.event_index > ie.event_index
      ORDER BY
        e.journey_id,
        e.event_index
      """, nativeQuery = true
  )
  List<BoardJourneyProjection> getDepartures(
    @Param("serverId") UUID serverId,
    @Param("pointId") UUID pointId,
    @Param("spanStart") OffsetDateTime timeSpanStart,
    @Param("spanEnd") OffsetDateTime timeSpanEnd,
    @Param("transportTypes") List<JourneyTransportType> transportTypes
  );
}
