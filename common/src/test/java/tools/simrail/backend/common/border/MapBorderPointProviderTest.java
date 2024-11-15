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

import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.util.HashSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import tools.simrail.backend.common.TimetableHolder;
import tools.simrail.backend.common.point.SimRailPointProvider;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {
  SimRailPointProvider.class,
  MapBorderPointProvider.class,
  JacksonAutoConfiguration.class,
})
public final class MapBorderPointProviderTest {

  @Autowired
  private SimRailPointProvider pointProvider;
  @Autowired
  private MapBorderPointProvider borderPointProvider;
  @Value("classpath:data/border_points.json")
  private Resource borderPointsResource;

  @Test
  void testAllBorderPointsWereLoaded() {
    var borderPoints = this.borderPointProvider.mapBorderPointIds;
    Assertions.assertEquals(22, borderPoints.size());
  }

  @Test
  void testBorderPointIdsAreUnique() throws IOException {
    var jsonMapper = new JsonMapper();
    var seenPointIds = new HashSet<String>();
    try (var stream = this.borderPointsResource.getInputStream()) {
      var borderPoints = jsonMapper.readTree(stream);
      for (var borderPoint : borderPoints) {
        var pointIds = borderPoint.get("ext_point_ids");
        for (var pointIdNode : pointIds) {
          var pointId = pointIdNode.asText();
          if (!seenPointIds.add(pointId)) {
            Assertions.fail("Duplicate border point id: " + pointId);
          }
        }
      }
    }
  }

  @Test
  void testAllBorderPointsAreMappedUniquely() {
    var borderPoints = this.borderPointProvider.mapBorderPointIds;
    for (var borderPoint : borderPoints) {
      this.pointProvider.findPointByPointId(borderPoint).ifPresent(point -> {
        for (String pointId : point.getSimRailPointIds()) {
          Assertions.assertTrue(this.borderPointProvider.isMapBorderPoint(pointId), pointId);
        }
      });
    }
  }

  @Test
  void testAllTimetableEntriesHaveBorderPoints() {
    var trainRuns = TimetableHolder.getDefaultServerTimetable();
    for (var trainRun : trainRuns) {
      String startPointId = null;
      String endPointId = null;

      var timetable = trainRun.get("timetable");
      var trainRunId = trainRun.get("runId").asText();
      for (var timetableEntry : timetable) {
        var pointId = timetableEntry.get("pointId").asText();
        var isBorderPoint = this.borderPointProvider.isMapBorderPoint(pointId);
        if (isBorderPoint) {
          if (startPointId == null) {
            startPointId = pointId;
          } else {
            endPointId = pointId;
          }
        }
      }

      Assertions.assertNotNull(startPointId, trainRunId);
      Assertions.assertNotNull(endPointId, trainRunId);
      Assertions.assertNotEquals(startPointId, endPointId, trainRunId);
    }
  }
}
