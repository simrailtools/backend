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

package tools.simrail.backend.common.util;

import org.locationtech.jts.geom.GeometryFactory;

public final class GeoUtil {

  /**
   * The JVM-static geometry factory to use for all geometry operations.
   */
  public static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
  /**
   * The radius of the earth in meters.
   */
  private static final double EARTH_RADIUS = 6_371_000;

  private GeoUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Calculates the distance between to geo points using the haversine formula.
   *
   * @param lat1 the latitude of the first point.
   * @param lon1 the longitude of the first point.
   * @param lat2 the latitude of the second point.
   * @param lon2 the longitude of the second point.
   * @return the distance between the 2 given points, in meters.
   */
  public static double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
    var lat1Rad = Math.toRadians(lat1);
    var lon1Rad = Math.toRadians(lon1);
    var lat2Rad = Math.toRadians(lat2);
    var lon2Rad = Math.toRadians(lon2);

    var deltaLat = lat2Rad - lat1Rad;
    var deltaLon = lon2Rad - lon1Rad;
    var a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
      + Math.cos(lat1Rad) * Math.cos(lat2Rad)
      * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
    var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return EARTH_RADIUS * c;
  }
}
