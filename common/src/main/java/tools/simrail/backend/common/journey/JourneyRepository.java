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

package tools.simrail.backend.common.journey;

import jakarta.annotation.Nonnull;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.query.Param;

/**
 * A repository for journeys.
 */
public interface JourneyRepository extends
  CrudRepository<JourneyEntity, UUID>,
  RevisionRepository<JourneyEntity, UUID, Long> {

  /**
   * Finds a single journey by the given server code and run id provided by the SimRail api.
   *
   * @param serverCode   the server code on which the journey is running.
   * @param foreignRunId the run id provided by the SimRail api of the journey.
   * @return an optional holding the journey on the given server with the given run id, if one exists.
   */
  @Nonnull
  Optional<JourneyEntity> findByServerCodeAndForeignRunId(@Nonnull String serverCode, @Nonnull UUID foreignRunId);

  /**
   * Finds the single journey by the given server code and foreign id that was last marked as active.
   *
   * @param serverCode   the server code on which the journey is running.
   * @param foreignRunId the foreign id of the journey provided by the SimRail api.
   * @return an optional holding the journey on the given server with the given id that was last marked as active.
   */
  @Nonnull
  @Query("SELECT e "
    + "FROM sit_journey e "
    + "WHERE e.serverCode = :serverCode AND e.foreignId = :foreignRunId AND e.firstSeenTime IS NOT NULL "
    + "ORDER BY e.firstSeenTime DESC "
    + "LIMIT 1")
  Optional<JourneyEntity> findLastActiveTrainByServerCodeAndForeignId(
    @Nonnull @Param("serverCode") String serverCode,
    @Nonnull @Param("foreignRunId") String foreignRunId);
}
