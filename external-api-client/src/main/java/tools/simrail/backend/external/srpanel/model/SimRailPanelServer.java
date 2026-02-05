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

/**
 * Information about a multiplayer server provided officially by SimRail.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public final class SimRailPanelServer {

  /**
   * The id of the server. Is a mongo object id.
   */
  @JsonProperty("id")
  private String id;

  /**
   * The code of the server which can be used to retrieve additional data from api.
   */
  @JsonProperty("ServerCode")
  private String code;
  /**
   * The full display name of the server.
   */
  @JsonProperty("ServerName")
  private String name;

  /**
   * The region where the server is located.
   */
  @JsonProperty("ServerRegion")
  private Region region;
  /**
   * If the server is currently active and online.
   */
  @JsonProperty("IsActive")
  private boolean online;

  /**
   * The possible regions where servers can be located.
   */
  public enum Region {

    /**
     * Asia region.
     */
    @JsonProperty("Asia")
    ASIA,
    /**
     * Europe region.
     */
    @JsonProperty("Europe")
    EUROPE,
    /**
     * North America region.
     */
    @JsonProperty("North_America")
    US_NORTH,
  }
}
