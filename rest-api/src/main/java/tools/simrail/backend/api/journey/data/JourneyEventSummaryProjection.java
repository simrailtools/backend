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
import org.jspecify.annotations.Nullable;
import tools.simrail.backend.common.journey.JourneyTransportType;

/**
 * Summary projection for a journey event.
 */
public interface JourneyEventSummaryProjection {

  /**
   * Get the id of the journey this event is associated with.
   *
   * @return the id of the journey this event is associated with.
   */
  @NonNull
  UUID getJourneyId();

  /**
   * Get the time when this event was scheduled to happen.
   *
   * @return the time when this event was scheduled to happen.
   */
  @NonNull
  LocalDateTime getScheduledTime();

  /**
   * Get if this event is canceled.
   *
   * @return true if this event is canceled, false otherwise.
   */
  boolean isCancelled();

  /**
   * Get the index of this event along the journey route.
   *
   * @return the index of this event along the journey route.
   */
  int getEventIndex();

  /**
   * Get the transport category at the event.
   *
   * @return the transport category.
   */
  @NonNull
  String getTransportCategory();

  /**
   * Get the external transport category at the event.
   *
   * @return the external transport category, null if the same as the internal category.
   */
  @Nullable
  String getTransportCategoryExternal();

  /**
   * Get the transport number at the event.
   *
   * @return the transport number.
   */
  @NonNull
  String getTransportNumber();

  /**
   * Get the transport type at the event.
   *
   * @return the transport type.
   */
  @NonNull
  JourneyTransportType getTransportType();

  /**
   * Get the transport line at the event.
   *
   * @return the transport line, possibly {@code null}.
   */
  @Nullable
  String getTransportLine();

  /**
   * Get the transport label at the event.
   *
   * @return the transport label, possibly {@code null}.
   */
  @Nullable
  String getTransportLabel();

  /**
   * Get the id of the point that this event happens at.
   *
   * @return the id of the point that this event happens at.
   */
  @NonNull
  UUID getPointId();

  /**
   * Get if the point of the event is within the playable border.
   *
   * @return true if the point of the event is within the playable border, false otherwise.
   */
  boolean isPointPlayable();
}
