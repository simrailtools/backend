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
import jakarta.annotation.Nullable;
import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import tools.simrail.backend.collector.server.SimRailServerDescriptor;
import tools.simrail.backend.common.journey.JourneyEntity;
import tools.simrail.backend.common.journey.JourneySignalInfo;
import tools.simrail.backend.common.rpc.GeoPosition;
import tools.simrail.backend.common.rpc.JourneyUpdateFrame;
import tools.simrail.backend.common.rpc.SignalInfo;
import tools.simrail.backend.common.rpc.SignalInfoWrapper;
import tools.simrail.backend.common.rpc.SteamIdWrapper;
import tools.simrail.backend.common.rpc.UpdateType;
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

  /**
   * Sets the foreign id in this recorder in case the foreign id of the original journey is not yet defined.
   *
   * @param foreignId the foreign id to set.
   */
  public void updateForeignId(@Nullable String foreignId) {
    var currentForeignId = this.original.getForeignId();
    if (currentForeignId == null) {
      this.foreignId = new ValueHolder<>(foreignId);
    }
  }

  /**
   * Sets the speed of the journey in this recorder in case it differs from the original journey.
   *
   * @param newSpeed the new speed to set.
   */
  public void updateSpeed(@Nullable Integer newSpeed) {
    var currentSpeed = this.original.getSpeed();
    if (!Objects.equals(currentSpeed, newSpeed)) {
      this.speed = new ValueHolder<>(newSpeed);
    }
  }

  /**
   * Sets the steam id of the driver in this recorder in case it differs from the original journey.
   *
   * @param newDriverSteamId the steam id of the driver to set.
   */
  public void updateDriverSteamId(@Nullable String newDriverSteamId) {
    var currentDriverSteamId = this.original.getDriverSteamId();
    if (!Objects.equals(currentDriverSteamId, newDriverSteamId)) {
      this.driverSteamId = new ValueHolder<>(newDriverSteamId);
    }
  }

  /**
   * Sets the position of the journey in this recorder in case it differs from the original journey.
   *
   * @param newPosition the position of the journey to set.
   */
  public void updatePosition(@Nullable GeoPositionEntity newPosition) {
    var currentPosition = this.original.getPosition();
    if (!Objects.equals(currentPosition, newPosition)) {
      this.position = new ValueHolder<>(newPosition);
    }
  }

  /**
   * Sets the information about the next signal in this recorder in case it differs from the original journey.
   *
   * @param newNextSignal the info about the next signal to set.
   */
  public void updateNextSignal(@Nullable JourneySignalInfo newNextSignal) {
    var currentNextSignal = this.original.getNextSignal();
    if (!Objects.equals(currentNextSignal, newNextSignal)) {
      this.nextSignal = new ValueHolder<>(newNextSignal);
    }
  }

  /**
   * Marks this journey as removed. This will override all properties in this recorder and the original journey if
   * applied.
   */
  public void markRemoved() {
    if (this.original.getLastSeenTime() == null) {
      this.removed = true;
    }
  }

  /**
   * Get if the position of the journey was updated.
   *
   * @return true if the position of the journey was updated, false otherwise.
   */
  public boolean hasPositionChanged() {
    return this.position != null;
  }

  /**
   * Get if the journey was removed.
   *
   * @return true if the journey was removed, false otherwise.
   */
  public boolean wasRemoved() {
    return this.removed;
  }

  /**
   * Get if the journey was first seen.
   *
   * @return true if the journey was first seen, false otherwise.
   */
  public boolean wasFirstSeen() {
    return this.foreignId != null;
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
      this.original.setForeignId(null);
      this.original.setNextSignal(null);
      this.original.setDriverSteamId(null);
      this.original.setLastSeenTime(this.server.currentTime());
      return this;
    }

    // apply changes to the original model
    if (this.foreignId != null) {
      this.original.setForeignId(this.foreignId.value());
      this.original.setFirstSeenTime(this.server.currentTime());
      this.original.setLastSeenTime(null);
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
   * Builds an update frame for the server based on the recoded changed fields. Also handles the add and remove of the
   * journeys.
   *
   * @return an update frame for the associated journey based on the recorded changed fields.
   */
  @SuppressWarnings("DataFlowIssue") // no, the journey id is not null
  public @Nonnull JourneyUpdateFrame buildUpdateFrame() {
    if (this.removed) {
      return JourneyUpdateFrame.newBuilder()
        .setUpdateType(UpdateType.REMOVE)
        .setServerId(this.server.id().toString())
        .setJourneyId(this.original.getId().toString())
        .setDriver(SteamIdWrapper.getDefaultInstance())
        .setNextSignal(SignalInfoWrapper.getDefaultInstance())
        .build();
    }

    // default information for the update frame
    var updateType = this.foreignId != null ? UpdateType.ADD : UpdateType.UPDATE;
    var updateFrameBuilder = JourneyUpdateFrame.newBuilder()
      .setUpdateType(updateType)
      .setServerId(this.server.id().toString())
      .setJourneyId(this.original.getId().toString());

    // insert information if the driver steam id changed
    if (this.driverSteamId != null) {
      var driverSteamId = this.driverSteamId.value();
      var steamIdWrapper = SteamIdWrapper.newBuilder().setUpdated(true);
      if (driverSteamId != null) {
        steamIdWrapper.setSteamId(driverSteamId);
      }
      updateFrameBuilder.setDriver(steamIdWrapper);
    } else {
      updateFrameBuilder.setDriver(SteamIdWrapper.getDefaultInstance());
    }

    // insert information if the next signal of the journey changed
    if (this.nextSignal != null) {
      var nextSignal = this.nextSignal.value();
      var nextSignalWrapper = SignalInfoWrapper.newBuilder().setUpdated(true);
      if (nextSignal != null) {
        var maxAllowedSpeed = nextSignal.getMaxAllowedSpeed();
        var signalInfo = SignalInfo.newBuilder()
          .setName(nextSignal.getName())
          .setDistance(nextSignal.getDistance());
        if (maxAllowedSpeed != null) {
          signalInfo.setMaxSpeed(maxAllowedSpeed);
        }
        nextSignalWrapper.setSignalInfo(signalInfo);
      }
      updateFrameBuilder.setNextSignal(nextSignalWrapper);
    } else {
      updateFrameBuilder.setNextSignal(SignalInfoWrapper.getDefaultInstance());
    }

    // insert information about the new speed of the journey
    if (this.speed != null) {
      updateFrameBuilder.setSpeed(this.speed.value());
    }

    // insert information about the new position of the journey
    if (this.position != null) {
      var position = this.position.value();
      var updatedPosition = GeoPosition.newBuilder()
        .setLatitude(position.getLatitude())
        .setLongitude(position.getLongitude());
      updateFrameBuilder.setPosition(updatedPosition);
    }

    return updateFrameBuilder.build();
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
