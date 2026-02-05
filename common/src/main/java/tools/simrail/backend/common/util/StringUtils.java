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

import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;

/**
 * Various utilities for working with strings.
 */
public final class StringUtils {

  private static final Pattern NON_NUMBER_PATTERN = Pattern.compile("[^0-9]");

  private StringUtils() {
    throw new UnsupportedOperationException();
  }

  /**
   * Safely parses an int from a string, removing all non-number chars from the string before parsing. If the string
   * does not contain a single number, zero is returned instead.
   *
   * @param input the string to parse as a number.
   * @return the number parsed from the given input.
   */
  public static int parseIntSafe(@NonNull String input) {
    var cleanedInput = NON_NUMBER_PATTERN.matcher(input).replaceAll("");
    return cleanedInput.isEmpty() ? 0 : Integer.parseInt(cleanedInput);
  }

  /**
   * Get an enum constant with the given name, returning the given fallback if the constant does not exist. The enum
   * type is derived from the given fallback constant.
   *
   * @param name     the name of the enum constant to get.
   * @param fallback the fallback enum constant to return if no constant with the given name exists.
   * @param <T>      the enum type.
   * @return the enum constant with the given name or the given fallback if it does not exist.
   */
  public static @NonNull <T extends Enum<T>> T decodeEnum(@NonNull String name, @NonNull T fallback) {
    try {
      var enumType = fallback.getDeclaringClass();
      return Enum.valueOf(enumType, name);
    } catch (IllegalArgumentException _) {
      return fallback;
    }
  }
}
