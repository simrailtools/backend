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

package tools.simrail.backend.common.railcar;

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Year;
import java.util.HashSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {
  RailcarProvider.class,
  JacksonAutoConfiguration.class,
})
public final class RailcarProviderTest {

  @Autowired
  private RailcarProvider railcarProvider;

  @Test
  void testAllRailcarsWereLoaded() {
    var railcars = this.railcarProvider.railcars;
    Assertions.assertEquals(102, railcars.size());
  }

  @Test
  void testAllRailcarIdsAreUnique() {
    var railcars = this.railcarProvider.railcars;
    var seenRailcarIds = new HashSet<>();
    for (var railcar : railcars) {
      var railcardId = railcar.getId();
      if (!seenRailcarIds.add(railcardId)) {
        Assertions.fail("Found duplicate railcard id: " + railcardId);
      }
    }
  }

  @Test
  void testAllRailcarApiIdsAreUnique() {
    var railcars = this.railcarProvider.railcars;
    var seenRailcarApiIds = new HashSet<>();
    for (var railcar : railcars) {
      var railcardApiId = railcar.getApiId();
      if (!seenRailcarApiIds.add(railcardApiId)) {
        Assertions.fail("Found duplicate railcard api id: " + railcardApiId);
      }
    }
  }

  @Test
  void testAllRailcarDisplayNamesAreUnique() {
    var railcars = this.railcarProvider.railcars;
    var seenRailcarApiIds = new HashSet<>();
    for (var railcar : railcars) {
      var railcardDisplayName = railcar.getDisplayName();
      if (!seenRailcarApiIds.add(railcardDisplayName)) {
        Assertions.fail("Found duplicate railcard display name id: " + railcardDisplayName);
      }
    }
  }

  @Test
  void testAllRailcardModelsAreValid() {
    var year1900 = Year.of(1900);
    var railcars = this.railcarProvider.railcars;
    for (var railcar : railcars) {
      Assertions.assertNotNull(railcar.getId());
      Assertions.assertNotNull(railcar.getApiId());
      Assertions.assertNotNull(railcar.getProducer());
      Assertions.assertNotNull(railcar.getDisplayName());
      Assertions.assertNotNull(railcar.getTypeGroupId());
      Assertions.assertNotNull(railcar.getDesignation());
      Assertions.assertNotNull(railcar.getRailcarType());
      Assertions.assertTrue(railcar.getWeight() > 10);
      Assertions.assertTrue(railcar.getLength() > 1);
      Assertions.assertTrue(railcar.getWidth() > 2);
      Assertions.assertTrue(railcar.getMaximumSpeed() >= 10);

      // validate the production years
      var productionYears = railcar.getProductionYears();
      Assertions.assertNotNull(productionYears);
      if (productionYears.contains("-")) {
        var parts = productionYears.split("-");
        Assertions.assertEquals(2, parts.length);
        var productionStart = Assertions.assertDoesNotThrow(() -> Year.parse(parts[0]));
        Assertions.assertTrue(productionStart.isAfter(year1900));

        if (parts[1].equals("Present")) {
          var productionEnd = Year.now();
          Assertions.assertTrue(productionStart.compareTo(productionEnd) <= 0);
        } else {
          var productionEnd = Assertions.assertDoesNotThrow(() -> Year.parse(parts[1]));
          Assertions.assertTrue(productionEnd.isAfter(year1900));
          Assertions.assertTrue(productionEnd.isAfter(productionStart));
        }
      } else {
        var productionYear = Assertions.assertDoesNotThrow(() -> Year.parse(productionYears));
        Assertions.assertTrue(productionYear.isAfter(year1900));
      }
    }
  }

  @Test
  void testRailcarsFromTrainListCanBeMapped() throws Exception {
    var client = HttpClient.newHttpClient();
    var request = HttpRequest.newBuilder()
      .GET()
      .uri(URI.create("https://panel.simrail.eu:8084/trains-open?serverCode=de1"))
      .build();
    var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
    Assertions.assertEquals(200, response.statusCode());

    var mapper = new JsonMapper();
    var responseObject = mapper.readTree(response.body());
    var activeTrains = responseObject.get("data");
    Assertions.assertTrue(activeTrains.isArray());
    for (var activeTrain : activeTrains) {
      var trainRailcars = activeTrain.get("Vehicles");
      for (var trainRailcar : trainRailcars) {
        var railcarDescriptor = trainRailcar.asText();
        var railcardApiId = railcarDescriptor.split(":")[0];
        var railcar = this.railcarProvider.findRailcarByApiId(railcardApiId);
        Assertions.assertTrue(railcar.isPresent(), "Missing railcar " + railcardApiId);
      }
    }
  }
}
