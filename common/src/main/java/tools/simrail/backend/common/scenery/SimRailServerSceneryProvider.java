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

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import tools.simrail.backend.common.server.SimRailServerScenery;

@Service
public final class SimRailServerSceneryProvider {

  // visible for testing only
  final Map<String, SimRailServerScenery> sceneryByServerId;

  @Autowired
  public SimRailServerSceneryProvider(
    @NonNull ObjectMapper objectMapper,
    @Value("classpath:data/server_scenery.json") Resource sceneryResource
  ) throws IOException {
    var serverIdToSceneryMapType = objectMapper.getTypeFactory()
      .constructMapType(Map.class, String.class, SimRailServerScenery.class);
    try (var inputStream = sceneryResource.getInputStream()) {
      this.sceneryByServerId = objectMapper.readValue(inputStream, serverIdToSceneryMapType);
    }
  }

  /**
   * Get the scenery of a server using the server id provided by the SimRail backend.
   *
   * @param serverId the SimRail id of the server to get the scenery of.
   * @return an optional holding the scenery of the server if one exists.
   */
  public @NonNull Optional<SimRailServerScenery> findSceneryByServerId(@NonNull String serverId) {
    return Optional.ofNullable(this.sceneryByServerId.get(serverId));
  }
}
