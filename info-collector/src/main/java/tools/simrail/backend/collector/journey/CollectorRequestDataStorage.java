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

package tools.simrail.backend.collector.journey;

import jakarta.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import lombok.Getter;
import org.springframework.http.HttpHeaders;
import tools.simrail.backend.external.feign.FeignJsonResponseTuple;

/**
 * Data storage for requests sent to the SimRail api during journey data collection.
 */
@Getter
final class CollectorRequestDataStorage {

  private static final int TIME_BETWEEN_TRAIN_COLLECTIONS = 7;

  private String trainsEtag;
  private String trainPositionsEtag;
  private Instant lastTrainsCollection;

  /**
   * Update the stored etag for the trains' endpoint.
   *
   * @param responseTuple the response tuple to extract the etag from.
   */
  public void updateTrainsEtag(@Nonnull FeignJsonResponseTuple<?> responseTuple) {
    this.trainsEtag = responseTuple.firstHeaderValue(HttpHeaders.ETAG).orElse(null);
  }

  /**
   * Update the stored etag for the train positions endpoint.
   *
   * @param responseTuple the response tuple to extract the etag from.
   */
  public void updateTrainPositionsEtag(@Nonnull FeignJsonResponseTuple<?> responseTuple) {
    this.trainPositionsEtag = responseTuple.firstHeaderValue(HttpHeaders.ETAG).orElse(null);
  }

  /**
   * Checks if train data should be collected.
   *
   * @param now the current time.
   * @return true if train data should be collected, false if not.
   */
  public boolean shouldDoTrainsCollection(@Nonnull Instant now) {
    if (this.lastTrainsCollection == null) {
      return true;
    } else {
      var secondsSinceLastCollect = Duration.between(this.lastTrainsCollection, now).toSeconds();
      return secondsSinceLastCollect >= TIME_BETWEEN_TRAIN_COLLECTIONS;
    }
  }

  /**
   * Updates the timestamp when the last train collection was done.
   *
   * @param now the timestamp when the train collection was executed.
   */
  public void updateLastTrainCollect(@Nonnull Instant now) {
    this.lastTrainsCollection = now;
  }
}
