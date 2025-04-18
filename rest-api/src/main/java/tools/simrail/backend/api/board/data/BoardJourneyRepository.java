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

  @Query(
    value = """
      WITH initial_matching_events AS (
        SELECT DISTINCT ON (je.journey_id)
          je.journey_id,
          je.event_index,
          je.id,
          je.additional,
          je.cancelled,
          je.scheduled_time,
          je.realtime_time,
          je.realtime_time_type,
          je.scheduled_platform,
          je.scheduled_track,
          je.realtime_platform,
          je.realtime_track,
          je.transport_type,
          je.transport_category,
          je.transport_number,
          je.transport_line,
          je.transport_label,
          je.transport_max_speed
        FROM sit_journey_event je
        JOIN sit_journey j ON j.id = je.journey_id
        WHERE
          j.server_id = :serverId
          AND je.event_type = 1 -- select departure event at point
          AND je.point_id = :pointId
          AND je.transport_type IN :transportTypes
          AND ((je.realtime_time >= :spanStart AND je.realtime_time <= :spanEnd)
            OR (je.scheduled_time >= :spanStart AND je.scheduled_time <= :spanEnd))
      ),
      via_events AS (
        SELECT
          e.journey_id,
          e.point_id,
          e.point_name,
          e.event_index,
          e.cancelled,
          e.additional,
          -- initial event data
          ie.id AS initial_event_id,
          ie.additional AS initial_additional,
          ie.cancelled AS initial_cancelled,
          ie.scheduled_time AS initial_scheduled_time,
          ie.realtime_time AS initial_realtime_time,
          ie.realtime_time_type AS initial_realtime_time_type,
          ie.scheduled_platform AS initial_scheduled_platform,
          ie.scheduled_track AS initial_scheduled_track,
          ie.realtime_platform AS initial_realtime_platform,
          ie.realtime_track AS initial_realtime_track,
          ie.transport_type AS initial_transport_type,
          ie.transport_category AS initial_transport_category,
          ie.transport_number AS initial_transport_number,
          ie.transport_line AS initial_transport_line,
          ie.transport_label AS initial_transport_label,
          ie.transport_max_speed AS initial_transport_max_speed
        FROM sit_journey_event e
        INNER JOIN initial_matching_events ie ON e.journey_id = ie.journey_id
        WHERE
          e.event_index > ie.event_index
          AND e.event_type = 0 -- select arrival event for follow-ups (catches final arrival event)
      )
      SELECT *
      FROM via_events
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
