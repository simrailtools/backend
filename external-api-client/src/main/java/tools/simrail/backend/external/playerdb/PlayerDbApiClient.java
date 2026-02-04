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

package tools.simrail.backend.external.playerdb;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import tools.simrail.backend.external.FeignClientProvider;
import tools.simrail.backend.external.feign.FeignHeaderInterceptor;
import tools.simrail.backend.external.feign.FeignJava11NoErrorClient;
import tools.simrail.backend.external.playerdb.model.PlayerDbResponseWrapper;

@Headers({"Accept: application/json"})
public interface PlayerDbApiClient {

  static @NonNull PlayerDbApiClient create(@NonNull String userAgent) {
    return FeignClientProvider.prepareJsonFeignInstance()
      .client(new FeignJava11NoErrorClient())
      .requestInterceptor(new FeignHeaderInterceptor(Map.of("User-Agent", userAgent)))
      .target(PlayerDbApiClient.class, "https://playerdb.co/api");
  }

  /**
   * Get player data from the playerdb steam endpoint.
   *
   * @param steamId the steam id of the player to fetch.
   * @return the player data for the requested steam user.
   */
  @NonNull
  @RequestLine("GET /player/steam/{steamId}")
  PlayerDbResponseWrapper getSteamPlayer(@Param("steamId") String steamId);

  /**
   * Get player data from the playerdb xbox endpoint.
   *
   * @param xuid the xbox user id of the player to fetch.
   * @return the player data for the requested xbox user.
   */
  @NonNull
  @RequestLine("GET /player/xbox/{xuid}")
  PlayerDbResponseWrapper getXboxPlayer(@Param("xuid") String xuid);
}
