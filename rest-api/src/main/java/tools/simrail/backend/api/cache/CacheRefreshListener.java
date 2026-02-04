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

package tools.simrail.backend.api.cache;

import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.simrail.backend.common.cache.DataCache;

@Component
final class CacheRefreshListener implements ConnectionListener {

  private final AtomicBoolean refreshRunning;
  private final Collection<DataCache<?>> dataCaches;

  @Autowired
  CacheRefreshListener(@NonNull Collection<DataCache<?>> dataCaches) {
    this.dataCaches = dataCaches;
    this.refreshRunning = new AtomicBoolean(false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void connectionEvent(@NonNull Connection connection, @NonNull Events type) {
    throw new AssertionError("deprecated method must not be called anymore!");
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void connectionEvent(
    @NonNull Connection conn,
    @NonNull Events type,
    @NonNull Long time,
    @Nullable String uriDetails
  ) {
    if (type == Events.RESUBSCRIBED) {
      this.triggerCacheRefresh();
    }
  }

  /**
   * Triggers a cache refresh unless a refresh is currently in flight.
   */
  private void triggerCacheRefresh() {
    if (!this.refreshRunning.compareAndSet(false, true)) {
      return;
    }

    Thread.ofVirtual().start(() -> {
      for (var cache : this.dataCaches) {
        cache.pullCacheFromStorage();
      }

      this.refreshRunning.set(false);
    });
  }
}
