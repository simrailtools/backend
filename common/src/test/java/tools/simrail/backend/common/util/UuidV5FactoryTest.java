/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024-2025 Pasqual Koschmieder and contributors
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

import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class UuidV5FactoryTest {

  static Stream<Arguments> uuidInputSource() {
    return Stream.of(
      Arguments.of("testing", UUID.fromString("7899701a-7d40-52c3-bf9c-c28419cab7e6")),
      Arguments.of("hello world", UUID.fromString("ccc93e04-5a2a-5691-a386-71c99fa4dc48")),
      Arguments.of("I love bananas", UUID.fromString("49d74ec8-1445-5ca4-9b57-6f83151f90d7")),
      Arguments.of("chi-hp-bungee107", UUID.fromString("68bd566c-ee84-5373-84cd-58ba3af9fe82")),
      Arguments.of("ROJ-40641-<hello>", UUID.fromString("44d6e7bd-9774-53eb-8198-746095c82ce7"))
    );
  }

  @ParameterizedTest
  @MethodSource("uuidInputSource")
  void testUuidV5Generation(String name, UUID expected) {
    var namespace = UUID.fromString("d32b76b2-d083-45d3-ab8f-d4d76398318b");
    var factory = new UuidV5Factory(namespace);
    var generatedUuid = factory.create(name);
    Assertions.assertEquals(expected, generatedUuid);
  }
}
