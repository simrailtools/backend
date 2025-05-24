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

package tools.simrail.backend.api.user;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import tools.simrail.backend.external.steam.model.SteamUserSummary;

@Service
class SimRailUserService {

  private final Cache userCache;
  private final SteamUserFetchQueue userFetchQueue;
  private final SimRailUserDtoConverter userConverter;

  @Autowired
  public SimRailUserService(
    @Nonnull SteamUserFetchQueue userFetchQueue,
    @Nonnull SimRailUserDtoConverter userConverter,
    @Nonnull CacheManager cacheManager
  ) {
    this.userFetchQueue = userFetchQueue;
    this.userConverter = userConverter;
    this.userCache = cacheManager.getCache("user_cache");
  }

  /**
   * Resolves the user information for the given steam ids, either from cache or from the steam api.
   *
   * @param steamIds the steam ids of the users to resolve the information of.
   * @return all resolvable profiles of all the given steam ids.
   */
  public @Nonnull List<SimRailUserDto> findUsersBySteamIds(@Nonnull Collection<String> steamIds) {
    var users = new ArrayList<SimRailUserDto>();

    // resolve the ids of the users that are not yet cached
    var missingIds = new ArrayList<String>();
    for (var steamId : steamIds) {
      var cacheEntry = this.userCache.get(steamId);
      if (cacheEntry != null) {
        // a resolve attempt was already made
        var user = (SimRailUserDto) cacheEntry.get();
        if (user != null) {
          users.add(user);
        }
      } else {
        // no entry was already cached for the user, needs to be resolved
        missingIds.add(steamId);
      }
    }

    if (!missingIds.isEmpty()) {
      // queue the resolving of the missing non-cached steam users
      var fetchFutures = missingIds.stream()
        .map(this.userFetchQueue::queueUserFetch)
        .toArray(CompletableFuture[]::new);
      CompletableFuture.allOf(fetchFutures)
        .orTimeout(5, TimeUnit.SECONDS)
        .exceptionally(_ -> null)
        .join();

      // cache all users that were fetched successfully from steam, either
      // their converted user information or just that they do not exist
      for (var future : fetchFutures) {
        if (future.state() == Future.State.SUCCESS) {
          //noinspection unchecked
          var fetchResult = (SimRailUserFetchResult<SteamUserSummary>) future.resultNow();
          switch (fetchResult) {
            case SimRailUserFetchResult.Success(var steamUser) -> {
              var user = this.userConverter.apply(steamUser);
              this.userCache.put(user.id(), user);
              users.add(user);
            }
            case SimRailUserFetchResult.NotFound(var userId) -> this.userCache.put(userId, null);
            case null, default -> {
              // ignore - nothing we can do about this
            }
          }
        }
      }
    }

    return users;
  }
}
