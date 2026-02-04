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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import java.time.Duration;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.StructuredTaskScope;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.simrail.backend.api.shared.UserDto;
import tools.simrail.backend.api.shared.UserPlatform;
import tools.simrail.backend.api.user.loader.UserCacheLoader;

@Service
class SimRailUserService {

  private static final Duration CACHE_TTL = Duration.ofDays(1);
  private static final Duration RESOLVE_TIMEOUT = Duration.ofSeconds(15);

  private final Map<UserPlatform, LoadingCache<String, Optional<SimRailUserDto>>> userCacheByPlatform;

  @Autowired
  public SimRailUserService(@NonNull Collection<UserCacheLoader> userCacheLoaders) {
    this.userCacheByPlatform = new EnumMap<>(UserPlatform.class);
    for (var cacheLoader : userCacheLoaders) {
      var cache = Caffeine.newBuilder()
        .recordStats()
        .maximumSize(5000)
        .expireAfterWrite(CACHE_TTL)
        .build(cacheLoader);
      this.userCacheByPlatform.put(cacheLoader.targetPlatform(), cache);
    }
  }

  public @NonNull List<SimRailUserDto> findUserDetails(@NonNull Collection<UserDto> users) {
    try (var scope = StructuredTaskScope.open(
      StructuredTaskScope.Joiner.awaitAll(),
      config -> config.withTimeout(RESOLVE_TIMEOUT))) {
      // fork a new resolve task for each requested user
      var resolveTasks = users.stream().map(user -> {
        var userCache = this.userCacheByPlatform.get(user.platform());
        Objects.requireNonNull(userCache, "no cache for platform registered: " + user.platform());
        return scope.fork(() -> userCache.get(user.id()));
      }).toList();
      scope.join();

      // after all resolve tasks complete (or time out), return the users that were successfully resolved
      return resolveTasks.stream()
        .filter(task -> task.state() == StructuredTaskScope.Subtask.State.SUCCESS)
        .map(StructuredTaskScope.Subtask::get)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toList();
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt(); // reset interrupted state
      return List.of();
    }
  }
}
