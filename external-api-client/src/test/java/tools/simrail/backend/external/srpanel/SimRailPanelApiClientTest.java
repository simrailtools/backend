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

package tools.simrail.backend.external.srpanel;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class SimRailPanelApiClientTest {

  @Test
  void testServers() {
    var client = SimRailPanelApiClient.create();
    var response = client.getServers();

    Assertions.assertNotNull(response);
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getEntries());
    Assertions.assertNotNull(response.getEntryCount());

    for (var entry : response.getEntries()) {
      Assertions.assertNotNull(entry.getId());
      Assertions.assertNotNull(entry.getCode());
      Assertions.assertNotNull(entry.getName());
      Assertions.assertNotNull(entry.getRegion());
    }
  }

  @Test
  void testTrains() {
    var client = SimRailPanelApiClient.create();
    var response = client.getTrains("de1");

    Assertions.assertNotNull(response);
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getEntries());
    Assertions.assertNotNull(response.getEntryCount());

    for (var entry : response.getEntries()) {
      Assertions.assertNotNull(entry.getId());
      Assertions.assertNotNull(entry.getName());
      Assertions.assertNotNull(entry.getNumber());
      Assertions.assertNotNull(entry.getRunId());

      var vehicles = entry.getVehicles();
      Assertions.assertNotNull(vehicles);
      Assertions.assertFalse(vehicles.isEmpty());

      Assertions.assertNotNull(entry.getDepartureStationName());
      Assertions.assertNotNull(entry.getDestinationStationName());

      var detailData = entry.getDetailData();
      Assertions.assertNotNull(detailData);
    }
  }

  @Test
  void testTrainPositions() {
    var client = SimRailPanelApiClient.create();
    var response = client.getTrainPositions("de1");

    Assertions.assertNotNull(response);
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getEntries());
    Assertions.assertNotNull(response.getEntryCount());

    for (var entry : response.getEntries()) {
      Assertions.assertNotNull(entry.getId());
    }
  }

  @Test
  void testDispatchPosts() {
    var client = SimRailPanelApiClient.create();
    var response = client.getDispatchPosts("de1");

    Assertions.assertNotNull(response);
    Assertions.assertTrue(response.isSuccess());
    Assertions.assertNotNull(response.getEntries());
    Assertions.assertNotNull(response.getEntryCount());

    for (var entry : response.getEntries()) {
      Assertions.assertNotNull(entry.getId());
      Assertions.assertNotNull(entry.getDispatchers());
      Assertions.assertNotNull(entry.getStationName());
      Assertions.assertNotNull(entry.getStationPrefix());
      Assertions.assertTrue(entry.getDifficulty() > 0);

      Assertions.assertNotNull(entry.getImage1Url());
      Assertions.assertNotNull(entry.getImage2Url());
      Assertions.assertNotNull(entry.getImage3Url());

      Assertions.assertNotEquals(0.0, entry.getPositionLatitude());
      Assertions.assertNotEquals(0.0, entry.getPositionLongitude());
    }
  }

  @Test
  void testUserInfo() {
    var client = SimRailPanelApiClient.create();
    var response = client.getUserInfo(List.of("76561198342066455"));

    Assertions.assertNotNull(response);
    Assertions.assertTrue(response.isSuccess());
    // Assertions.assertNotNull(response.getEntryCount()); // always null for some reason

    var entries = response.getEntries();
    Assertions.assertNotNull(entries);
    Assertions.assertEquals(1, entries.size());

    var firstEntry = entries.getFirst();
    Assertions.assertEquals("76561198342066455", firstEntry.getSteamUserId());
  }
}
