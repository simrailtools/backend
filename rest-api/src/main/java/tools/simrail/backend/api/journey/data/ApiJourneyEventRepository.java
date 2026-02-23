/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024-present Pasqual Koschmieder and contributors
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

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tools.simrail.backend.common.journey.JourneyEventRepository;

public interface ApiJourneyEventRepository extends JourneyEventRepository {

  /**
   * Finds the first and last events of the journey with the given ids.
   *
   * @param journeyIds the ids of the journeys to find the first and last event of.
   * @return a list holding the first and last events of the journey with the given ids.
   */
  @Query(value = """
    (
      SELECT DISTINCT ON (e.journey_id)
        e.journey_id,
        e.scheduled_time,
        e.cancelled,
        e.event_index,
        e.transport_category,
        e.transport_category_external,
        e.transport_number,
        e.transport_type,
        e.transport_line,
        e.transport_label,
        e.point_id,
        e.in_playable_border AS point_playable
      FROM sit_journey_event e
      WHERE e.journey_id = ANY(:journeyIds)
      ORDER BY e.journey_id, e.event_index
    )
    UNION ALL
    (
      SELECT DISTINCT ON (e.journey_id)
        e.journey_id,
        e.scheduled_time,
        e.cancelled,
        e.event_index,
        e.transport_category,
        e.transport_category_external,
        e.transport_number,
        e.transport_type,
        e.transport_line,
        e.transport_label,
        e.point_id,
        e.in_playable_border AS point_playable
      FROM sit_journey_event e
      WHERE e.journey_id = ANY(:journeyIds)
      ORDER BY e.journey_id, e.event_index DESC
    )
    """, nativeQuery = true)
  List<JourneyEventSummaryProjection> findFirstAndLastEventOfJourneys(@Param("journeyIds") UUID[] journeyIds);
}
