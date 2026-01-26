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

package tools.simrail.backend.collector.server;

import jakarta.transaction.Transactional;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.simrail.backend.common.server.SimRailServerEntity;
import tools.simrail.backend.common.server.SimRailServerRegion;
import tools.simrail.backend.common.server.SimRailServerRepository;
import tools.simrail.backend.common.util.MongoIdDecodeUtil;
import tools.simrail.backend.external.srpanel.model.SimRailPanelServer;

@Service
class CollectorServerService {

  private final SimRailServerRepository serverRepository;

  @Autowired
  public CollectorServerService(@NonNull SimRailServerRepository serverRepository) {
    this.serverRepository = serverRepository;
  }

  /**
   * Saves data about the given server into the database.
   *
   * @param server              the full data of the server.
   * @param serverUpdaterHolder the holder for all the live data of the server.
   */
  @Transactional
  public void saveServer(@NonNull SimRailPanelServer server, @NonNull ServerUpdateHolder serverUpdaterHolder) {
    var serverEntity = new SimRailServerEntity();
    serverEntity.setId(serverUpdaterHolder.id);
    serverEntity.setForeignId(serverUpdaterHolder.foreignId);
    serverEntity.setCode(server.getCode());
    serverEntity.setDeleted(false); // can't be deleted

    serverEntity.setTags(serverUpdaterHolder.tags.currentValue());
    serverEntity.setScenery(serverUpdaterHolder.scenery.currentValue());
    serverEntity.setSpokenLanguage(serverUpdaterHolder.spokenLanguage.currentValue());

    var utcOffsetHours = ServerTimeUtil.convertToHours(serverUpdaterHolder.utcOffsetSeconds.currentValue());
    serverEntity.setUtcOffsetHours(utcOffsetHours);

    var region = switch (server.getRegion()) {
      case ASIA -> SimRailServerRegion.ASIA;
      case EUROPE -> SimRailServerRegion.EUROPE;
      case US_NORTH -> SimRailServerRegion.US_NORTH;
    };
    serverEntity.setRegion(region);

    var registeredSince = MongoIdDecodeUtil.parseMongoId(serverUpdaterHolder.foreignId);
    serverEntity.setRegisteredSince(registeredSince);
    this.serverRepository.save(serverEntity);
  }

  /**
   * Marks all servers whose id is not contained in the given server ids set as deleted.
   *
   * @param registeredServerIds the ids of the servers that are still registered.
   */
  @Transactional
  public void markUncontainedServersAsDeleted(@NonNull Set<UUID> registeredServerIds) {
    var missingServers = this.serverRepository.findAllByIdNotInAndNotDeleted(registeredServerIds);
    for (var missingServer : missingServers) {
      missingServer.setDeleted(true);
      this.serverRepository.save(missingServer);
    }
  }
}
