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

package tools.simrail.backend.external.srpanel.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Full information about a train that is driving on a server.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public final class SimRailPanelTrain {

  /**
   * The unique identifier of the train. This is a mongo object identifier.
   */
  @JsonProperty("id")
  private String id;
  /**
   * The id of the train run.
   */
  @JsonProperty("RunId")
  private UUID runId;
  /**
   * The server code on which the train is running.
   */
  @JsonProperty("ServerCode")
  private String serverCode;

  /**
   * The name of the train.
   */
  @JsonProperty("TrainName")
  private String name;
  /**
   * The number of the train.
   */
  @JsonProperty("TrainNoLocal")
  private String number;

  /**
   * The station where the journey initially departs.
   */
  @JsonProperty("StartStation")
  private String departureStationName;
  /**
   * The station where the journey terminates.
   */
  @JsonProperty("EndStation")
  private String destinationStationName;

  /**
   * The vehicles that build up the train. The first entry is the locomotive, followed by the wagons (in order).
   */
  @JsonProperty("Vehicles")
  private List<String> vehicles;

  /**
   * Detailed information about the current train state.
   */
  @JsonProperty("TrainData")
  private DetailData detailData;

  /**
   * Detailed data about the train.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static final class DetailData {

    /**
     * The steam id of the player that is controlling the train, null if controlled by a bot.
     */
    @JsonProperty("ControlledBySteamID")
    private String driverSteamId;

    /**
     * The current speed of the train.
     */
    @JsonProperty("Velocity")
    private double currentSpeed;
    /**
     * The latitude of the train position. Can be null in rare cases.
     */
    @JsonProperty("Latititute")
    private Double positionLatitude;
    /**
     * The longitude of the train position. Can be null in rare cases.
     */
    @JsonProperty("Longitute")
    private Double positionLongitude;

    /**
     * The id of the signal in front of the train. Can be null if >5 km away.
     */
    @JsonProperty("SignalInFront")
    private String nextSignalId;
    /**
     * The allowed maximum speed that is shown by the next signal.
     */
    @JsonProperty("SignalInFrontSpeed")
    private short nextSignalSpeed;
    /**
     * The distance to the next signal. Can be null in rare cases.
     */
    @JsonProperty("DistanceToSignalInFront")
    private Double nextSignalDistance;
  }
}
