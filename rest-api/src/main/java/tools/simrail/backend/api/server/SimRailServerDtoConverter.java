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

import java.util.function.Function;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import tools.simrail.backend.common.cache.DataCache;
import tools.simrail.backend.common.proto.EventBusProto;
import tools.simrail.backend.common.server.SimRailServerEntity;

/**
 * A converter for server entities to server DTOs.
 */
@Component
final class SimRailServerDtoConverter implements Function<SimRailServerEntity, SimRailServerDto> {

  private final DataCache<EventBusProto.ServerUpdateFrame> serverDataCache;

  @Autowired
  SimRailServerDtoConverter(
    @NonNull @Qualifier("server_data_cache") DataCache<EventBusProto.ServerUpdateFrame> serverDataCache
  ) {
    this.serverDataCache = serverDataCache;
  }

  @Override
  public @NonNull SimRailServerDto apply(@NonNull SimRailServerEntity server) {
    var serverData = this.serverDataCache.findByPrimaryKey(server.getId().toString());
    var serverIsOnline = serverData != null && serverData.getServerData().getOnline();
    return new SimRailServerDto(
      server.getId(),
      server.getCode(),
      server.getUtcOffsetHours(),
      server.getRegion(),
      server.getTags(),
      server.getSpokenLanguage(),
      server.getScenery(),
      server.getUpdateTime(),
      server.getRegisteredSince(),
      serverIsOnline,
      server.isDeleted());
  }
}
