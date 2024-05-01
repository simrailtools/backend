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

package tools.simrail.backend.external.steam.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.ApiStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class SteamUserStats {

  /**
   * The steam id of the user to whom the stats belong.
   */
  @JsonProperty("steamID")
  private String userId;
  /**
   * The name of the requested game.
   */
  @JsonProperty("gameName")
  private String gameName;

  /**
   * The stats of the user in the requested game.
   */
  @JsonProperty("stats")
  private List<Stat> stats;
  /**
   * The achievements of the user in the requested game.
   */
  @JsonProperty("achievements")
  private List<Achievement> achievements;

  /**
   * A statistic about the requested player in the requested game.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static final class Stat {

    /**
     * The name of the statistic.
     */
    @JsonProperty("name")
    private String name;
    /**
     * The value of the statistic.
     */
    @JsonProperty("value")
    private int value;
  }

  /**
   * An achievement that can be achieved in a game.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static final class Achievement {

    /**
     * The name of the achievement.
     */
    @JsonProperty("name")
    private String name;
    /**
     * If the achievement was achieved by the user.
     */
    private boolean achieved;

    /**
     * Deserializes the achievement status of this achievement.
     *
     * @param achieved the status of this achievement. Can be {@code 0} (not achieved) or {@code 1} (achieved).
     */
    @ApiStatus.Internal
    @JsonProperty("achieved")
    public void deserializeAchieved(int achieved) {
      this.achieved = achieved == 1;
    }
  }
}
