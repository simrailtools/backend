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

package tools.simrail.backend.external.sraws;

import feign.Param;
import feign.RequestLine;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import tools.simrail.backend.external.FeignClientProvider;

public interface SimRailIntAwsApiClient {

  @Contract(" -> new")
  static @NotNull SimRailIntAwsApiClient create() {
    return FeignClientProvider.prepareFeignInstance()
      .target(SimRailIntAwsApiClient.class, "https://api1.aws.simrail.eu:8083");
  }

  /**
   * Get the thumbnail image (as png) of the train with the given type.
   *
   * @param trainType the type of the train to get a thumbnail of.
   * @return the thumbnail for the train with the given type.
   */
  @RequestLine("GET /Thumbnails/Locomotives/{type}.png")
  byte[] getTrainThumbnail(@Param("type") String trainType);
}
