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

package tools.simrail.backend.collector.cleanup;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tools.simrail.backend.common.journey.JourneyRepository;

interface CleanupJourneyRepository extends JourneyRepository {

  /**
   * Finds the ids of all journeys that received their last update before the given start time.
   *
   * @param cleanupStartTime the start time to find journeys without a data update from.
   * @return the ids of the journeys that didn't receive an update after the given time.
   */
  @Query(value = "SELECT j.id FROM sit_journey j WHERE j.update_time < :time", nativeQuery = true)
  List<UUID> findJourneyIdsByCleanupStartDate(@Param("time") Instant cleanupStartTime);

  /**
   * Deletes all journeys with one of the given ids.
   *
   * @param journeyIds the ids of the journeys to delete.
   */
  @Modifying
  @Query(value = "DELETE FROM sit_journey j WHERE j.id IN :journeyIds", nativeQuery = true)
  void deleteAllByJourneyIdIn(@Param("journeyIds") Collection<UUID> journeyIds);
}
