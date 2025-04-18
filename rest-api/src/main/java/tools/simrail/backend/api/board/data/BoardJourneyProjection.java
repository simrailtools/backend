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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.UUID;

public interface BoardJourneyProjection {

  /**
   * @return
   */
  @Nonnull
  UUID getJourneyId();

  @Nonnull
  UUID getPointId();

  @Nonnull
  String getPointName();

  int getEventIndex();

  boolean isCancelled();

  boolean isAdditional();

  @Nonnull
  UUID getInitialEventId();

  boolean isInitialAdditional();

  boolean isInitialCancelled();

  @Nonnull
  OffsetDateTime getInitialScheduledTime();

  @Nonnull
  OffsetDateTime getInitialRealtimeTime();

  short getInitialRealtimeTimeType();

  @Nullable
  Integer getInitialScheduledPlatform();

  @Nullable
  Integer getInitialScheduledTrack();

  @Nullable
  Integer getInitialRealtimePlatform();

  @Nullable
  Integer getInitialRealtimeTrack();

  short getInitialTransportType();

  @Nonnull
  String getInitialTransportCategory();

  @Nonnull
  String getInitialTransportNumber();

  @Nullable
  String getInitialTransportLine();

  @Nullable
  String getInitialTransportLabel();

  int getInitialTransportMaxSpeed();
}
