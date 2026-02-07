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

package tools.simrail.backend.common.cache;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Configures a size metric gauge for each known data cache.
 */
@Component
final class DataCacheMetricProcessor implements ApplicationListener<ApplicationReadyEvent> {

  private final MeterRegistry meterRegistry;

  @Autowired
  public DataCacheMetricProcessor(@NonNull MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
    var caches = event.getApplicationContext().getBeansOfType(DataCache.class);
    for (var dataCache : caches.values()) {
      Gauge.builder("data_cache_size_total", dataCache, DataCache::getLocalCacheSize)
        .tag("cache_name", dataCache.getName())
        .description("The total size of the data cache")
        .register(this.meterRegistry);
    }
  }
}
