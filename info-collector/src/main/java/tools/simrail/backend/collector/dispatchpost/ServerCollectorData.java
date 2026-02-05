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

package tools.simrail.backend.collector.dispatchpost;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import tools.simrail.backend.external.feign.FeignJsonResponseTuple;

/**
 * Data storage for requests sent to the SimRail api during dispatch post data collection.
 */
@Getter
final class ServerCollectorData {

  // mapping of the foreign dispatch post id to the data for post
  final Map<String, DispatchPostUpdateHolder> updateHoldersByForeignId = new ConcurrentHashMap<>();

  private String dispatchPostEtag;
  private Instant lastDatabaseUpdate;

  /**
   * Update the stored etag for the dispatch post endpoint.
   *
   * @param responseTuple the response tuple to extract the etag from.
   */
  public void updateDispatchPostEtag(@NonNull FeignJsonResponseTuple<?> responseTuple) {
    this.dispatchPostEtag = responseTuple.firstHeaderValue(HttpHeaders.ETAG).orElse(null);
  }

  /**
   * Get if the database should be updated for the server during the current collection. This method calls resets the
   * timer state if that is the case.
   *
   * @return true if the collected dispatch post data should be updated in the database, false otherwise.
   */
  public boolean shouldUpdateDatabase() {
    var now = Instant.now();
    var lastDbUpdate = this.lastDatabaseUpdate;
    var shouldUpdate = lastDbUpdate == null || Duration.between(lastDbUpdate, now).abs().toMinutes() >= 5;
    if (shouldUpdate) {
      this.lastDatabaseUpdate = now;
      return true;
    }

    return false;
  }
}
