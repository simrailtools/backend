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

package tools.simrail.backend.external.simkol;

import feign.Headers;
import feign.RequestLine;
import java.util.List;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import tools.simrail.backend.external.FeignClientProvider;
import tools.simrail.backend.external.simkol.model.SimKolSpeedLimit;

@Headers({"Accept: application/json"})
public interface SimKolApiClient {

  @Contract("-> new")
  static @NotNull SimKolApiClient create() {
    return FeignClientProvider.prepareJsonFeignInstance()
      .target(SimKolApiClient.class, "https://webhost.simkol.pl");
  }

  /**
   * Get the speed limits that globally apply to all tracks.
   *
   * @return the speed limits that globally apply to all tracks.
   */
  @NotNull
  @RequestLine("GET /speeds.json")
  List<SimKolSpeedLimit> getSpeedLimits();
}
