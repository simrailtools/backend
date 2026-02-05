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

package tools.simrail.backend.api.journey.data;

import java.time.LocalDateTime;
import java.util.UUID;
import org.jspecify.annotations.NonNull;

/**
 * Projection of a journey entity including some extra fields for a single event.
 */
public interface JourneyWithEventSummaryProjection extends JourneySummaryProjection {

  /**
   * Get the id of the point at the additionally selected journey event.
   *
   * @return the id of the point at the additionally selected journey event.
   */
  @NonNull
  UUID getEventPointId();

  /**
   * Get if the point of the additionally selected journey event is within the playable border.
   *
   * @return true if the point of the event is within the playable border, false otherwise.
   */
  boolean isEventPointPlayable();

  /**
   * Get the time when the additionally selected event is scheduled to happen.
   *
   * @return the time when the additionally selected event is scheduled to happen.
   */
  @NonNull
  LocalDateTime getEventScheduledTime();

  /**
   * Get if the additionally selected event is marked as canceled.
   *
   * @return true if the additionally selected event is marked as canceled, false otherwise.
   */
  boolean isEventCancelled();
}
