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

package tools.simrail.backend.common.dispatchpost;

import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.history.RevisionRepository;

/**
 * A repository for SimRail dispatch post information.
 */
public interface SimRailDispatchPostRepository extends
  ListCrudRepository<SimRailDispatchPostEntity, UUID>,
  RevisionRepository<SimRailDispatchPostEntity, UUID, Long> {

  /**
   * Finds a single dispatch post by the given point id.
   *
   * @param pointId the point id of the dispatch post to get.
   * @return an optional holding the dispatch post entity if an entity with the given point id exists.
   */
  @Nonnull
  Optional<SimRailDispatchPostEntity> findByPointId(@Nonnull UUID pointId);

  /**
   * Finds a single dispatch post by the given foreign id.
   *
   * @param id       the foreign id of the dispatch post to get.
   * @param serverId the server id of the server to get the dispatch post on.
   * @return an optional holding the dispatch post entity if an entity with the given foreign id exists.
   */
  @Nonnull
  Optional<SimRailDispatchPostEntity> findByForeignIdAndServerId(@Nonnull String id, @Nonnull UUID serverId);

  /**
   * Finds all dispatch post entities for the given server code.
   *
   * @param serverId the id of the server to get the dispatch posts of.
   * @return all dispatch posts that are registered for the given server code.
   */
  @Nonnull
  List<SimRailDispatchPostEntity> findAllByServerId(@Nonnull UUID serverId);
}
