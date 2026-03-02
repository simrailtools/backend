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

package tools.simrail.backend.common;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Assertions;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;

/**
 * Utility class for tests which statically holds or downloads the train timetable to a local file. The timetable is
 * only updated once per hour to reduce load on the SimRail backend when tests are executed many times.
 */
public final class TimetableHolder {

  /**
   * The default server from which the timetable is retrieved if no specific one is requested.
   */
  private static final String DEFAULT_SERVER_CODE = "de1";
  /**
   * The location of the tmp directory in which the timetable is locally stored. Uses {@code ../tmp}.
   */
  private static final Path TMP_DIR = Path.of("").toAbsolutePath().getParent().resolve("tmp");

  static {
    // initializes the tmp directory if needed
    try {
      if (Files.notExists(TMP_DIR)) {
        Files.createDirectories(TMP_DIR);
      }
    } catch (IOException exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  private TimetableHolder() {
    throw new UnsupportedOperationException();
  }

  /**
   * Get the timetable data of the default server, either from local cached or from the SimRail backend.
   *
   * @return the timetable data of the default server.
   */
  public static ArrayNode getDefaultServerTimetable() {
    return getServerTimetable(DEFAULT_SERVER_CODE);
  }

  /**
   * Get the timetable data of the given server, either from local cached or from the SimRail backend.
   *
   * @param serverCode the code of the server to get the timetable data from.
   * @return the timetable data of the given server.
   */
  public static ArrayNode getServerTimetable(String serverCode) {
    var time = LocalDateTime.now();
    var cacheFileName = String.format("timetable_%s_%s_%s.json", serverCode, time.toLocalDate(), time.getHour());
    var cacheFilePath = TMP_DIR.resolve(cacheFileName);
    if (Files.notExists(cacheFilePath)) {
      try {
        // download the timetable and cache it
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
          .GET()
          .uri(URI.create("https://api1.aws.simrail.eu:8082/api/getAllTimetables?serverCode=" + serverCode))
          .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofFile(cacheFilePath));
        Assertions.assertEquals(200, response.statusCode());
      } catch (IOException | InterruptedException exception) {
        throw new IllegalStateException("Unable to download timetable for server " + serverCode, exception);
      }
    }

    // read the timetable and parse it
    try (var timetableInputStream = Files.newInputStream(cacheFilePath, StandardOpenOption.READ)) {
      var jsonMapper = new JsonMapper();
      return (ArrayNode) jsonMapper.readTree(timetableInputStream);
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to parse timetable for server " + serverCode, exception);
    }
  }
}
