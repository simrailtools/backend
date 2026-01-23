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

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.simrail.backend.external.brouter.request.BRouterRouteRequest;

public final class BRouterApiClientTest {

  @Test
  void testRouting() {
    var client = BRouterApiClient.create();
    var jsonMapper = JsonMapper.builder().build();
    var waypoints = List.of(
      new BRouterRouteRequest.GeoPosition(50.481008999348, 19.4231231),
      new BRouterRouteRequest.GeoPosition(50.5844056993271, 19.467981),
      new BRouterRouteRequest.GeoPosition(50.7348405992977, 19.8195589),
      new BRouterRouteRequest.GeoPosition(50.8009137992852, 19.9048019),
      new BRouterRouteRequest.GeoPosition(50.856394699275, 19.9460452),
      new BRouterRouteRequest.GeoPosition(51.1001588992325, 20.0686251));
    var routeRequest = BRouterRouteRequest.create()
      .withProfile("rail")
      .withPositions(waypoints)
      .withOutputFormat(BRouterRouteRequest.OutputFormat.GEOJSON);
    var data = Assertions.assertDoesNotThrow(() -> client.route(routeRequest));
    var parsed = Assertions.assertDoesNotThrow(() -> jsonMapper.readTree(data));
    Assertions.assertEquals("FeatureCollection", parsed.path("type").asString());
    Assertions.assertTrue(parsed.has("features"));
  }
}
