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

package tools.simrail.backend.common.server;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;

/**
 * A repository for SimRail server information.
 */
@Primary
public interface SimRailServerRepository extends ListCrudRepository<SimRailServerEntity, UUID> {

  /**
   * Finds a single SimRail server by the given server code.
   *
   * @param code the code of the server to get.
   * @return an optional holding the server entity if an entity with the given code exists.
   */
  @NonNull
  Optional<SimRailServerEntity> findByCode(@NonNull String code);

  /**
   * Finds a single SimRail server by the given foreign id.
   *
   * @param foreignId the foreign id of the server to get.
   * @return an optional holding the server entity if an entity with the given foreign id exists.
   */
  @NonNull
  Optional<SimRailServerEntity> findByForeignId(@NonNull String foreignId);

  /**
   * Finds all server whose id is not in the given id collection and is not deleted.
   *
   * @param ids the ids of the servers which shouldn't be returned.
   * @return all servers whose id is not in the given collection and not marked as deleted.
   */
  @NonNull
  @Query("SELECT s FROM sr_server s WHERE s.id NOT IN :ids AND s.deleted IS false")
  List<SimRailServerEntity> findAllByIdNotInAndNotDeleted(@NonNull @Param("ids") Collection<UUID> ids);
}
