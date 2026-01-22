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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A utility to convert roman numbers to integers.
 */
public final class RomanNumberConverter {

  private RomanNumberConverter() {
    throw new UnsupportedOperationException();
  }

  /**
   * Converts the given roman number string into an integer. Any extra chars that cannot be mapped to a number are
   * ignored.
   *
   * @param romanNumber the romain number string to convert.
   * @return the integer representation of the given roman number string.
   */
  public static int decodeRomanNumber(@NonNull String romanNumber) {
    var result = 0;
    var prevValue = 0;

    // move from the last index of the string to the first
    // if the current number is smaller than the previous, then the value is subtracted from
    // the current result (e.g. IV), if the value is higher than the previous the value is added (e.g. VI)
    var lastStringIndex = romanNumber.length() - 1;
    for (var index = lastStringIndex; index >= 0; index--) {
      var c = romanNumber.charAt(index);
      var currValue = convertRomanChar(c);
      if (currValue == null) {
        // might happen if non-roman char is encountered, e.g. 'Ia' should just be read as 'I'
        continue;
      }

      if (currValue < prevValue) {
        result -= currValue;
      } else {
        result += currValue;
      }

      prevValue = currValue;
    }

    return result;
  }

  /**
   * Converts a single roman character to an integer.
   *
   * @param roman the roman character to convert to an integer.
   * @return the integer representation of the given roman character.
   */
  private static @Nullable Integer convertRomanChar(char roman) {
    return switch (roman) {
      case 'I' -> 1;
      case 'V' -> 5;
      case 'X' -> 10;
      case 'L' -> 50;
      case 'C' -> 100;
      case 'D' -> 500;
      case 'M' -> 1000;
      default -> null;
    };
  }
}
