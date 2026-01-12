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

package tools.simrail.backend.common.scenery;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {
  JacksonAutoConfiguration.class,
  SimRailServerSceneryProvider.class,
})
public class SimRailServerSceneryProviderTest {

  @Autowired
  private ObjectMapper objectMapper;
  @Autowired
  private SimRailServerSceneryProvider sceneryProvider;

  @Test
  void testAllSceneriesWereLoaded() {
    var sceneries = this.sceneryProvider.sceneryByServerId;
    Assertions.assertEquals(18, sceneries.size());
  }

  @Test
  void testSceneryExistsForAllServers() throws Exception {
    var client = HttpClient.newHttpClient();
    var request = HttpRequest.newBuilder()
      .GET()
      .uri(URI.create("https://panel.simrail.eu:8084/servers-open"))
      .build();
    var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
    Assertions.assertEquals(200, response.statusCode());

    var responseObject = this.objectMapper.readTree(response.body());
    var servers = responseObject.get("data");
    Assertions.assertFalse(servers.isEmpty());

    for (var server : servers) {
      var serverId = server.get("id").asText();
      var scenery = this.sceneryProvider.findSceneryByServerId(serverId);
      Assertions.assertTrue(scenery.isPresent(), "missing scenery for " + serverId);
    }
  }
}
