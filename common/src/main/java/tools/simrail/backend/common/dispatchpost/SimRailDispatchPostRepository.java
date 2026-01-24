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

package tools.simrail.backend.common.dispatchpost;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.data.repository.ListCrudRepository;

/**
 * A repository for SimRail dispatch post information.
 */
public interface SimRailDispatchPostRepository extends ListCrudRepository<SimRailDispatchPostEntity, UUID> {

  /**
   * Finds a single dispatch post by the given point id.
   *
   * @param pointId the point id of the dispatch post to get.
   * @return an optional holding the dispatch post entity if an entity with the given point id exists.
   */
  @NonNull
  Optional<SimRailDispatchPostEntity> findByPointId(@NonNull UUID pointId);

  /**
   * Finds all dispatch post entities for the given server code.
   *
   * @param serverId the id of the server to get the dispatch posts of.
   * @return all dispatch posts that are registered for the given server code.
   */
  @NonNull
  List<SimRailDispatchPostEntity> findAllByServerId(@NonNull UUID serverId);

  /**
   * Finds all dispatch posts on the given server whose foreign id is not in the given set and are not deleted.
   *
   * @param serverId   the id of the server the dispatch posts must be located on.
   * @param foreignIds the foreign ids of the dispatch posts to not return.
   * @return the dispatch posts on the given server that don't have one of the given foreign ids.
   */
  @NonNull
  List<SimRailDispatchPostEntity> findAllByServerIdAndForeignIdNotInAndDeletedIsFalse(
    @NonNull UUID serverId,
    @NonNull Set<String> foreignIds);
}
