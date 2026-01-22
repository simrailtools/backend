/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024-2026 Pasqual Koschmieder and contributors
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

package tools.simrail.backend.collector.configuration;

import java.time.Duration;
import org.jspecify.annotations.NonNull;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.simrail.backend.common.cache.DataCache;
import tools.simrail.backend.common.proto.CacheProto;

/**
 * Configures everything related to data caching.
 */
@Configuration
public class DataCacheConfiguration {

  @Bean(name = "journey_checksum_cache")
  public @NonNull DataCache<CacheProto.JourneyChecksumData> checksumDataDataCache(@NonNull RedissonClient redisson) {
    var dataCache = new DataCache<>(
      "journey_checksum_cache",
      Duration.ofDays(5),
      redisson,
      CacheProto.JourneyChecksumData.parser(),
      _ -> System.nanoTime(), // only written to by collector, version is not relevant
      CacheProto.JourneyChecksumData::getForeignRunId);
    dataCache.pullCacheFromStorage(); // restore state from storage
    return dataCache;
  }
}
