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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import tools.simrail.backend.api.eventbus.cache.SitSnapshotCache;
import tools.simrail.backend.api.eventbus.dto.EventbusServerSnapshotDto;

@Service
public final class SimRailServerTimeService {

  private final SitSnapshotCache snapshotCache;

  @Autowired
  public SimRailServerTimeService(@Nonnull SitSnapshotCache snapshotCache) {
    this.snapshotCache = snapshotCache;
  }

  /**
   * Get the cached server snapshot and current server time for the server with the given id.
   *
   * @param serverId the id of the server to get the snapshot and time of.
   * @return a pair holding the server snapshot and time of the server with the given id.
   */
  @Nonnull
  public Optional<Pair<EventbusServerSnapshotDto, OffsetDateTime>> findServerAndTime(@Nonnull String serverId) {
    return this.snapshotCache.findCachedServer(serverId).map(serverSnapshot -> {
      var serverTimezone = ZoneId.of(serverSnapshot.getTimezoneId());
      var serverTime = ZonedDateTime.now(ZoneOffset.UTC)
        .plusHours(serverSnapshot.getUtcOffsetHours())
        .withZoneSameLocal(serverTimezone)
        .toOffsetDateTime();
      return Pair.of(serverSnapshot, serverTime);
    });
  }
}
