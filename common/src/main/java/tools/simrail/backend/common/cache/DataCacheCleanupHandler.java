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

package tools.simrail.backend.common.cache;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Handler to clean stale nodes from caches after a given ttl.
 */
@Component
public class DataCacheCleanupHandler {

  // how long cache nodes should be kept after being marked for removal
  private static final long TTL_AFTER_REMOVE = TimeUnit.SECONDS.toNanos(30);

  private final Collection<DataCache<?>> knownCaches = ConcurrentHashMap.newKeySet();

  @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS)
  public void cleanupCaches() {
    var now = System.nanoTime();
    for (var cache : this.knownCaches) {
      var localCache = cache.localCache;
      for (var cacheNodeEntry : localCache.entrySet()) {
        var key = cacheNodeEntry.getKey();
        var cacheNode = cacheNodeEntry.getValue();
        var staleFor = now - cacheNode.lastWriteNanos;
        if (cacheNode.removed) {
          // node was marked as removed, check if it exceeded the time to remain in the cache after removal
          if (staleFor >= TTL_AFTER_REMOVE) {
            localCache.remove(key, cacheNode);
          }
          continue;
        }

        // check if the node exceeded the cache ttl and should be removed
        if (staleFor >= cache.ttlNanos) {
          cacheNode.markRemoved();
        }
      }
    }
  }

  /**
   * Registers the given cache for cleanup processing.
   *
   * @param cache the cache to register.
   */
  public void registerCache(@NonNull DataCache<?> cache) {
    this.knownCaches.add(cache);
  }
}
