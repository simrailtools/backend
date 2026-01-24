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

package tools.simrail.backend.common.event;

import org.jspecify.annotations.NonNull;

/**
 * Factory to create message subjects for event data frames.
 */
public final class EventSubjectFactory {

  /**
   * Delimiting char for message components.
   */
  private static final char DELIM = '.';
  /**
   * Message prefix for all sit-events messages.
   */
  private static final String ALL_FRAMES_PREFIX = "sit-events";

  private EventSubjectFactory() {
    throw new UnsupportedOperationException();
  }

  /**
   * Constructs a subject for publishing/listening to journey updates.
   *
   * @param serverId  the id of the server the journey runs on.
   * @param journeyId the id of the journey.
   * @return a subject to publish/listen to journey updates.
   */
  public static @NonNull String createJourneyUpdateSubjectV1(@NonNull String serverId, @NonNull String journeyId) {
    return ALL_FRAMES_PREFIX + DELIM + "journey-updates" + DELIM + "v1" + DELIM + serverId + DELIM + journeyId;
  }

  /**
   * Constructs a subject for publishing/listening to journey removals.
   *
   * @param serverId  the id of the server the journey runs on.
   * @param journeyId the id of the journey.
   * @return a subject to publish/listen to journey removals.
   */
  public static @NonNull String createJourneyRemoveSubjectV1(@NonNull String serverId, @NonNull String journeyId) {
    return ALL_FRAMES_PREFIX + DELIM + "journey-removals" + DELIM + "v1" + DELIM + serverId + DELIM + journeyId;
  }

  /**
   * Constructs a subject for publishing/listening to server updates.
   *
   * @param serverId the id of the server.
   * @return a subject to publish/listen to server updates.
   */
  public static @NonNull String createServerUpdateSubjectV1(@NonNull String serverId) {
    return ALL_FRAMES_PREFIX + DELIM + "server-updates" + DELIM + "v1" + DELIM + serverId;
  }

  /**
   * Constructs a subject for publishing/listening to server removals.
   *
   * @param serverId the id of the server.
   * @return a subject to publish/listen to server removals.
   */
  public static @NonNull String createServerRemoveSubjectV1(@NonNull String serverId) {
    return ALL_FRAMES_PREFIX + DELIM + "server-removals" + DELIM + "v1" + DELIM + serverId;
  }

  public static @NonNull String createDispatchPostUpdateSubjectV1(@NonNull String serverId, @NonNull String postId) {
    return ALL_FRAMES_PREFIX + DELIM + "dispatch-post-updates" + DELIM + "v1" + DELIM + serverId + DELIM + postId;
  }

  public static @NonNull String createDispatchPostRemoveSubjectV1(@NonNull String serverId, @NonNull String postId) {
    return ALL_FRAMES_PREFIX + DELIM + "dispatch-post-removals" + DELIM + "v1" + DELIM + serverId + DELIM + postId;
  }
}
