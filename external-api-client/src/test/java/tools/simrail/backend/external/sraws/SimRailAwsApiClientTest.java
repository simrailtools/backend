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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
}
