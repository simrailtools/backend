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

package tools.simrail.backend.api.server;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import tools.simrail.backend.common.cache.DataCache;
import tools.simrail.backend.common.proto.EventBusProto;
import tools.simrail.backend.common.server.SimRailServerRepository;

@Service
class SimRailServerService {

  private final SimRailServerRepository serverRepository;
  private final SimRailServerDtoConverter serverDtoConverter;
  private final DataCache<EventBusProto.ServerUpdateFrame> serverDataCache;

  @Autowired
  SimRailServerService(
    @NonNull SimRailServerRepository serverRepository,
    @NonNull SimRailServerDtoConverter serverDtoConverter,
    @NonNull @Qualifier("server_data_cache") DataCache<EventBusProto.ServerUpdateFrame> serverDataCache
  ) {
    this.serverRepository = serverRepository;
    this.serverDtoConverter = serverDtoConverter;
    this.serverDataCache = serverDataCache;
  }

  /**
   * Lists all servers that are registered and are matching the given filter options.
   *
   * @param includeOffline if offline servers should be included in the returned list.
   * @param includeDeleted if deleted servers should be included in the returned list.
   * @return a list of all servers that are matching the given filter options.
   */
  public @NonNull List<SimRailServerDto> listServers(boolean includeOffline, boolean includeDeleted) {
    return this.serverRepository.findAll().stream()
      .filter(server -> !server.isDeleted() || includeDeleted)
      .filter(server -> {
        var serverData = this.serverDataCache.findByPrimaryKey(server.getId().toString());
        var serverIsOnline = serverData != null && serverData.getServerData().getOnline();
        return serverIsOnline || includeOffline;
      })
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
  public @NonNull Optional<SimRailServerDto> findServerById(@NonNull UUID id) {
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
  public @NonNull Optional<SimRailServerDto> findServerByCode(@NonNull String code) {
    return this.serverRepository.findByCode(code).map(this.serverDtoConverter);
  }
}
