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

package tools.simrail.backend.collector.server;

import feign.Response;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;

final class ServerTimeUtil {

  private ServerTimeUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Calculates the seconds between the server time and UTC from the given time endpoint http response.
   *
   * @param serverTimeResponse the response from the SimRail api time endpoint.
   * @return the time difference between UTC and the server time, in seconds.
   */
  public static @Nullable Long calculateTimezoneOffsetSeconds(@NonNull Response serverTimeResponse) {
    try (var bodyStream = serverTimeResponse.body().asInputStream()) {
      // parse the server time returned in the response body
      var serverTimeBytes = bodyStream.readAllBytes();
      var serverTimeString = new String(serverTimeBytes, StandardCharsets.UTF_8);
      var serverTimeMillis = Long.parseLong(serverTimeString);
      if (serverTimeMillis == -1) {
        // api responds with -1 if the server is offline, cannot parse time in that case
        return null;
      }

      // parse the date when the server sent the response (current time)
      var dateValues = serverTimeResponse.headers().get(HttpHeaders.DATE);
      var dateValue = dateValues.iterator().next();
      var responseDate = ZonedDateTime.parse(dateValue, DateTimeFormatter.RFC_1123_DATE_TIME);
      var responseInstant = responseDate.toInstant();

      // calculate the time difference between the realtime and server time in seconds
      var serverTime = Instant.ofEpochMilli(serverTimeMillis);
      var between = Duration.between(responseInstant, serverTime);
      return between.toSeconds();
    } catch (Exception exception) {
      throw new IllegalStateException("Unable to parse server time response", exception);
    }
  }

  /**
   * Converts the given second value to the closest hour value.
   *
   * @param seconds the seconds to convert to hours.
   * @return the closest hour value based on the given seconds.
   */
  public static int convertToHours(long seconds) {
    return (int) Math.round(seconds / 3600.0); // 3600 - 1 hour in seconds
  }
}
