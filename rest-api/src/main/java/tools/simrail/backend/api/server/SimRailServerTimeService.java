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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tools.simrail.backend.common.cache.DataCache;
import tools.simrail.backend.common.proto.EventBusProto;

@Service
public class SimRailServerTimeService {

  private final DataCache<EventBusProto.ServerUpdateFrame> serverDataCache;

  @Autowired
  public SimRailServerTimeService(
    @NonNull @Qualifier("server_data_cache") DataCache<EventBusProto.ServerUpdateFrame> serverDataCache
  ) {
    this.serverDataCache = serverDataCache;
  }

  /**
   * Tries to parse the given server id and resolve a valid server from it. If that is possible, a pair of the parsed
   * server id and current server time is returned in all other cases, an empty optional is returned instead.
   *
   * @param serverId the id of the server to get the snapshot and time of.
   * @return a pair holding the parsed server id and time of the server with the given id.
   */
  @NonNull
  public Optional<LocalDateTime> resolveServerTime(@NonNull String serverId) {
    var cachedData = this.serverDataCache.findByPrimaryKey(serverId);
    if (cachedData == null) {
      return Optional.empty();
    }

    var utcOffsetSeconds = cachedData.getServerData().getUtcOffsetSeconds();
    var currentServerTime = LocalDateTime.now(ZoneOffset.UTC)
      .plusSeconds(utcOffsetSeconds)
      .truncatedTo(ChronoUnit.SECONDS);
    return Optional.of(currentServerTime);
  }
}
