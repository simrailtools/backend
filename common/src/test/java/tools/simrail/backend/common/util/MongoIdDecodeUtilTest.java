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

package tools.simrail.backend.common.util;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class MongoIdDecodeUtilTest {

  static Stream<Arguments> mongoIdSource() {
    return Stream.of(
      Arguments.of("6390db9a9401bed7d6409dbb", OffsetDateTime.of(2022, 12, 7, 18, 29, 46, 0, ZoneOffset.UTC)),
      Arguments.of("638fe94cd089346098624eb3", OffsetDateTime.of(2022, 12, 7, 1, 15, 56, 0, ZoneOffset.UTC)),
      Arguments.of("638fd331b8d1724f7a1ce321", OffsetDateTime.of(2022, 12, 6, 23, 41, 37, 0, ZoneOffset.UTC)),
      Arguments.of("638f53a50938809ac3f1cf60", OffsetDateTime.of(2022, 12, 6, 14, 37, 25, 0, ZoneOffset.UTC)),
      Arguments.of("63c2fcabcacd60ea3e93f9c5", OffsetDateTime.of(2023, 1, 14, 19, 4, 11, 0, ZoneOffset.UTC))
    );
  }

  @ParameterizedTest
  @MethodSource("mongoIdSource")
  void testMongoIdDecode(String mongoId, OffsetDateTime expectedDate) {
    var parsedDate = MongoIdDecodeUtil.parseMongoId(mongoId);
    Assertions.assertEquals(expectedDate, parsedDate);
  }
}
