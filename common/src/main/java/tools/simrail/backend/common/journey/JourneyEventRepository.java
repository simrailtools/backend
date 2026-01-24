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

package tools.simrail.backend.common.journey;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * A repository for journey events.
 */
public interface JourneyEventRepository extends JpaRepository<JourneyEventEntity, UUID> {

  /**
   * Get all events of the given journey.
   *
   * @param journeyId the id of the journey to get the events of.
   * @return all events of the given journey.
   */
  @NonNull
  List<JourneyEventEntity> findAllByJourneyId(@NonNull UUID journeyId);

  /**
   * Retrieves all journey events that are associated with a journey on the given server and with one of the run ids.
   *
   * @param serverId the id of the server where the journeys are running on.
   * @param runIds   the ids of the runs to get the associated journey events of.
   * @return the journey events associated with a journey on the given server and with one of the run ids.
   */
  @NonNull
  @Query("SELECT e FROM sit_journey_event e WHERE e.journey.id IN ("
    + "  SELECT j.id FROM sit_journey j"
    + "  WHERE j.serverId = :serverId"
    + "  AND j.foreignRunId IN :runIds"
    + "  AND j.firstSeenTime IS NULL"
    + ")")
  List<JourneyEventEntity> findAllInactiveByServerIdAndRunId(
    @NonNull @Param("serverId") UUID serverId,
    @NonNull @Param("runIds") Collection<UUID> runIds);
}
