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

package tools.simrail.backend.common.vehicle;

import org.jspecify.annotations.NonNull;

/**
 * An enumeration of the different loads that can be transported by a vehicle.
 */
public enum JourneyVehicleLoad {

  TIE,
  T_BEAM,
  PIPELINE,
  CONTAINER,
  TREE_TRUNK,
  WOODEN_BEAM,
  METAL_SHEET,
  STEEL_CIRCLE,
  CONCRETE_SLAB,
  GAS_CONTAINER,

  PETROL,
  ETHANOL,
  CRUDE_OIL,
  HEATING_OIL,

  COAL,
  SAND,
  BALLAST,
  WOOD_LOGS,

  UNKNOWN;

  /**
   * Maps the given load name provided by the SimRail api to its corresponding vehicle load constant.
   *
   * @param apiLoadName the name of the load returned by the SimRail api.
   * @return the vehicle load constant corresponding to the given load name.
   */
  public static @NonNull JourneyVehicleLoad mapApiLoadType(@NonNull String apiLoadName) {
    return switch (apiLoadName) {
      // 412W
      case "Tie" -> TIE;
      case "T_beam" -> T_BEAM;
      case "Tree_trunk" -> TREE_TRUNK;
      case "Wooden_beam" -> WOODEN_BEAM;
      case "Sheet_metal" -> METAL_SHEET;
      case "Steel_circle" -> STEEL_CIRCLE;
      case "Concrete_slab" -> CONCRETE_SLAB;
      case "Pipeline", "Gas_pipeline" -> PIPELINE;
      case "GasContainer4x20", "GasContainer3x20", "GasContainer2x20" -> GAS_CONTAINER;
      case "RandomContainerAll",
           "RandomContainer3x20",
           "RandomContainer1x40",
           "RandomContainer2040",
           "RandomContainer1x20",
           "RandomContainer2x20",
           "RandomContainer4x20",
           "RandomContainer2x40",
           "RandomContainer202040",
           "RandomContainer402020" -> CONTAINER;

      // 406Ra
      case "Petrol" -> PETROL;
      case "Ethanol" -> ETHANOL;
      case "CrudeOil" -> CRUDE_OIL;
      case "HeatingOil" -> HEATING_OIL;

      // 412W_v4
      case "Coal" -> COAL;
      case "Sand" -> SAND;
      case "Ballast" -> BALLAST;
      case "WoodLogs" -> WOOD_LOGS;

      // fallback
      default -> UNKNOWN;
    };
  }
}
