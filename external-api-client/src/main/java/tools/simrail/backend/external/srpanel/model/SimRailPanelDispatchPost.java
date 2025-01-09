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

package tools.simrail.backend.external.srpanel.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Information about a dispatch post that is playable in SimRail.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public final class SimRailPanelDispatchPost {

  /**
   * The id of the dispatch post. This is a fully encoded mongo id.
   */
  @JsonProperty("id")
  private String id;

  /**
   * The full name of the station associated with the dispatch post.
   */
  @JsonProperty("Name")
  private String stationName;
  /**
   * The prefix of the station associated with the dispatch post. For example used for signals of the station.
   */
  @JsonProperty("Prefix")
  private String stationPrefix;

  /**
   * The difficulty of the dispatch post. Number from 1 (easy) to 5 (very difficult).
   */
  @JsonProperty("DifficultyLevel")
  private int difficulty;

  /**
   * The latitude of the position of the dispatch post building.
   */
  @JsonProperty("Latititude")
  private double positionLatitude;
  /**
   * The longitude of the position of the dispatch post building.
   */
  @JsonProperty("Longitude")
  private double positionLongitude;

  /**
   * The first picture of the station.
   */
  @JsonProperty("MainImageURL")
  private String image1Url;
  /**
   * The second picture of the station.
   */
  @JsonProperty("AdditionalImage1URL")
  private String image2Url;
  /**
   * The third picture of the station.
   */
  @JsonProperty("AdditionalImage2URL")
  private String image3Url;

  /**
   * The current dispatchers of the station. At the moment only a single player can dispatch a station at a time.
   */
  @JsonProperty("DispatchedBy")
  private List<Dispatcher> dispatchers;

  /**
   * Information about the person that is currently dispatching the station.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static final class Dispatcher {

    /**
     * The steam user identifier of the player that is dispatching the station.
     */
    @JsonProperty("SteamId")
    private String steamId;
  }
}
