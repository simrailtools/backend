/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024 Pasqual Koschmieder and contributors
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
import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import tools.simrail.backend.collector.server.SimRailServerDescriptor;
import tools.simrail.backend.common.journey.JourneyEntity;
import tools.simrail.backend.common.journey.JourneySignalInfo;
import tools.simrail.backend.common.shared.GeoPositionEntity;

/**
 * A recorder for the dirty state of a journey. The changed fields can either be applied to the journey at the end or
 * only kept local to this recorder for sending them to the api.
 */
@RequiredArgsConstructor
final class JourneyDirtyStateRecorder {

  /**
   * The original journey that this recorder records changes of.
   */
  @Getter
  private final JourneyEntity original;
  /**
   * The server on which the associated journey is active.
   */
  private final SimRailServerDescriptor server;

  // === fields keeping track of changes in the recorder
  private boolean removed;
  private ValueHolder<String> foreignId;
  private ValueHolder<Integer> speed;
  private ValueHolder<String> driverSteamId;
  private ValueHolder<GeoPositionEntity> position;
  private ValueHolder<JourneySignalInfo> nextSignal;

  public void updateForeignId(@Nullable final String foreignId) {
    var currentForeignId = this.original.getForeignId();
    if (currentForeignId == null) {
      this.foreignId = new ValueHolder<>(foreignId);
    }
  }

  public void updateSpeed(@Nullable Integer newSpeed) {
    var currentSpeed = this.original.getSpeed();
    if (!Objects.equals(currentSpeed, newSpeed)) {
      this.speed = new ValueHolder<>(newSpeed);
    }
  }

  public void updateDriverSteamId(@Nullable String newDriverSteamId) {
    var currentDriverSteamId = this.original.getDriverSteamId();
    if (!Objects.equals(currentDriverSteamId, newDriverSteamId)) {
      this.driverSteamId = new ValueHolder<>(newDriverSteamId);
    }
  }

  public void updatePosition(@Nullable GeoPositionEntity newPosition) {
    var currentPosition = this.original.getPosition();
    if (!Objects.equals(currentPosition, newPosition)) {
      this.position = new ValueHolder<>(newPosition);
    }
  }

  public void updateNextSignal(@Nullable JourneySignalInfo newNextSignal) {
    var currentNextSignal = this.original.getNextSignal();
    if (!Objects.equals(currentNextSignal, newNextSignal)) {
      this.nextSignal = new ValueHolder<>(newNextSignal);
    }
  }

  public void markRemoved() {
    if (this.original.getLastSeenTime() == null) {
      this.removed = true;
    }
  }

  /**
   * Gets if this recorder recorded any changes compared to the given original entity.
   *
   * @return true if this recorder recorded any changes compared to the given original entity, false otherwise.
   */
  public boolean isDirty() {
    return this.removed
      || this.foreignId != null
      || this.speed != null
      || this.driverSteamId != null
      || this.position != null
      || this.nextSignal != null;
  }

  /**
   * Applies the dirty field changes hold in this recorder into the provided original entity.
   *
   * @return this holder, for chaining.
   */
  public @Nonnull JourneyDirtyStateRecorder applyChangesToOriginal() {
    // if the journey was removed in this collection cycle then we don't want to
    // apply any of the remaining changes and just set every data field to null
    // and set the last seen time
    if (this.removed) {
      this.original.setSpeed(null);
      this.original.setPosition(null);
      this.original.setNextSignal(null);
      this.original.setDriverSteamId(null);
      this.original.setLastSeenTime(OffsetDateTime.now(this.server.timezoneOffset()));
      return this;
    }

    // apply changes to the original model
    if (this.foreignId != null) {
      this.original.setForeignId(this.foreignId.value());
      this.original.setFirstSeenTime(OffsetDateTime.now(this.server.timezoneOffset()));
    }
    if (this.speed != null) {
      this.original.setSpeed(this.speed.value());
    }
    if (this.driverSteamId != null) {
      this.original.setDriverSteamId(this.driverSteamId.value());
    }
    if (this.position != null) {
      this.original.setPosition(this.position.value());
    }
    if (this.nextSignal != null) {
      this.original.setNextSignal(this.nextSignal.value());
    }

    return this;
  }

  /**
   * A holder for a nullable instance of a generic type.
   *
   * @param value the value being wrapped in this holder.
   * @param <T>   the generic type of the value.
   */
  private record ValueHolder<T>(@Nullable T value) {

  }
}
