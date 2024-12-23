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

package tools.simrail.backend.external.steam;

import feign.CollectionFormat;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import java.util.List;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import tools.simrail.backend.external.FeignClientProvider;
import tools.simrail.backend.external.steam.feign.SteamApiKeyInterceptor;
import tools.simrail.backend.external.steam.wrapper.SteamAchievementPercentageWrapper;
import tools.simrail.backend.external.steam.wrapper.SteamUserStatsWrapper;
import tools.simrail.backend.external.steam.wrapper.SteamUserSummaryWrapper;

@Headers({"Accept: application/json"})
public interface SteamApiClient {

  @Contract("_ -> new")
  static @NotNull SteamApiClient create(@NotNull String apiKey) {
    return FeignClientProvider.prepareJsonFeignInstance()
      .requestInterceptor(new SteamApiKeyInterceptor(apiKey))
      .target(SteamApiClient.class, "https://api.steampowered.com");
  }

  /**
   * Get detailed information about the players with the given steam ids. Up to 100 players can be requested in a single
   * request.
   *
   * @param steamIds the steam ids of the players to get.
   * @return detailed information about the players with the given steam ids.
   */
  @NotNull
  @RequestLine(value = "GET /ISteamUser/GetPlayerSummaries/v0002?steamids={steamIds}", collectionFormat = CollectionFormat.CSV)
  SteamUserSummaryWrapper.Root getPlayerSummaries(@Param("steamIds") List<String> steamIds);

  /**
   * Get the stats of the given user for the given game.
   *
   * @param appId  the id of the game to get the stats of.
   * @param userId the steam id of the user to get the stats of.
   * @return the stats of the given user in the given game.
   */
  @NotNull
  @RequestLine("GET /ISteamUserStats/GetUserStatsForGame/v0002?appid={appId}&steamid={userId}")
  SteamUserStatsWrapper getUserStats(@Param("appId") String appId, @Param("userId") String userId);

  /**
   * Get the global achievement progress of all players for the given game.
   *
   * @param appId the id of the game to get the achievement progress for.
   * @return the global achievement progress of all players in the given game.
   */
  @NotNull
  @RequestLine("GET /ISteamUserStats/GetGlobalAchievementPercentagesForApp/v0002?gameid={appId}")
  SteamAchievementPercentageWrapper.Root getGlobalAchievementProgress(@Param("appId") String appId);
}
