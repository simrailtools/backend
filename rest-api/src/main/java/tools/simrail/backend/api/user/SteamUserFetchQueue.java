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
import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.simrail.backend.external.steam.SteamApiClient;
import tools.simrail.backend.external.steam.model.SteamUserSummary;

@Component
class SteamUserFetchQueue {

  private static final int MAX_FETCH_TRIES = 5;
  private static final Pattern RESPONSE_STATUS_PATTERN = Pattern.compile(".*status=([1-5]\\d{2});.*");
  private static final SimRailUserFetchResult.Failure<SteamUserSummary> FETCH_FAILURE = new SimRailUserFetchResult.Failure<>();

  private static final Logger LOGGER = LoggerFactory.getLogger(SteamUserFetchQueue.class);

  private final SteamApiClient steamApiClient;
  private final Lock fetchQueueLock = new ReentrantLock();
  private final Map<String, UserFetchRequest> userFetchQueue = new LinkedHashMap<>();

  @Autowired
  public SteamUserFetchQueue(@Nonnull SteamApiClient steamApiClient) {
    this.steamApiClient = steamApiClient;
  }

  /**
   * Fetches all queued user requests in a single batch, every 500ms between invocations.
   */
  @Scheduled(fixedDelay = 500)
  public void fetchUsers() {
    var fetchBatch = this.getNextUserFetchBatch();
    if (fetchBatch == null) {
      return;
    }

    try {
      // fetch users from the batch and complete their respective futures
      var idsToFetch = List.copyOf(fetchBatch.keySet());
      var fetchResponse = this.steamApiClient.getPlayerSummaries(idsToFetch);
      var userSummaries = fetchResponse.response().players();
      for (var userSummary : userSummaries) {
        var fetchRequest = fetchBatch.remove(userSummary.getId());
        var fetchResult = new SimRailUserFetchResult.Success<>(userSummary);
        fetchRequest.future.complete(fetchResult);
      }

      // mark the remaining users that weren't included in the response as not found
      for (var remaining : fetchBatch.values()) {
        var fetchResult = new SimRailUserFetchResult.NotFound<SteamUserSummary>(remaining.userId);
        remaining.future.complete(fetchResult);
      }
    } catch (Exception exception) {
      LOGGER.warn("Exception while fetching steam users", exception);

      // remove and complete all requests that have been retried too many times
      for (var entry : fetchBatch.entrySet()) {
        var request = entry.getValue();
        request.fetchTries++;
        if (request.fetchTries > MAX_FETCH_TRIES) {
          fetchBatch.remove(entry.getKey());
          request.future.complete(FETCH_FAILURE);
        }
      }

      // retry all remaining requests when receiving a 429 (too many requests)
      // or internal server error (>=500) as the http response code
      var exceptionMessage = exception.getMessage();
      if (exceptionMessage != null) {
        var matcher = RESPONSE_STATUS_PATTERN.matcher(exceptionMessage);
        if (matcher.matches()) {
          var statusCode = Integer.parseInt(matcher.group(1));
          if (statusCode == 429 || statusCode >= 500) {
            this.requeueUserFetchBatch(fetchBatch);
            return;
          }
        }
      }

      // complete the remaining entries in the batch with a failure result
      for (var remaining : fetchBatch.values()) {
        remaining.future.complete(FETCH_FAILURE);
      }
    }
  }

  /**
   * Get the next batch of users to fetch from the queue (up to 100 in a single batch).
   */
  private @Nullable Map<String, UserFetchRequest> getNextUserFetchBatch() {
    this.fetchQueueLock.lock();
    try {
      // if there are no requests pending, just return null
      var fetchQueue = this.userFetchQueue;
      if (fetchQueue.isEmpty()) {
        return null;
      }

      // if there are 100 or less user requests queued we can just return the map
      // of users to fetch as we can execute these all in a single request
      if (fetchQueue.size() <= 100) {
        var fetchBatch = new HashMap<>(fetchQueue);
        fetchQueue.clear();
        return fetchBatch;
      }

      // copy the first 100 entries from the map into a new map (next batch to fetch)
      // while only leaving the remaining entries in the current fetch queue
      var fetchQueueItr = fetchQueue.entrySet().iterator();
      Map<String, UserFetchRequest> fetchBatch = LinkedHashMap.newLinkedHashMap(100);
      for (var index = 0; index < 100; index++) {
        var nextEntry = fetchQueueItr.next();
        fetchQueueItr.remove();
        fetchBatch.put(nextEntry.getKey(), nextEntry.getValue());
      }

      return fetchBatch;
    } finally {
      this.fetchQueueLock.unlock();
    }
  }

  /**
   * Re-queues the fetch of the given user fetch requests.
   */
  private void requeueUserFetchBatch(@Nonnull Map<String, UserFetchRequest> fetchBatch) {
    this.fetchQueueLock.lock();
    try {
      this.userFetchQueue.putAll(fetchBatch);
    } finally {
      this.fetchQueueLock.unlock();
    }
  }

  /**
   * Queues the fetch of a single user, returning a future that will be completed with either of the tree states:
   * <ol>
   *   <li>{@code null} to indicate that the fetch was tried to many times and timed out.
   *   <li>{@code (userId, null)} to indicate that the fetch was successful but the user doesn't exist.
   *   <li>{@code (userId, userSummary)} when the fetch of the user data was successful.
   * </ol>
   */
  public @Nonnull CompletableFuture<SimRailUserFetchResult<SteamUserSummary>> queueUserFetch(@Nonnull String userId) {
    this.fetchQueueLock.lock();
    try {
      var fetchRequest = this.userFetchQueue.computeIfAbsent(userId, uid -> {
        var resultFuture = new CompletableFuture<SimRailUserFetchResult<SteamUserSummary>>();
        return new UserFetchRequest(uid, resultFuture);
      });
      return fetchRequest.future;
    } finally {
      this.fetchQueueLock.unlock();
    }
  }

  /**
   * A mapping for a single user fetch request. Contains the id of the user to fetch as well as the future to which the
   * fetch result should be submitted. The fetch tries are recorded as well to stop the fetch requests after a few
   * retries.
   */
  @RequiredArgsConstructor
  private static final class UserFetchRequest {

    private final String userId;
    private final CompletableFuture<SimRailUserFetchResult<SteamUserSummary>> future;

    private int fetchTries;
  }
}
