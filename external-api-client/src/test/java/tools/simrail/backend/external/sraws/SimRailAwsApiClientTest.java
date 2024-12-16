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

package tools.simrail.backend.external.sraws;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.function.Predicate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.simrail.backend.external.sraws.model.SimRailAwsTimetableEntry;

public final class SimRailAwsApiClientTest {

  @Test
  void testServerTimeOffset() {
    var client = SimRailAwsApiClient.create();
    var de1Offset = client.getServerTimeOffset("de1");
    Assertions.assertTrue(de1Offset == 1 || de1Offset == 2);

    var pl1Offset = client.getServerTimeOffset("pl1");
    Assertions.assertEquals(0, pl1Offset);
  }

  @Test
  void testServerTimeMillis() {
    var client = SimRailAwsApiClient.create();

    var de1Offset = client.getServerTimeOffset("de1");
    var de1ZoneOffset = ZoneOffset.ofHours(de1Offset);

    var de1Time = client.getServerTimeMillis("de1");
    var instant = Instant.ofEpochMilli(de1Time);
    var convertedTime = OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    var inTimezone = convertedTime.withOffsetSameLocal(de1ZoneOffset);
    Assertions.assertTrue(inTimezone.getYear() >= 2024);
  }

  @Test
  void testTrainRuns() {
    var client = SimRailAwsApiClient.create();
    var de1TrainRuns = client.getTrainRuns("de1");
    Assertions.assertNotNull(de1TrainRuns);
    Assertions.assertFalse(de1TrainRuns.isEmpty());

    for (var trainRun : de1TrainRuns) {
      Assertions.assertNotNull(trainRun.getRunId());
      Assertions.assertNotNull(trainRun.getTrainDisplayName());
      Assertions.assertFalse(trainRun.getTrainDisplayName().isEmpty());
      Assertions.assertNotNull(trainRun.getTrainNumber());
      Assertions.assertDoesNotThrow(() -> Integer.parseInt(trainRun.getTrainNumber()));
      Assertions.assertNotNull(trainRun.getTrainNumberInternational());
      Assertions.assertTrue(trainRun.getTrainNumberInternational().isEmpty());
      Assertions.assertNotNull(trainRun.getContinuationTrainNumber());
      Assertions.assertTrue(trainRun.getContinuationTrainNumber().isEmpty());
      Assertions.assertTrue(trainRun.getLength() > 0);
      Assertions.assertTrue(trainRun.getWeight() > 0);
      // todo: re-enable when SimRail fixed this in their timetable
      // Assertions.assertNotNull(trainRun.getTractionUnitName());
      // Assertions.assertFalse(trainRun.getTractionUnitName().isEmpty());

      var timetable = trainRun.getTimetable();
      Assertions.assertNotNull(timetable);
      Assertions.assertFalse(timetable.isEmpty());
      for (int index = 0; index < timetable.size(); index++) {
        var timetableEntry = timetable.get(index);
        Assertions.assertNotNull(timetableEntry.getTrainType());
        Assertions.assertFalse(timetableEntry.getTrainType().isEmpty());
        Assertions.assertNotNull(timetableEntry.getTrainNumber());
        Assertions.assertFalse(timetableEntry.getTrainNumber().isEmpty());
        Assertions.assertNotNull(timetableEntry.getPointId());
        Assertions.assertFalse(timetableEntry.getPointId().isEmpty());
        Assertions.assertNotNull(timetableEntry.getPointName());
        Assertions.assertFalse(timetableEntry.getPointName().isEmpty());
        Assertions.assertNotNull(timetableEntry.getPrettyPointName());
        Assertions.assertFalse(timetableEntry.getPrettyPointName().isEmpty());
        Assertions.assertFalse(Double.isNaN(timetableEntry.getPointMileage()));
        Assertions.assertNotNull(timetableEntry.getStopType());
        Assertions.assertTrue(timetableEntry.getDepartureLine() > 0);
        Assertions.assertTrue(timetableEntry.getTrainMaxAllowedSpeed() >= 0);

        // all entries must have an arrival and departure time, where the departure is after the arrival
        // except the first and last entry, they might not have one
        var isLastEntry = index == (timetable.size() - 1);
        Assertions.assertTrue(index == 0 || timetableEntry.getArrivalTime() != null);
        Assertions.assertTrue(isLastEntry || timetableEntry.getDepartureTime() != null);
        if (index > 0 && !isLastEntry) {
          var arrivalTime = timetableEntry.getArrivalTime();
          var departureTime = timetableEntry.getDepartureTime();
          Assertions.assertTrue(departureTime.isAfter(arrivalTime) || departureTime.isEqual(arrivalTime));
        }

        // either both or none of these information should be present
        var stopTrack = timetableEntry.getStopTrack();
        var stopPlatform = timetableEntry.getStopPlatform();
        Assertions.assertTrue((stopTrack == null && stopPlatform == null)
          || (stopTrack != null && stopPlatform != null));
        if (timetableEntry.getStopType() == SimRailAwsTimetableEntry.StopType.PH) {
          Assertions.assertNotNull(timetableEntry.getStationCategory());
          var stationCategory = timetableEntry.getStationCategory();
          Assertions.assertNotNull(stationCategory);
          Assertions.assertEquals(1, stationCategory.length());
          var stationCategoryChar = stationCategory.charAt(0);
          Assertions.assertTrue(stationCategoryChar >= 'A' && stationCategoryChar <= 'E');
        }

        // radio channel should either be a single space, one channel or multiple split with a comma
        Predicate<String> isValidChannel = c -> c.length() == 2
          && c.charAt(0) == 'R'
          && Character.isDigit(c.charAt(1));
        var rc = timetableEntry.getRadioChannels();
        Assertions.assertTrue((rc.isEmpty() || rc.equals(" "))
          || isValidChannel.test(rc)
          || (rc.contains(",") && Arrays.stream(rc.split(", ")).allMatch(isValidChannel)));
      }
    }
  }
}
