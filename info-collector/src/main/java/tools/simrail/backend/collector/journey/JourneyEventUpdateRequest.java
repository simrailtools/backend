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

package tools.simrail.backend.collector.journey;

import java.time.LocalDateTime;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import tools.simrail.backend.collector.server.SimRailServerDescriptor;
import tools.simrail.backend.common.point.SimRailPoint;
import tools.simrail.backend.common.proto.EventBusProto;

/**
 * Request passed to the realtime event updater for a journey.
 */
sealed interface JourneyEventUpdateRequest {

  /**
   * Constructs a new update request to handle the removal of a journey.
   *
   * @param journeyId the id of the journey that was removed.
   * @param server    the server that the journey is on.
   * @return a new update request to update the events of the journey because the journey was removed.
   */
  static @NonNull JourneyEventUpdateRequest forRemoval(
    @NonNull UUID journeyId,
    @NonNull SimRailServerDescriptor server
  ) {
    var serverTime = server.currentTime();
    return new ForRemoval(journeyId, serverTime);
  }

  /**
   * Creates a new request to update the events of a journey because it arrived or departed from a station.
   *
   * @param journeyId    the id of the journey that arrived / departed from a point.
   * @param server       the server that the journey is on.
   * @param prevPointId  the id of the previous point of the journey was at, possibly {@code null}.
   * @param currentPoint the id of the current point where the journey is at, possibly {@code null}.
   * @param nextSignal   the signal in front of the journey, possibly {@code null}.
   * @return a new update request to update the events of the journey because of a point change.
   * @throws IllegalArgumentException if the given previous and current point are null.
   */
  static @NonNull JourneyEventUpdateRequest forEventUpdate(
    @NonNull UUID journeyId,
    @NonNull SimRailServerDescriptor server,
    @Nullable UUID prevPointId,
    @Nullable SimRailPoint currentPoint,
    EventBusProto.@Nullable SignalInfo nextSignal
  ) {
    var serverTime = server.currentTime();
    return new ForPointChange(journeyId, serverTime, prevPointId, currentPoint, nextSignal);
  }

  /**
   * Creates a new request to update the event of the journey at the given current point with the realtime platform
   * info. If the journey has no passenger stop at the point, the request is ignored.
   *
   * @param journeyId    the id of the journey whose next signal updated.
   * @param server       the server that the journey is on.
   * @param currentPoint the point where the journey is currently at.
   * @param nextSignalId the id of the signal in front of the journey.
   * @return a new request to update the realtime passenger info for a journey based on the current point and signal id.
   */
  static @NonNull JourneyEventUpdateRequest forSignalUpdate(
    @NonNull UUID journeyId,
    @NonNull SimRailServerDescriptor server,
    @NonNull SimRailPoint currentPoint,
    @NonNull String nextSignalId
  ) {
    var serverTime = server.currentTime();
    return new ForSignalUpdate(journeyId, serverTime, currentPoint, nextSignalId);
  }

  /**
   * Get the id of the journey whose event should get updated.
   *
   * @return the id of the journey whose event should get updated.
   */
  @NonNull UUID journeyId();

  /**
   * Get the time on the server at the time the request was created.
   *
   * @return the time on the server at the time the request was created.
   */
  @NonNull LocalDateTime serverTime();

  /**
   * Request to update the events of a journey because the point where the journey is currently at changed.
   *
   * @param journeyId    the id of the journey that should be updated.
   * @param serverTime   the time on the associated server when the request was created.
   * @param prevPointId  the previous point the journey was at, possibly {@code null}.
   * @param currentPoint the current point where the journey is at, possibly {@code null}.
   * @param nextSignal   the signal in front of the journey, possibly {@code null}.
   */
  record ForPointChange(
    @NonNull UUID journeyId,
    @NonNull LocalDateTime serverTime,
    @Nullable UUID prevPointId,
    @Nullable SimRailPoint currentPoint,
    EventBusProto.@Nullable SignalInfo nextSignal
  ) implements JourneyEventUpdateRequest {

    public ForPointChange {
      Assert.isTrue(prevPointId != null || currentPoint != null, "prev or current point must be given");
    }

    /**
     * Get if the journey arrived at a new point along the route.
     *
     * @return true if the journey arrived at a new point, false otherwise.
     */
    public boolean arrivedAtPoint() {
      return this.currentPoint != null;
    }

    /**
     * Get if the journey departed from any point along the route.
     *
     * @return true if the journey departed from a point, false otherwise.
     */
    public boolean departedFromPoint() {
      return this.prevPointId != null;
    }
  }

  /**
   * Used to indicate that the signal in front of the journey updated. Only used to record the current platform where
   * the journey is at, therefore the current point as well as the next signal id are mandatory.
   *
   * @param journeyId    the id of the journey that should be updated.
   * @param serverTime   the time on the associated server when the request was created.
   * @param currentPoint the current point where the journey is at
   * @param nextSignalId the id of the signal in front of the journey.
   */
  record ForSignalUpdate(
    @NonNull UUID journeyId,
    @NonNull LocalDateTime serverTime,
    @NonNull SimRailPoint currentPoint,
    @NonNull String nextSignalId
  ) implements JourneyEventUpdateRequest {

  }

  /**
   * Request to update the events of a journey because the journey is no longer active on the server.
   *
   * @param journeyId  the id of the journey that should be updated.
   * @param serverTime the time on the associated server when the request was created.
   */
  record ForRemoval(@NonNull UUID journeyId, @NonNull LocalDateTime serverTime) implements JourneyEventUpdateRequest {

  }
}
