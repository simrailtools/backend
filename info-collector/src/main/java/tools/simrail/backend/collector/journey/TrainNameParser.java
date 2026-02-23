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

import java.util.regex.Pattern;
import lombok.With;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Parser for the train name of a timetable entry.
 */
final class TrainNameParser {

  /**
   * Pattern that matches on all whitespace chars.
   */
  private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");
  /**
   * Pattern for the possible delimiter chars between the train category and line/label. Note that the chars might seem
   * identical at first glance, but they're actually not.
   */
  private static final Pattern DELIMITER_CHAR_PATTERN = Pattern.compile("[-–]");
  /**
   * Pattern that matches on train categories (3 uppercase chars).
   */
  private static final Pattern TRAIN_CATEGORY_PATTERN = Pattern.compile("^[A-Z]{3}$");
  /**
   * Pattern that matches on train lines (starts with uppercase char, contains at least one number).
   */
  private static final Pattern TRAIN_LINE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9]*\\d[A-Z0-9]*$");

  private TrainNameParser() {
    throw new UnsupportedOperationException();
  }

  /**
   * Parses the given train name, extracting all possible data from it.
   *
   * @param trainName the name of the train to parse.
   * @return the parsed result from the given train name.
   */
  public static @NonNull Result parseTrainName(@NonNull String trainName) {
    // removes all whitespace from the name, this simplifies handling the string parts in the next step
    var cleanedName = WHITESPACE_PATTERN.matcher(trainName).replaceAll("");

    var result = Result.NULL;
    var trainNameParts = DELIMITER_CHAR_PATTERN.split(cleanedName);
    for (var part : trainNameParts) {
      // skip empty parts, these are always irrelevant
      if (part.isEmpty()) {
        continue;
      }

      // checks if the part is the train category
      var isCategory = TRAIN_CATEGORY_PATTERN.matcher(part).matches();
      if (isCategory) {
        result = result.withCategory(part);
        continue;
      }

      // check for a train label, these entries always start and end with a quotation mark
      var isLabel = part.startsWith("\"") && part.endsWith("\"");
      if (isLabel) {
        var label = part.replace("\"", ""); // to fix labels such as '\"Zdeněk\"\"'
        result = result.withLabel(label);
        continue;
      }

      // checks if the part is the train line
      var isLine = TRAIN_LINE_PATTERN.matcher(part).matches();
      if (isLine) {
        result = result.withLine(part);
        continue;
      }

      // if the part is nothing else, just assume it's the actual external category
      // we only apply this category if none other was matched on previously
      if (result.category() == null) {
        result = result.withCategory(part);
      }
    }

    return result;
  }

  /**
   * Parse result holder.
   *
   * @param category the category parsed from the name.
   * @param line     the line parsed from the name.
   * @param label    the label parsed from the name.
   */
  @With
  public record Result(@Nullable String category, @Nullable String line, @Nullable String label) {

    /**
     * Result which has all fields set to null.
     */
    private static final Result NULL = new Result(null, null, null);
  }
}
