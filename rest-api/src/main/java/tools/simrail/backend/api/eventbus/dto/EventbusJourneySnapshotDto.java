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

package tools.simrail.backend.api.eventbus.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.UUID;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import tools.simrail.backend.common.journey.JourneySignalInfo;
import tools.simrail.backend.common.journey.JourneyTransport;
import tools.simrail.backend.common.rpc.JourneyUpdateFrame;
import tools.simrail.backend.common.shared.GeoPositionEntity;

/**
 * DTO with changing fields for journey updates.
 */
@Data
@ToString
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class EventbusJourneySnapshotDto {

  // static fields
  private final UUID journeyId;
  private final UUID serverId;

  private final String category;
  private final String number;
  private final String line;
  private final String label;

  // changing fields
  private Integer speed;
  private String driverSteamId;

  private String nextSignalId;
  private Integer nextSignalDistance;
  private Integer nextSignalMaxSpeed;

  private Double positionLat;
  private Double positionLng;

  public EventbusJourneySnapshotDto(
    @Nonnull UUID journeyId,
    @Nonnull UUID serverId,
    @Nonnull JourneyTransport transport,
    @Nullable Integer speed,
    @Nullable String driverSteamId,
    @Nullable JourneySignalInfo nextSignal,
    @Nullable GeoPositionEntity position
  ) {
    this.journeyId = journeyId;
    this.serverId = serverId;

    this.category = transport.getCategory();
    this.number = transport.getNumber();
    this.line = transport.getLine();
    this.label = transport.getLabel();

    this.speed = speed;
    this.driverSteamId = driverSteamId;

    if (position != null) {
      this.positionLat = position.getLatitude();
      this.positionLng = position.getLongitude();
    }

    if (nextSignal != null) {
      this.nextSignalId = nextSignal.getName();
      this.nextSignalDistance = nextSignal.getDistance();
      if (nextSignal.getMaxAllowedSpeed() != null) {
        this.nextSignalMaxSpeed = nextSignal.getMaxAllowedSpeed().intValue();
      }
    }
  }

  /**
   * Applies the given journey update frame to this snapshot.
   *
   * @param frame the update frame of the journey to apply.
   */
  public synchronized void applyUpdateFrame(@Nonnull JourneyUpdateFrame frame) {
    if (frame.hasSpeed()) {
      this.speed = frame.getSpeed();
    }

    if (frame.hasDriver()) {
      var driver = frame.getDriver();
      if (driver.getUpdated()) {
        this.driverSteamId = driver.hasSteamId() ? driver.getSteamId() : null;
      }
    }

    if (frame.hasNextSignal()) {
      var nextSignal = frame.getNextSignal();
      if (nextSignal.getUpdated()) {
        if (nextSignal.hasSignalInfo()) {
          var nextSignalInfo = nextSignal.getSignalInfo();
          this.nextSignalId = nextSignalInfo.getName();
          this.nextSignalDistance = nextSignalInfo.getDistance();
          this.nextSignalMaxSpeed = nextSignalInfo.getMaxSpeed();
        } else {
          this.nextSignalId = null;
          this.nextSignalDistance = null;
          this.nextSignalMaxSpeed = null;
        }
      }
    }

    if (frame.hasPosition()) {
      var position = frame.getPosition();
      this.positionLat = position.getLatitude();
      this.positionLng = position.getLongitude();
    }
  }
}
