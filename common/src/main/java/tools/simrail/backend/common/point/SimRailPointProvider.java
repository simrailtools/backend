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

package tools.simrail.backend.common.point;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.simrail.backend.common.util.GeometryConstants;

/**
 * A service that provides information about points based on the bundles data/points.json file.
 */
@Service
public final class SimRailPointProvider {

  // visible for testing only
  final List<SimRailPoint> points;

  @Autowired
  public SimRailPointProvider(
    @Nonnull ObjectMapper objectMapper,
    @Value("classpath:data/points.json") Resource pointsResource
  ) throws IOException {
    // deserialize the points from the points json bundled in the application jar file
    var pointListType = objectMapper.getTypeFactory().constructCollectionType(List.class, SimRailPoint.class);
    try (var inputStream = pointsResource.getInputStream()) {
      this.points = objectMapper.readValue(inputStream, pointListType);
    }
  }

  /**
   * Finds a single point based on the given internal point id.
   *
   * @param id the internal id of the point to get.
   * @return an optional holding the point if one with the given internal id exists.
   */
  public @Nonnull Optional<SimRailPoint> findPointByIntId(@Nonnull UUID id) {
    return this.points.stream().filter(point -> point.getId().equals(id)).findFirst();
  }

  /**
   * Finds a single point based on the given SimRail point id.
   *
   * @param pointId the SimRail point id of the point to get.
   * @return an optional holding the point if one with the given SimRail point id exists.
   */
  public @Nonnull Optional<SimRailPoint> findPointByPointId(@Nonnull String pointId) {
    return this.points.stream().filter(point -> point.getSimRailPointIds().contains(pointId)).findFirst();
  }

  /**
   * Finds a single point based on the given point name.
   *
   * @param name the name of the point to get.
   * @return an optional holding the point if one with the given name exists.
   */
  public @Nonnull Optional<SimRailPoint> findPointByName(@Nonnull String name) {
    return this.points.stream().filter(point -> point.getName().equals(name)).findFirst();
  }

  /**
   * Finds a single point whose bounding box contains the given geo coordinate.
   *
   * @param longitude the longitude that should be in the bounding box of the point.
   * @param latitude  the latitude that should be in the bounding box of the point.
   * @return an optional holding the point whose bounding box contains the given coordinate if one exists.
   */
  public @Nonnull Optional<SimRailPoint> findPointWherePosInBounds(double longitude, double latitude) {
    var pointCoordinate = new Coordinate(longitude, latitude);
    var geoPoint = GeometryConstants.GEOMETRY_FACTORY.createPoint(pointCoordinate);
    return this.points.stream().filter(point -> point.getBoundingBox().contains(geoPoint)).findFirst();
  }
}
