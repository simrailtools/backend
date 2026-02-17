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

package tools.simrail.backend.external.srpanel.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class SimRailPanelUserInfo {

  /**
   * The steam id of the user to which this data belongs.
   */
  @JsonProperty("SteamId")
  private String steamUserId;

  /**
   * The current state of the user.
   */
  @JsonProperty("UserType")
  private State currerntState = State.NONE;
  /**
   * The code of the server on which the player is currently playing, null if the player is currently not on a server.
   */
  @JsonProperty("ServerCode")
  private String currentServerCode;
  /**
   * The detailed information about the current player state, null if the player is currently not on a server.
   */
  @JsonProperty("AdditionalInformation")
  private DetailData currerntDetailData;

  public enum State {

    /**
     * The user is currently not playing on any server.
     */
    NONE,
    /**
     * The user is currently driving a train on a server.
     */
    @JsonProperty("driver")
    DRIVER,
    /**
     * The user is currently dispatching a station on a server.
     */
    @JsonProperty("dispatcher")
    DISPATCHER,
  }

  /**
   * Detailed information about the current player activity.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static final class DetailData {

    /**
     * The number of the train that is driven by the player on the given server. Only present if the player is currently
     * driving any train.
     */
    @JsonProperty("TrainNoLocal")
    private String trainNumber;
    /**
     * The name of the dispatch post that is dispatched by the player on the given server. Only present if the player is
     * currently dispatching a station.
     */
    @JsonProperty("StationName")
    private String dispatchPostName;
  }
}
