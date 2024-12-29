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

package tools.simrail.backend.api.event.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import tools.simrail.backend.common.rpc.JourneyUpdateFrame;
import tools.simrail.backend.common.tristate.Tristate;

/**
 * DTO for updating a journey, only contains the fields that can be updated.
 */
public record EventJourneyUpdateDto(
  @Nonnull String journeyId,
  @Nullable @JsonInclude(JsonInclude.Include.NON_NULL) Integer speed,
  @Nonnull @JsonInclude(JsonInclude.Include.NON_EMPTY) Tristate<String> driverSteamId,

  @Nonnull @JsonInclude(JsonInclude.Include.NON_EMPTY) Tristate<String> nextSignalId,
  @Nonnull @JsonInclude(JsonInclude.Include.NON_EMPTY) Tristate<Integer> nextSignalDistance,
  @Nonnull @JsonInclude(JsonInclude.Include.NON_EMPTY) Tristate<Integer> nextSignalMaxSpeed,

  @Nullable @JsonInclude(JsonInclude.Include.NON_NULL) Double positionLat,
  @Nullable @JsonInclude(JsonInclude.Include.NON_NULL) Double positionLng
) {

  /**
   * Constructs an update DTO instance from the given update frame.
   *
   * @param frame the update frame to construct the update frame from.
   * @return the constructed update dto from the given update frame.
   */
  public static @Nonnull EventJourneyUpdateDto fromJourneyUpdateFrame(@Nonnull JourneyUpdateFrame frame) {
    // extract the position data, if given
    Double positionLat = null;
    Double positionLng = null;
    if (frame.hasPosition()) {
      var position = frame.getPosition();
      positionLat = position.getLatitude();
      positionLng = position.getLongitude();
    }

    // extract the driver steam id, if given
    Tristate<String> driverSteamId = Tristate.undefined();
    if (frame.hasDriver()) {
      var driver = frame.getDriver();
      if (driver.getUpdated()) {
        if (driver.hasSteamId()) {
          driverSteamId = Tristate.holdingValue(driver.getSteamId());
        } else {
          driverSteamId = Tristate.holdingNull();
        }
      }
    }

    // extract the given information about the next signal, if present
    Tristate<String> nextSignalId = Tristate.undefined();
    Tristate<Integer> nextSignalDistance = Tristate.undefined();
    Tristate<Integer> nextSignalMaxSpeed = Tristate.undefined();
    if (frame.hasNextSignal()) {
      var nextSignal = frame.getNextSignal();
      if (nextSignal.getUpdated()) {
        if (nextSignal.hasSignalInfo()) {
          var signalInfo = nextSignal.getSignalInfo();
          nextSignalId = Tristate.holdingValue(signalInfo.getName());
          nextSignalDistance = Tristate.holdingValue(signalInfo.getDistance());
          if (signalInfo.hasMaxSpeed()) {
            nextSignalMaxSpeed = Tristate.holdingValue(signalInfo.getMaxSpeed());
          } else {
            nextSignalMaxSpeed = Tristate.holdingNull();
          }
        } else {
          nextSignalId = Tristate.holdingNull();
          nextSignalDistance = Tristate.holdingNull();
          nextSignalMaxSpeed = Tristate.holdingNull();
        }
      }
    }

    var speed = frame.hasSpeed() ? frame.getSpeed() : null;
    return new EventJourneyUpdateDto(
      frame.getJourneyId(),
      speed,
      driverSteamId,
      nextSignalId,
      nextSignalDistance,
      nextSignalMaxSpeed,
      positionLat,
      positionLng);
  }
}
