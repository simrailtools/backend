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

package tools.simrail.backend.api.dispatchpost;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tools.simrail.backend.common.dispatchpost.SimRailDispatchPostEntity;
import tools.simrail.backend.common.dispatchpost.SimRailDispatchPostRepository;

interface ApiDispatchPostRepository extends SimRailDispatchPostRepository {

  /**
   * Finds dispatch posts using the given filter parameters and paging options. No filter parameters can be provided to
   * just get a paginated view of all dispatch posts. Multiple filter parameters are chained using a logical AND
   * operator.
   *
   * @param serverId   the id of the server on which the dispatch posts to return should be located.
   * @param difficulty the difficulty of the dispatch posts to return.
   * @param pointId    the point where with which the dispatch post is associated.
   * @param deleted    if the dispatch post should be deleted (no longer registered).
   * @param limit      the maximum items to return.
   * @param offset     the offset from which to start returning items.
   * @return a list of the dispatch posts that are matching the given parameters.
   */
  @Query(value = """
    SELECT e
    FROM sr_dispatch_post e
    WHERE
      (:serverId IS NULL OR e.serverId = :serverId)
      AND (:difficulty IS NULL OR e.difficultyLevel = :difficulty)
      AND (:pointId IS NULL OR e.pointId = :pointId)
      AND (:deleted IS NULL OR e.deleted = :deleted)
    ORDER BY e.registeredSince, e.id
    LIMIT :limit
    OFFSET :offset
    """)
  List<SimRailDispatchPostEntity> find(
    @Param("serverId") UUID serverId,
    @Param("difficulty") Integer difficulty,
    @Param("pointId") UUID pointId,
    @Param("deleted") Boolean deleted,
    @Param("limit") int limit,
    @Param("offset") int offset
  );
}
