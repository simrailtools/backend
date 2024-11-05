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

package tools.simrail.backend.common.border;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * A provider for the border points of the SimRail map.
 */
@Service
public final class MapBorderPointProvider {

  // visible for testing only
  final Set<String> mapBorderPointIds;

  @Autowired
  public MapBorderPointProvider(
    @Nonnull ObjectMapper objectMapper,
    @Nonnull @Value("classpath:data/border_points.json") Resource borderPointsResource
  ) throws IOException {
    // deserialize the border points from the points json bundled in the application jar file
    var borderPointsType = objectMapper.getTypeFactory().constructCollectionType(List.class, MapBorderPoint.class);
    try (var inputStream = borderPointsResource.getInputStream()) {
      List<MapBorderPoint> points = objectMapper.readValue(inputStream, borderPointsType);
      this.mapBorderPointIds = points.stream().map(MapBorderPoint::getSimRailPointId).collect(Collectors.toSet());
    }
  }

  /**
   * Get if the given point id is at the border of the playable SimRail map.
   *
   * @param simRailPointId the id of the point to check.
   * @return true if the given point id is at the border of the playable SimRail map, false otherwise.
   */
  public boolean isMapBorderPoint(@Nonnull String simRailPointId) {
    return this.mapBorderPointIds.contains(simRailPointId);
  }
}
