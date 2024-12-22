/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024 Pasqual Koschmieder and contributors
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

package tools.simrail.backend.api.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.Nonnull;
import java.util.concurrent.TimeUnit;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
class CacheConfiguration {

  /**
   * Cache for server data (data expires after 10 seconds in the cache).
   */
  @Bean
  public @Nonnull Cache serverCache() {
    return new CaffeineCache(
      "server_cache",
      Caffeine.newBuilder()
        .expireAfterWrite(10, TimeUnit.SECONDS)
        .build());
  }

  /**
   * Cache for journey data (data expires after 5 seconds in the cache).
   */
  @Bean
  public @Nonnull Cache journeyCache() {
    return new CaffeineCache(
      "journey_cache",
      Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.SECONDS)
        .build());
  }

  /**
   * Cache for journey search data (data expires after 5 minutes in the cache).
   */
  @Bean
  public @Nonnull Cache journeySearchCache() {
    return new CaffeineCache(
      "journey_search_cache",
      Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build());
  }

  /**
   * Cache for points, once resolved they can reside in the cache longer as the point data can only update with an
   * application restart which clears the cache.
   */
  @Bean
  public @Nonnull Cache pointCache() {
    return new CaffeineCache(
      "point_cache",
      Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build());
  }

  /**
   * Cache for dispatch post information (expires after 5 seconds to always provide realtime information).
   */
  @Bean
  public @Nonnull Cache dispatchPostCache() {
    return new CaffeineCache(
      "dispatch_post_cache",
      Caffeine.newBuilder()
        .expireAfterWrite(5, TimeUnit.SECONDS)
        .build());
  }

  /**
   * Cache for vehicle sequences (expires after 2 minutes).
   */
  @Bean
  public @Nonnull Cache vehicleSequenceCache() {
    return new CaffeineCache(
      "vehicle_sequence_cache",
      Caffeine.newBuilder()
        .expireAfterWrite(2, TimeUnit.MINUTES)
        .build());
  }
}
