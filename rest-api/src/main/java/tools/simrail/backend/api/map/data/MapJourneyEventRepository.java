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

package tools.simrail.backend.api.map.data;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tools.simrail.backend.common.journey.JourneyEventRepository;

public interface MapJourneyEventRepository extends JourneyEventRepository {

  /**
   * Finds the relevant event data for map plotting.
   *
   * @param journeyId         the id of the journey to get the map data of.
   * @param includeCancelled  if canceled events should be included in the result
   * @param includeAdditional if additional events should be included in the result.
   * @return the relevant event data for map plotting for the given journey id.
   */
  @Query(value = """
    SELECT DISTINCT ON (e.point_id)
      e.journey_id, e.event_index, e.point_id, e.in_playable_border
    FROM sit_journey_event e
    WHERE
      e.journey_id = :journeyId
      AND (TRUE = :#{#includeCancelled} OR e.cancelled = :includeCancelled)
      AND (TRUE = :#{#includeAdditional} OR e.additional = :includeAdditional)
    """, nativeQuery = true)
  List<MapEventSummaryProjection> findMapEventDataByJourneyId(
    @Param("journeyId") UUID journeyId,
    @Param("includeCancelled") boolean includeCancelled,
    @Param("includeAdditional") boolean includeAdditional);
}
