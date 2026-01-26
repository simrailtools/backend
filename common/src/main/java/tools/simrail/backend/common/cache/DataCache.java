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

import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.stream.Gatherers;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.api.options.KeysScanOptions;
import org.redisson.client.codec.ByteArrayCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache for data, backed by a storage supported by redisson. Values in this cache are versioned, only newer versions
 * are actually flushed to the underlying storage. Reads are always based on the local cache and never hit the backing
 * storage.
 *
 * @param <T> the type of the elements being cached.
 */
public final class DataCache<T extends MessageLite> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DataCache.class);

  private static final char KEY_TYPE_PRIMARY = 'P';
  private static final char KEY_TYPE_SECONDARY = 'S';

  // exposed for cache cleanup
  final long ttlNanos;
  final Map<String, DataCacheNode<T>> localCache;

  private final String name;
  private final Duration ttl;
  private final RedissonClient redisson;
  private final Parser<T> messageParser;

  // prefix used for all keys
  private final String primaryKeyPrefix;
  private final String secondaryKeyPrefix;

  // functions to extract data from the type being wrapped
  private final ToLongFunction<T> versionExtractor;
  private final Function<T, String> primaryKeyExtractor;
  private final Function<T, String> secondaryKeyExtractor;

  public DataCache(
    @NonNull String name,
    @NonNull Duration ttl,
    @NonNull RedissonClient redisson,
    @NonNull Parser<T> messageParser,
    @NonNull ToLongFunction<T> versionExtractor,
    @NonNull Function<T, String> primaryKeyExtractor
  ) {
    this(name, ttl, redisson, messageParser, versionExtractor, primaryKeyExtractor, null);
  }

  public DataCache(
    @NonNull String name,
    @NonNull Duration ttl,
    @NonNull RedissonClient redisson,
    @NonNull Parser<T> messageParser,
    @NonNull ToLongFunction<T> versionExtractor,
    @NonNull Function<T, String> primaryKeyExtractor,
    @Nullable Function<T, String> secondaryKeyExtractor
  ) {
    this.name = name;
    this.ttl = ttl;
    this.redisson = redisson;
    this.messageParser = messageParser;
    this.versionExtractor = versionExtractor;
    this.primaryKeyExtractor = primaryKeyExtractor;
    this.secondaryKeyExtractor = secondaryKeyExtractor;

    this.ttlNanos = ttl.toNanos();
    this.localCache = new ConcurrentHashMap<>();

    this.primaryKeyPrefix = this.generateFullKey("", KEY_TYPE_PRIMARY); // 'this' escape is safe
    this.secondaryKeyPrefix = this.generateFullKey("", KEY_TYPE_SECONDARY);
  }

  /**
   * Finds a value that is stored in this cache by the given primary key.
   *
   * @param key the primary key of the value to get.
   * @return the value associated with the given primary key, {@code null} if no value is associated with the key.
   */
  public @Nullable T findByPrimaryKey(@NonNull String key) {
    return this.findByKey(key, KEY_TYPE_PRIMARY);
  }

  /**
   * Finds a value that is stored in this cache by the given secondary key.
   *
   * @param key the secondary key of the value to get.
   * @return the value associated with the given secondary key, {@code null} if no value is associated with the key.
   */
  public @Nullable T findBySecondaryKey(@NonNull String key) {
    return this.findByKey(key, KEY_TYPE_SECONDARY);
  }

  /**
   * Finds a value that is stored in this cache by the given key and key type.
   *
   * @param key     the key of the value to get.
   * @param keyType the type of the key to use for retrieving the value.
   * @return the value associated with the given key, {@code null} if no value is associated with the key.
   */
  private @Nullable T findByKey(@NonNull String key, char keyType) {
    var cacheKey = this.generateFullKey(key, keyType);
    var localNode = this.localCache.get(cacheKey);
    return localNode != null && !localNode.removed ? localNode.value : null;
  }

  /**
   * Creates a snapshot of the values currently in the cache. The returned collection is not updated when a new value is
   * added or an old value is removed from the cache. A re-computation of the collection is required for that.
   *
   * @return a snapshot of the elements that are currently locally in this cache.
   */
  public @NonNull Collection<T> cachedValuesSnapshot() {
    // this whole thing is technically wrong because the 'removed' state can be set
    // while the mapping is in progress. but we don't care about this small race,
    // it's completely fine to have this, each call should re-compute the list anyway
    return this.localCache.entrySet()
      .stream()
      .filter(entry -> entry.getKey().startsWith(this.primaryKeyPrefix))
      .map(Map.Entry::getValue)
      .filter(cacheNode -> !cacheNode.removed)
      .map(cacheNode -> cacheNode.value)
      .toList();
  }

  /**
   * Updates the value that is locally stored in this cache. Note that the value is not flushed to the underlying
   * storage when using this method, use {@link #setCachedValue(MessageLite)} instead. Note that the given value is
   * returned from this method if it was stored into the cache initially.
   *
   * @param value the value to potentially write into the local store.
   * @return the previous value replaced by the new one, {@code null} if no change was made to the cache.
   */
  public @Nullable T updateLocalValue(@NonNull T value) {
    var primaryKey = this.primaryKeyExtractor.apply(value);
    var primaryCacheKey = this.generateFullKey(primaryKey, KEY_TYPE_PRIMARY);
    return this.updateLocalValue0(primaryCacheKey, value);
  }

  /**
   * Internal method to update the local cache value, taking an additional primary cache key argument. This method is
   * just a performance optimization to prevent constructing the cache key twice in {@code setCachedValue()}.
   *
   * @param primaryCacheKey the primary cache key of the value being updated.
   * @param value           the value to potentially write into the local store.
   * @return the previous value replaced by the new one, {@code null} if no change was made to the cache.
   */
  private @Nullable T updateLocalValue0(@NonNull String primaryCacheKey, @NonNull T value) {
    var localNode = this.localCache.get(primaryCacheKey);
    return switch (localNode) {
      case DataCacheNode<T> node -> node.swapToNewVersion(value);
      case null -> {
        var newNode = new DataCacheNode<>(this.versionExtractor, value);
        var currentNode = this.localCache.putIfAbsent(primaryCacheKey, newNode);
        if (currentNode != null) {
          // some other thread computed the cache node concurrently, just retry
          yield this.updateLocalValue0(primaryCacheKey, value);
        }

        // new node was written to the local cache successfully, also store
        // the value to redis. we need to also associate the node with the
        // secondary key, if any is required. this is safe because we secondary
        // key is never used to write to the cache, only for reading, so there
        // can't be any concurrent access to the key
        if (this.secondaryKeyExtractor != null) {
          var secondaryKey = this.secondaryKeyExtractor.apply(value);
          var secondaryCacheKey = this.generateFullKey(secondaryKey, KEY_TYPE_SECONDARY);
          this.localCache.put(secondaryCacheKey, newNode);
          if (newNode.removed) {
            // well, some concurrent remove call happened?
            this.localCache.remove(secondaryCacheKey, newNode);
          }
        }

        // null is used to indicate that the previous value was not updated, so we
        // yield the provided value to indicate the initial write to the cache
        yield value;
      }
    };
  }

  /**
   * Updates or sets the cached value, also flushing the change to the underlying store if it changed.
   *
   * @param value the new value to write into the cache.
   * @return true if the value changed and was updated, false otherwise.
   */
  public boolean setCachedValue(@NonNull T value) {
    var primaryKey = this.primaryKeyExtractor.apply(value);
    var primaryCacheKey = this.generateFullKey(primaryKey, KEY_TYPE_PRIMARY);
    var previousValue = this.updateLocalValue0(primaryCacheKey, value);
    if (previousValue == null) {
      // nothing changed, noting to flush to the underlying store
      return false;
    }

    // flush the change to the underlying cache. this is done async in a "fire and forget"
    // manner, but the expectation is that the local cache is correct and sync, the underlying
    // store is a "just in case something crashes" thing, 100% data correctness is not required
    var bucket = this.getBucket(primaryCacheKey);
    bucket.setAsync(value.toByteArray(), this.ttl);
    return true;
  }

  /**
   * Removes the value associated with the given primary key from this cache, if not already done.
   *
   * @param key the primary key of the value to remove.
   * @return true if the value was removed from the cache, false otherwise.
   */
  public boolean removeByPrimaryKey(@NonNull String key) {
    var cacheKey = this.generateFullKey(key, KEY_TYPE_PRIMARY);
    var cacheNode = this.localCache.get(cacheKey);
    return cacheNode != null && cacheNode.markRemoved();
  }

  /**
   *
   * @param keys
   * @return
   */
  public @NonNull Stream<T> findBySecondaryKeyNotIn(@NonNull Set<String> keys) {
    return this.localCache.entrySet().stream()
      .filter(entry -> entry.getKey().startsWith(this.secondaryKeyPrefix))
      .filter(entry -> {
        var rawSecondaryKey = entry.getKey().substring(this.secondaryKeyPrefix.length());
        return !keys.contains(rawSecondaryKey);
      })
      .map(Map.Entry::getValue)
      .filter(node -> !node.removed)
      .map(node -> node.value);
  }

  /**
   * Pulls the full cache from the underlying storage. Only updates the locally cached values that were updated on the
   * remote store but not in the local cache.
   */
  public void pullCacheFromStorage() {
    // remove everything from the local store when this method is called, only new updates
    // and the data pulled from the storage should be in here after invocation
    this.localCache.clear();

    var keySearchPattern = this.generateFullKey("*", KEY_TYPE_PRIMARY);
    var keyScanOptions = KeysScanOptions.defaults().pattern(keySearchPattern).chunkSize(100);
    try (var storedKeysStream = this.redisson.getKeys().getKeysStream(keyScanOptions)) {
      var bucketOps = this.redisson.getBuckets(ByteArrayCodec.INSTANCE);
      storedKeysStream
        .gather(Gatherers.windowFixed(100))
        .map(keys -> {
          var keysArray = keys.toArray(String[]::new);
          return bucketOps.<byte[]>get(keysArray);
        })
        .flatMap(map -> map.entrySet().stream())
        .forEach(entry -> {
          try {
            var value = this.messageParser.parseFrom(entry.getValue());
            this.updateLocalValue(value);
          } catch (Exception exception) {
            LOGGER.warn("Failed to parse stored cache value (key: {}): {}", entry.getKey(), exception.getMessage());
          }
        });
    }
  }

  /**
   * Get the bucket for the underlying store to store cached data in. This operation is very cheap.
   *
   * @param key the key of the storage bucket to get.
   * @return a bucket to store cached values in the underlying storage.
   */
  private @NonNull RBucket<byte[]> getBucket(@NonNull String key) {
    return this.redisson.getBucket(key, ByteArrayCodec.INSTANCE);
  }

  /**
   * Generates a unique key for this cache based on the given key and key type.
   *
   * @param key     the key to generate.
   * @param keyType the type of key to generate.
   * @return a unique key for this cache based on the given key and key type.
   */
  private @NonNull String generateFullKey(@NonNull String key, char keyType) {
    return this.name + ':' + keyType + ':' + key;
  }
}
