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
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import tools.simrail.backend.api.eventbus.cache.SitSnapshotCache;
import tools.simrail.backend.common.server.SimRailServerRepository;

@Service
public class SimRailServerTimeService {

  private final SitSnapshotCache snapshotCache;
  private final SimRailServerRepository serverRepository;

  @Autowired
  public SimRailServerTimeService(
    @Nonnull SitSnapshotCache snapshotCache,
    @Nonnull SimRailServerRepository serverRepository
  ) {
    this.snapshotCache = snapshotCache;
    this.serverRepository = serverRepository;
  }

  /**
   * Tries to parse the given server id and resolve a valid server from it. If that is possible, a pair of the parsed
   * server id and current server time is returned in all other cases, an empty optional is returned instead.
   *
   * @param serverId the id of the server to get the snapshot and time of.
   * @return a pair holding the parsed server id and time of the server with the given id.
   */
  @Nonnull
  @Cacheable(cacheNames = "server_cache", key = "'st_' + #serverId")
  public Optional<Pair<UUID, OffsetDateTime>> resolveServerTime(@Nonnull String serverId) {
    return this.snapshotCache.findCachedServer(serverId).map(serverSnapshot -> {
      var serverTime = this.calculateServerTime(serverSnapshot.getTimezoneId(), serverSnapshot.getUtcOffsetHours());
      return Pair.of(serverSnapshot.getServerId(), serverTime);
    }).or(() -> {
      var parsedServerId = UUID.fromString(serverId);
      return this.serverRepository.findById(parsedServerId).map(server -> {
        var serverTime = this.calculateServerTime(server.getTimezone(), server.getUtcOffsetHours());
        return Pair.of(server.getId(), serverTime);
      });
    });
  }

  /**
   * Calculates the current offset date time on a server based on the timezone identifier and utc offset hours.
   *
   * @param timezoneId     the id of the timezone on the server.
   * @param utcOffsetHours the server time offset from utc in hours.
   * @return the current time on the server based on the timezone id and utc offset hours.
   */
  private @Nonnull OffsetDateTime calculateServerTime(@Nonnull String timezoneId, int utcOffsetHours) {
    var serverTimezone = ZoneId.of(timezoneId);
    return ZonedDateTime.now(ZoneOffset.UTC)
      .plusHours(utcOffsetHours)
      .withZoneSameLocal(serverTimezone)
      .toOffsetDateTime();
  }
}
