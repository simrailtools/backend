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

package tools.simrail.backend.api.server;

import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import tools.simrail.backend.common.server.SimRailServerRepository;

@Service
class SimRailServerService {

  private final SimRailServerRepository serverRepository;
  private final SimRailServerDtoConverter serverDtoConverter;

  @Autowired
  public SimRailServerService(
    @Nonnull SimRailServerRepository serverRepository,
    @Nonnull SimRailServerDtoConverter serverDtoConverter
  ) {
    this.serverRepository = serverRepository;
    this.serverDtoConverter = serverDtoConverter;
  }

  /**
   * Lists all servers that are registered and are matching the given filter options.
   *
   * @param includeOffline if offline servers should be included in the returned list.
   * @param includeDeleted if deleted servers should be included in the returned list.
   * @return a list of all servers that are matching the given filter options.
   */
  @Cacheable(cacheNames = "server_cache", key = "'list_' + #includeOffline + '_' + #includeDeleted")
  public @Nonnull List<SimRailServerDto> listServers(boolean includeOffline, boolean includeDeleted) {
    return this.serverRepository.findAll().stream()
      .filter(server -> server.isOnline() || server.isDeleted() || includeOffline)
      .filter(server -> !server.isDeleted() || includeDeleted)
      .map(this.serverDtoConverter)
      .toList();
  }

  /**
   * Get detail information about a single server by the given id, returning an empty optional if no such server
   * exists.
   *
   * @param id the id of the server to get the details of.
   * @return an optional holding the detail data of the server with the given id, if one exists.
   */
  @Cacheable(cacheNames = "server_cache", key = "'by_id_' + #id")
  public @Nonnull Optional<SimRailServerDto> findServerById(@Nonnull UUID id) {
    return this.serverRepository.findById(id).map(this.serverDtoConverter);
  }

  /**
   * Get detail information about a single server by the given code, returning an empty optional if no such server
   * exists.
   *
   * @param code the code of the server to get the details of.
   * @return an optional holding the detail data of the server with the given code, if one exists.
   */
  @Cacheable(cacheNames = "server_cache", key = "'by_code_' + #code")
  public @Nonnull Optional<SimRailServerDto> findServerByCode(@Nonnull String code) {
    return this.serverRepository.findByCode(code).map(this.serverDtoConverter);
  }
}
