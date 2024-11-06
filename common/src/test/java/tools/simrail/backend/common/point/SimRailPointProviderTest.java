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

import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import tools.simrail.backend.common.TimetableHolder;
import tools.simrail.backend.common.util.GeometryConstants;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {
  SimRailPointProvider.class,
  JacksonAutoConfiguration.class,
})
public final class SimRailPointProviderTest {

  @Autowired
  private SimRailPointProvider pointProvider;

  @Test
  void testPointsWereLoaded() {
    var points = this.pointProvider.points;
    Assertions.assertEquals(404, points.size());
  }

  @Test
  void testPointNamesAreUnique() {
    var points = this.pointProvider.points;
    var seenPointNames = new HashSet<String>();
    for (var point : points) {
      if (!seenPointNames.add(point.getName())) {
        Assertions.fail("Duplicate point name: " + point.getName());
      }
    }
  }

  @Test
  void testPointPrefixesAreUnique() {
    var points = this.pointProvider.points;
    var seenPrefixes = new HashSet<String>();
    for (var point : points) {
      var pointPrefix = point.getPrefix();
      if (pointPrefix != null && !seenPrefixes.add(pointPrefix)) {
        Assertions.fail("Duplicate point prefix: " + pointPrefix);
      }
    }
  }

  @Test
  void testPointOsmNodeIdsAreUnique() {
    var points = this.pointProvider.points;
    var seenOsmNodeIds = new HashSet<Long>();
    for (var point : points) {
      if (!seenOsmNodeIds.add(point.getOsmNodeId())) {
        Assertions.fail("Duplicate point OSM node id: " + point.getOsmNodeId());
      }
    }
  }

  @Test
  void testPointUicRefsAreUnique() {
    var points = this.pointProvider.points;
    var seenUicRefs = new HashSet<String>();
    for (var point : points) {
      var uicRef = point.getUicRef();
      if (uicRef != null && !seenUicRefs.add(uicRef)) {
        Assertions.fail("Duplicate point UIC ref: " + uicRef);
      }
    }
  }

  @Test
  void testAllPointsHaveAUniqueInternalId() {
    var points = this.pointProvider.points;
    var seenPointIds = new HashSet<UUID>();
    for (var point : points) {
      if (!seenPointIds.add(point.getId())) {
        Assertions.fail("Detected duplicate point internal id: " + point.getId());
      }
    }
  }

  @Test
  void testAllSimRailPointIdsAreOnlyAssociatedOnce() {
    var points = this.pointProvider.points;
    var seenPointIds = new HashSet<String>();
    for (var point : points) {
      var simRailPointIds = point.getSimRailPointIds();
      for (var simRailPointId : simRailPointIds) {
        if (!seenPointIds.add(simRailPointId)) {
          Assertions.fail("Detected duplicate SimRail point id mapping: " + simRailPointId);
        }
      }
    }
  }

  @Test
  void testPositionOfStationIsInBoundingBox() {
    var points = this.pointProvider.points;
    var allowedPoints = Set.of(
      "Bydgoszcz Wschód Towarowa"
    );
    for (var point : points) {
      if (!allowedPoints.contains(point.getName())) {
        var pointPosition = point.getPosition();
        var pointCoordinate = new Coordinate(pointPosition.getLongitude(), pointPosition.getLatitude());
        var geoPoint = GeometryConstants.GEOMETRY_FACTORY.createPoint(pointCoordinate);
        var pointInBB = point.getBoundingBox().contains(geoPoint);
        Assertions.assertTrue(pointInBB, "pos of point not in bb: " + point.getName());
      }
    }
  }

  @Test
  void testFindPointByInternalId() {
    var point = this.pointProvider.findPointByIntId(UUID.fromString("9474d33a-1f42-4e1e-8bf8-84258e4e3dfa"));
    Assertions.assertTrue(point.isPresent());
    Assertions.assertEquals("Katowice", point.get().getName());
  }

  @Test
  void testFindPointBySimRailPointId() {
    var point = this.pointProvider.findPointByPointId("5262");
    Assertions.assertTrue(point.isPresent());
    Assertions.assertEquals("Zawiercie", point.get().getName());
  }

  @Test
  void testFindPointByName() {
    var point = this.pointProvider.findPointByName("Warszawa Wschodnia");
    Assertions.assertTrue(point.isPresent());
    Assertions.assertEquals("Warszawa Wschodnia", point.get().getName());
  }

  @Test
  void testFindPointByPositionInBoundingBox() {
    var point = this.pointProvider.findPointWherePosInBounds(20.9989804, 52.2277704);
    Assertions.assertTrue(point.isPresent());
    Assertions.assertEquals("Warszawa Centralna", point.get().getName());
  }

  @Test
  void testAllTimetableEntriesHaveAPointMapping() {
    var missingPoints = new HashSet<String>();
    var trainRuns = TimetableHolder.getDefaultServerTimetable();
    for (var trainRun : trainRuns) {
      var timetable = (ArrayNode) trainRun.get("timetable");
      for (var timetableEntry : timetable) {
        var pointId = timetableEntry.get("pointId").asText();
        var pointName = timetableEntry.get("nameOfPoint").asText();
        var point = this.pointProvider.findPointByPointId(pointId);
        if (point.isEmpty()) {
          missingPoints.add(pointName);
        }
      }
    }

    Assertions.assertEquals(111, missingPoints.size(), () -> {
      var allMissingPointNames = String.join(", ", missingPoints);
      return "Found unexpected count of missing points: " + allMissingPointNames;
    });
  }
}