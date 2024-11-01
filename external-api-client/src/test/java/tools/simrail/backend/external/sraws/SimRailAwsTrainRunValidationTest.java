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

package tools.simrail.backend.external.sraws;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.simrail.backend.external.util.JsonFieldValidator;

public class SimRailAwsTrainRunValidationTest {

  private static final Set<String> RUN_EXPECTED_KEYS = Set.of(
    "trainNoLocal",
    "trainNoInternational",
    "trainName",
    "startStation",
    "startsAt",
    "endStation",
    "endsAt",
    "locoType",
    "trainLength",
    "trainWeight",
    "continuesAs",
    "runId",
    "timetable"
  );
  private static final Set<String> TIMETABLE_EXPECTED_KEYS = Set.of(
    "nameOfPoint",
    "nameForPerson",
    "pointId",
    "supervisedBy",
    "radioChanels",
    "displayedTrainNumber",
    "arrivalTime",
    "departureTime",
    "stopType",
    "line",
    "platform",
    "track",
    "trainType",
    "mileage",
    "maxSpeed",
    "stationCategory"
  );
  private static final Set<String> STOP_TYPES_EXPECTED = Set.of(
    "NoStopOver",
    "CommercialStop",
    "NoncommercialStop"
  );

  @Test
  void testTimetableKeys() throws Exception {
    var client = HttpClient.newHttpClient();
    var request = HttpRequest.newBuilder()
      .GET()
      .uri(URI.create("https://api1.aws.simrail.eu:8082/api/getAllTimetables?serverCode=de1"))
      .build();
    var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
    Assertions.assertEquals(200, response.statusCode());

    var node = new JsonMapper().readTree(response.body());
    var runs = Assertions.assertInstanceOf(ArrayNode.class, node);
    for (var run : runs) {
      var runObject = Assertions.assertInstanceOf(ObjectNode.class, run);
      JsonFieldValidator.assertContainsAllKeys(runObject, RUN_EXPECTED_KEYS);

      var timetableEntries = Assertions.assertInstanceOf(ArrayNode.class, runObject.get("timetable"));
      for (var entry : timetableEntries) {
        var entryObject = Assertions.assertInstanceOf(ObjectNode.class, entry);
        JsonFieldValidator.assertContainsAllKeys(entryObject, TIMETABLE_EXPECTED_KEYS);

        // special check for stop type
        var stopTypeNode = Assertions.assertInstanceOf(TextNode.class, entryObject.get("stopType"));
        Assertions.assertTrue(STOP_TYPES_EXPECTED.contains(stopTypeNode.asText()));
      }
    }
  }
}
