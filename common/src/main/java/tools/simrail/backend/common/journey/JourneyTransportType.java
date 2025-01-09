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

package tools.simrail.backend.common.journey;

import jakarta.annotation.Nonnull;
import java.util.Locale;
import java.util.Objects;

/**
 * A list of higher-level categories for a journey transport.
 */
public enum JourneyTransportType {

  // passenger trains
  /**
   * Trains that are only driving within the nation and only have a few stops in big cities.
   */
  NATIONAL_EXPRESS_TRAIN,
  /**
   * Trains that are driving internationally and only have a few stops in big cities.
   */
  INTER_NATIONAL_EXPRESS_TRAIN,
  /**
   * Trains that are driving interregional and only have a few stops.
   */
  INTER_REGIONAL_EXPRESS_TRAIN,
  /**
   * Trains that are driving interregional and stop at a lot of places.
   */
  INTER_REGIONAL_TRAIN,
  /**
   * Trains that are only driving in a local region but don't have a lot of stops.
   */
  REGIONAL_FAST_TRAIN,
  /**
   * Trains that are driving in a local region and stop at a lot of places.
   */
  REGIONAL_TRAIN,
  /**
   * Trains that are added to the schedule when the demand is very high.
   */
  ADDITIONAL_TRAIN,

  // empty/trial trains
  /**
   * Trains that are only used for maneuver/shunting operations.
   */
  MANEUVER_TRAIN,
  /**
   * Trains that are being transferred from one place to another.
   */
  EMPTY_TRANSFER_TRAIN,

  // cargo trains
  /**
   * International cargo trains.
   */
  INTER_NATIONAL_CARGO_TRAIN,
  /**
   * National cargo trains.
   */
  NATIONAL_CARGO_TRAIN,

  // maintenance trains
  /**
   * Trains for railway maintenance work.
   */
  MAINTENANCE_TRAIN;

  /**
   * Maps the given train type (3 characters long) string to transport type.
   *
   * @param trainType the train type to map.
   * @return the mapped transport type.
   * @throws IllegalArgumentException if the given train type cannot be mapped.
   */
  public static @Nonnull JourneyTransportType fromTrainType(@Nonnull String trainType) {
    // train types must be 3 characters long
    Objects.checkIndex(2, trainType.length());
    var trainTypeIdentifier = trainType.substring(0, 2);
    return switch (trainTypeIdentifier.toUpperCase(Locale.ROOT)) {
      // passenger trains
      case "EI" -> NATIONAL_EXPRESS_TRAIN;
      case "EC", "EN", "MM" -> INTER_NATIONAL_EXPRESS_TRAIN;
      case "MP", "MH" -> INTER_REGIONAL_EXPRESS_TRAIN;
      case "MO", "MA" -> INTER_REGIONAL_TRAIN;
      case "RP" -> REGIONAL_FAST_TRAIN;
      case "RA", "RM", "RO", "AM", "AP" -> REGIONAL_TRAIN;
      case "OK" -> ADDITIONAL_TRAIN;

      // empty/trial trains
      case "LM", "LW", "LP", "LT", "LZ", "LS" -> MANEUVER_TRAIN;
      case "PC", "PW", "PX", "PH", "TH", "TS", "TT", "TK" -> EMPTY_TRANSFER_TRAIN;

      // cargo trains
      case "TA", "TC", "TG", "TR" -> INTER_NATIONAL_CARGO_TRAIN;
      case "TB", "TD", "TP", "TN", "TM", "TL" -> NATIONAL_CARGO_TRAIN;

      // maintenance trains
      case "ZG", "ZN", "ZX", "ZH" -> MAINTENANCE_TRAIN;

      // unknown train type
      default -> throw new IllegalArgumentException("Unknown train type identifier: " + trainTypeIdentifier);
    };
  }
}
