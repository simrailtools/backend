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

package tools.simrail.backend.common.border;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * A provider for the border points of the SimRail map.
 */
@Service
public final class MapBorderPointProvider {

  // visible for testing only
  final Map<String, MapBorderPoint> mapBorderPointIds;

  @Autowired
  public MapBorderPointProvider(
    @NonNull ObjectMapper objectMapper,
    @NonNull @Value("classpath:data/border_points.json") Resource borderPointsResource
  ) throws IOException {
    // deserialize the border points from the points JSON bundled in the application jar file
    var borderPointsType = objectMapper.getTypeFactory().constructCollectionType(List.class, MapBorderPoint.class);
    try (var inputStream = borderPointsResource.getInputStream()) {
      List<MapBorderPoint> points = objectMapper.readValue(inputStream, borderPointsType);

      this.mapBorderPointIds = new HashMap<>();
      for (var point : points) {
        point.getSimRailPointIds().forEach(id -> this.mapBorderPointIds.put(id, point));
      }
    }
  }

  /**
   * Get if the given point id is at the border of the playable SimRail map.
   *
   * @param simRailPointId the id of the point to check.
   * @return true if the given point id is at the border of the playable SimRail map, false otherwise.
   */
  public boolean isMapBorderPoint(@NonNull String simRailPointId) {
    return this.mapBorderPointIds.containsKey(simRailPointId);
  }

  /**
   * Finds a border point mapping for the given point id.
   *
   * @param simRailPointId the id of the point to get the border point mapping of.
   * @return an optional holding the map border point for the given point id if one exists.
   */
  public @NonNull Optional<MapBorderPoint> findMapBorderPoint(@NonNull String simRailPointId) {
    return Optional.ofNullable(this.mapBorderPointIds.get(simRailPointId));
  }
}
