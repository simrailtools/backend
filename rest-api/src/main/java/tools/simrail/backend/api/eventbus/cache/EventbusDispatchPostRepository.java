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

package tools.simrail.backend.api.eventbus.cache;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tools.simrail.backend.api.eventbus.dto.EventbusDispatchPostSnapshotDto;
import tools.simrail.backend.common.dispatchpost.SimRailDispatchPostRepository;

interface EventbusDispatchPostRepository extends SimRailDispatchPostRepository {

  /**
   * Get the snapshots of all active dispatch posts on all servers.
   *
   * @return a list of snapshots for each active dispatch post on all servers.
   */
  @Query(value = """
    SELECT new tools.simrail.backend.api.eventbus.dto.EventbusDispatchPostSnapshotDto(d)
    FROM sr_dispatch_post d
    WHERE d.deleted IS FALSE
    """)
  List<EventbusDispatchPostSnapshotDto> findSnapshotsOfAllActiveDispatchPosts();

  /**
   * Get a snapshot of the dispatch post associated with the given id, if one exists.
   *
   * @param postId the id of the dispatch post to get the snapshot of.
   * @return an optional holding a snapshot of the dispatch post with the given id, if one exists.
   */
  @Query(value = """
    SELECT new tools.simrail.backend.api.eventbus.dto.EventbusDispatchPostSnapshotDto(d)
    FROM sr_dispatch_post d
    WHERE d.id = :postId
    """)
  Optional<EventbusDispatchPostSnapshotDto> findDispatchPostSnapshotById(@Param("postId") UUID postId);
}
