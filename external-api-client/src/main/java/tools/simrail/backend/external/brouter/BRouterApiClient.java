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

package tools.simrail.backend.external.brouter;

import feign.QueryMap;
import feign.RequestLine;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import tools.simrail.backend.external.FeignClientProvider;
import tools.simrail.backend.external.brouter.request.BRouterRouteRequest;

public interface BRouterApiClient {

  @Contract("-> new")
  static @NotNull BRouterApiClient create() {
    return FeignClientProvider.prepareFeignInstance()
      .target(BRouterApiClient.class, "https://brouter.de");
  }

  /**
   * Requests the routing along the given waypoints.
   *
   * @param request the request holding the routing options.
   * @return a stream of the routing output data.
   */
  @RequestLine("GET /brouter")
  String route(@QueryMap BRouterRouteRequest request);
}
