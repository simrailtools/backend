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

package tools.simrail.backend.collector.journey;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class TrainNameParserTest {

  static Stream<Arguments> categoryTestArguments() {
    return Stream.of(
      Arguments.of("", new TrainNameParser.Result(null, null, null)),
      Arguments.of("  ", new TrainNameParser.Result(null, null, null)),
      Arguments.of(" - RE1", new TrainNameParser.Result(null, "RE1", null)),
      Arguments.of("R", new TrainNameParser.Result("R", null, null)),
      Arguments.of("IR", new TrainNameParser.Result("IR", null, null)),
      Arguments.of("TSE", new TrainNameParser.Result("TSE", null, null)),
      Arguments.of("ZXE", new TrainNameParser.Result("ZXE", null, null)),
      Arguments.of("RE1", new TrainNameParser.Result(null, "RE1", null)),
      Arguments.of("RAJ - R3", new TrainNameParser.Result("RAJ", "R3", null)),
      Arguments.of("ROJ - S1", new TrainNameParser.Result("ROJ", "S1", null)),
      Arguments.of("RPJ - S41", new TrainNameParser.Result("RPJ", "S41", null)),
      Arguments.of("MPE – \"Łukasz\"", new TrainNameParser.Result("MPE", null, "Łukasz")),
      Arguments.of("ECE -  \"Růžena\"", new TrainNameParser.Result("ECE", null, "Růžena")),
      Arguments.of("ECE - \"Ludmila\"", new TrainNameParser.Result("ECE", null, "Ludmila")),
      Arguments.of("ECE - \"Zdeněk\"\"", new TrainNameParser.Result("ECE", null, "Zdeněk"))
    );
  }

  @ParameterizedTest
  @MethodSource("categoryTestArguments")
  void testTrainNameParsing(String trainName, TrainNameParser.Result expectedResult) {
    var actualResult = TrainNameParser.parseTrainName(trainName);
    Assertions.assertEquals(expectedResult, actualResult);
  }
}
