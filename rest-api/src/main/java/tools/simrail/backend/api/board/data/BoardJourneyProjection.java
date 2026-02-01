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

package tools.simrail.backend.api.board.data;

import java.time.LocalDateTime;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tools.simrail.backend.common.journey.JourneyStopType;
import tools.simrail.backend.common.journey.JourneyTimeType;
import tools.simrail.backend.common.journey.JourneyTransportType;

/**
 * Projection of a journey event including relevant info about the initially matched event to build a board entry.
 */
public interface BoardJourneyProjection {

  /**
   * Get the id of the journey.
   *
   * @return the id of the journey.
   */
  @NonNull
  UUID getJourneyId();

  /**
   * Get the id of the point.
   *
   * @return the id of the point.
   */
  @NonNull
  UUID getPointId();

  /**
   * Get the index of the event along the journey route.
   *
   * @return the index of the event along the journey route.
   */
  int getEventIndex();

  /**
   * Get if the event is canceled.
   *
   * @return if the event is canceled.
   */
  boolean isCancelled();

  /**
   * Get if the event is additional.
   *
   * @return if the event is additional.
   */
  boolean isAdditional();

  /**
   * Get the platform at which the journey is scheduled to stop.
   *
   * @return the platform at which the journey is scheduled to stop.
   */
  @Nullable
  String getScheduledPlatform();

  /**
   * Get the id of the initially matched event.
   *
   * @return the id of the initially matched event.
   */
  @NonNull
  UUID getInitialEventId();

  /**
   * Get if the initially matched event is additional.
   *
   * @return if the initially matched event is additional.
   */
  boolean isInitialAdditional();

  /**
   * Get if the initially matched event is canceled.
   *
   * @return if the initially matched event is canceled.
   */
  boolean isInitialCancelled();

  /**
   * Get the scheduled time of the initially matched event.
   *
   * @return the scheduled time of the initially matched event.
   */
  @NonNull
  LocalDateTime getInitialScheduledTime();

  /**
   * Get the realtime time of the initially matched event.
   *
   * @return the realtime time of the initially matched event.
   */
  @NonNull
  LocalDateTime getInitialRealtimeTime();

  /**
   * Get the realtime time type of the initially matched event.
   *
   * @return the realtime time type of the initially matched event.
   */
  @NonNull
  JourneyTimeType getInitialRealtimeTimeType();

  /**
   * Get the scheduled platform of the event, null if no passenger stop is scheduled.
   *
   * @return the scheduled platform of the event, null if no passenger stop is scheduled.
   */
  @Nullable
  Integer getInitialScheduledPlatform();

  /**
   * Get the scheduled track of the event, null if no passenger stop is scheduled.
   *
   * @return the scheduled track of the event, null if no passenger stop is scheduled.
   */
  @Nullable
  Integer getInitialScheduledTrack();

  /**
   * Get the realtime platform of the event, null if no stop is scheduled, or it didn't happen yet.
   *
   * @return the realtime platform of the event, null if no stop is scheduled, or it didn't happen yet.
   */
  @Nullable
  Integer getInitialRealtimePlatform();

  /**
   * Get the realtime track of the event, null if no stop is scheduled, or it didn't happen yet.
   *
   * @return the realtime track of the event, null if no stop is scheduled, or it didn't happen yet.
   */
  @Nullable
  Integer getInitialRealtimeTrack();

  /**
   * Get the stop type of the initially matched event.
   *
   * @return the stop type of the initially matched event.
   */
  @NonNull
  JourneyStopType getInitialStopType();

  /**
   * Get the transport type of the initially matched event.
   *
   * @return the transport type of the initially matched event.
   */
  @NonNull
  JourneyTransportType getInitialTransportType();

  /**
   * Get the transport category of the initially matched event.
   *
   * @return the transport category of the initially matched event.
   */
  @NonNull
  String getInitialTransportCategory();

  /**
   * Get the transport number of the initially matched event.
   *
   * @return the transport number of the initially matched event.
   */
  @NonNull
  String getInitialTransportNumber();

  /**
   * Get the transport line of the initially matched event.
   *
   * @return the transport line of the initially matched event.
   */
  @Nullable
  String getInitialTransportLine();

  /**
   * Get the transport label of the initially matched event.
   *
   * @return the transport label of the initially matched event.
   */
  @Nullable
  String getInitialTransportLabel();

  /**
   * Get the maximum speed at the initially matched event.
   *
   * @return the maximum speed at the initially matched event.
   */
  int getInitialTransportMaxSpeed();
}
