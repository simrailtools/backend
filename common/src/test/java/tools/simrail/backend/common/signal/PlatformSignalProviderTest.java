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

package tools.simrail.backend.common.signal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.simrail.backend.common.TimetableHolder;
import tools.simrail.backend.common.point.SimRailPointProvider;
import tools.simrail.backend.common.util.RomanNumberConverter;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = {
  SimRailPointProvider.class,
  PlatformSignalProvider.class,
  JacksonAutoConfiguration.class,
})
public class PlatformSignalProviderTest {

  @Autowired
  private SimRailPointProvider pointProvider;
  @Autowired
  private PlatformSignalProvider signalProvider;
  @Value("classpath:data/signals.json")
  private Resource signalsResource;

  static Stream<Arguments> signalPointSource() {
    return Stream.of(
      Arguments.of(UUID.fromString("27f5db2c-07a1-4c23-aaa5-dff759f687ba"), "Kz_G6"),
      Arguments.of(UUID.fromString("8c58048c-e3f9-4faf-8f1f-5ecf3e978e54"), "WSD_J20"),
      Arguments.of(UUID.fromString("8c58048c-e3f9-4faf-8f1f-5ecf3e978e54"), "WSD_J23"),
      Arguments.of(UUID.fromString("6840caa1-ef46-4cc4-93fb-0f26abf7c7d9"), "Zw_G2"),
      Arguments.of(UUID.fromString("a50dcebe-76b0-43d4-840e-6a8475c9756d"), "SG_N2")
    );
  }

  @Test
  void testAllSignalMappingsWereLoaded() {
    var signalCount = this.signalProvider.signalsByPointAndId.values()
      .stream()
      .mapToInt(Map::size)
      .sum();
    Assertions.assertEquals(553, signalCount);
  }

  @Test
  void testAllSignalNamesAreUnique() throws IOException {
    var jsonMapper = new JsonMapper();
    var seenSignalIdsByPoint = new HashMap<String, Set<String>>();
    try (var stream = this.signalsResource.getInputStream()) {
      var signals = jsonMapper.readTree(stream);
      for (var signal : signals) {
        var signalId = signal.get("id").asString();
        var pointId = signal.get("point_id").asString();
        Assertions.assertFalse(signalId.isBlank());
        Assertions.assertFalse(pointId.isBlank());

        var seenSignalIds = seenSignalIdsByPoint.computeIfAbsent(pointId, _ -> new HashSet<>());
        Assertions.assertTrue(seenSignalIds.add(signalId), signalId);

        var track = signal.get("track").asInt();
        var platform = signal.get("platform").asInt();
        Assertions.assertTrue(track > 0, signalId);
        Assertions.assertTrue(platform > 0, signalId);
      }
    }
  }

  @ParameterizedTest
  @MethodSource("signalPointSource")
  void testFindSignal(UUID pointId, String signalId) {
    var signalInfo = this.signalProvider.findSignalInfo(pointId, signalId);
    Assertions.assertTrue(signalInfo.isPresent());
  }

  @Test
  void testAllScheduledPlatformsHaveASignalMapping() throws IOException {
    var pointsWithWrongPlatformMapping = Set.of(
      "Olkusz", // wrong platform mapping
      "Gałkówek", // wrong platform mapping
      "Sędziszów", // wrong platform mapping
      "Opoczno Południe", // wrong platform mapping
      "Dąbrowa Górnicza", // wrong platform mapping
      "Dąbrowa Górnicza Gołonóg" // wrong platform mapping
    );
    var pointsWithMultipleSignalsForSameTrack = Set.of(
      "Żyrardów" // Track 1 Platform 1 can be reached from 3 signals
    );

    var missingPoints = new HashSet<String>();
    var trainRuns = TimetableHolder.getDefaultServerTimetable();
    for (var trainRun : trainRuns) {
      var timetable = (ArrayNode) trainRun.get("timetable");
      for (var timetableEntry : timetable) {
        var stopType = timetableEntry.get("stopType").asString();
        var pointName = timetableEntry.get("nameOfPoint").asString();
        var platformString = timetableEntry.get("platform").stringValue(null);
        if (stopType.equals("CommercialStop")
          && platformString != null
          && !missingPoints.contains(pointName)
          && !pointsWithWrongPlatformMapping.contains(pointName)) {
          // get the point associated with the stop
          var pointId = timetableEntry.get("pointId").asString();
          var point = this.pointProvider.findPointByPointId(pointId);
          Assertions.assertTrue(point.isPresent(), "Missing point " + pointId);

          // get the track information of the stop and the signal info for the point
          var track = timetableEntry.get("track").asInt();
          var platform = RomanNumberConverter.decodeRomanNumber(platformString);
          var signalsOfPoint = this.signalProvider.signalsByPointAndId.get(point.get().getId());
          if (signalsOfPoint == null) {
            missingPoints.add(pointName);
            continue;
          }

          // find the signal info for the track and validate it's present
          var signalInfo = signalsOfPoint.values().stream()
            .filter(signal -> signal.getTrack() == track && signal.getPlatform() == platform)
            .toList();
          if (pointsWithMultipleSignalsForSameTrack.contains(pointName)) {
            Assertions.assertFalse(signalInfo.isEmpty(), point.get().getName());
          } else {
            Assertions.assertTrue(
              !signalInfo.isEmpty() && signalInfo.size() <= 2,
              () -> String.format("Bad mapping for %s [P: %s, T: %s]", pointName, platform, track));
          }
        }
      }
    }

    var knownMissingMappings = Files.readAllLines(Path.of("src/test/resources/missing_signal_mappings.txt"));
    knownMissingMappings.forEach(missingPoints::remove);
    Assertions.assertTrue(missingPoints.isEmpty(), () -> {
      var joinedStationNames = String.join(", ", missingPoints);
      return "Found unexpected count of stations without platform signal info: " + joinedStationNames;
    });
  }
}
