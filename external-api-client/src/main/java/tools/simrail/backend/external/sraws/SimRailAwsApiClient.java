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

package tools.simrail.backend.external.sraws;

import feign.Param;
import feign.RequestLine;
import feign.Response;
import java.util.List;
import org.jspecify.annotations.NonNull;
import tools.simrail.backend.external.FeignClientProvider;
import tools.simrail.backend.external.sraws.model.SimRailAwsTrainRun;

public interface SimRailAwsApiClient {

  static @NonNull SimRailAwsApiClient create() {
    return FeignClientProvider.prepareJsonFeignInstance()
      .target(SimRailAwsApiClient.class, "https://api1.aws.simrail.eu:8082/api");
  }

  /**
   * Get the timezone offset hours of the server with the given code.
   *
   * @param serverCode the code of the server to get the offset of.
   * @return the timezone offset hours of the requested server.
   */
  @RequestLine("GET /getTimeZone?serverCode={serverCode}")
  int getServerTimeOffset(@Param("serverCode") String serverCode);

  /**
   * Get the server time in milliseconds since the epoch.
   *
   * @param serverCode the code of the server to get the time of.
   * @return the time of the requested server in millis since the epoch.
   */
  @RequestLine("GET /getTime?serverCode={serverCode}")
  Response getServerTimeMillis(@Param("serverCode") String serverCode);

  /**
   * Get all current and future scheduled train runs of a specific server.
   *
   * @param serverCode the code of the server to get the train runs of.
   * @return all current and future train runs of the specified server.
   */
  @RequestLine("GET /getAllTimetables?serverCode={serverCode}")
  List<SimRailAwsTrainRun> getTrainRuns(@Param("serverCode") String serverCode);

  /**
   * Get the train runs (currently only returns the most recent one) of the train with the specified number.
   *
   * @param serverCode  the code of the server to get the train runs on.
   * @param trainNumber the number of the train to get the runs of.
   * @return the train runs of the given train on the given server.
   */
  @RequestLine("GET /getAllTimetables?serverCode={serverCode}&train={train}")
  List<SimRailAwsTrainRun> getTrainRuns(@Param("serverCode") String serverCode, @Param("train") String trainNumber);
}
