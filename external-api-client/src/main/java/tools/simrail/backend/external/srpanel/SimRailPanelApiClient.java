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

package tools.simrail.backend.external.srpanel;

import feign.Headers;
import feign.Param;
import feign.RequestLine;
import java.util.List;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import tools.simrail.backend.external.FeignClientProvider;
import tools.simrail.backend.external.srpanel.model.SimRailPanelDispatchPost;
import tools.simrail.backend.external.srpanel.model.SimRailPanelResponseWrapper;
import tools.simrail.backend.external.srpanel.model.SimRailPanelServer;
import tools.simrail.backend.external.srpanel.model.SimRailPanelTrain;
import tools.simrail.backend.external.srpanel.model.SimRailPanelTrainPosition;
import tools.simrail.backend.external.srpanel.model.SimRailPanelUserInfo;

@Headers({"Accept: application/json"})
public interface SimRailPanelApiClient {

  @Contract(" -> new")
  static @NotNull SimRailPanelApiClient create() {
    return FeignClientProvider.prepareJsonFeignInstance()
      .target(SimRailPanelApiClient.class, "https://panel.simrail.eu:8084");
  }

  /**
   * Lists all official SimRail servers (not depending on the state [offline or online]).
   *
   * @return all official SimRail servers.
   */
  @NotNull
  @RequestLine("GET /servers-open")
  SimRailPanelResponseWrapper<SimRailPanelServer> getServers();

  /**
   * Lists all trains that are currently active on the specified server.
   *
   * @param serverCode the code of the servers to retrieve the trains of.
   * @return the trains that are currently active on the server with the given code.
   */
  @NotNull
  @RequestLine("GET /trains-open?serverCode={serverCode}")
  SimRailPanelResponseWrapper<SimRailPanelTrain> getTrains(@Param("serverCode") String serverCode);

  /**
   * Get the current positions of the trains that are active on the specified server.
   *
   * @param serverCode the code of the servers to retrieve the train positions of.
   * @return the positions of the trains that are currently active on the server with the given code.
   */
  @NotNull
  @RequestLine("GET /train-positions-open?serverCode={serverCode}")
  SimRailPanelResponseWrapper<SimRailPanelTrainPosition> getTrainPositions(@Param("serverCode") String serverCode);

  /**
   * Lists the dispatch posts that are available on the specified server.
   *
   * @param serverCode the code of the server to retrieve the dispatch posts of.
   * @return the dispatch posts that are available on the server with the given code.
   */
  @NotNull
  @RequestLine("GET /stations-open?serverCode={serverCode}")
  SimRailPanelResponseWrapper<SimRailPanelDispatchPost> getDispatchPosts(@Param("serverCode") String serverCode);

  /**
   * Get the current SimRail information about one or more users.
   *
   * @param userIds the steam ids of the users to get the current state information of.
   * @return the current SimRail state information of the requested users.
   */
  @NotNull
  @RequestLine("GET /users-open/{userIds}?force=true")
  SimRailPanelResponseWrapper<SimRailPanelUserInfo> getUserInfo(@Param("userIds") List<String> userIds);
}
