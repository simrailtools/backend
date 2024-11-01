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

package tools.simrail.backend.external.util;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Assertions;

public final class JsonFieldValidator {

  public static void assertContainsAllKeys(ObjectNode node, Set<String> keyNames) {
    var fieldNames = node.fieldNames();
    var expectedFieldNames = new HashSet<>(keyNames);
    while (fieldNames.hasNext()) {
      var fieldName = fieldNames.next();
      if (!expectedFieldNames.remove(fieldName)) {
        // unknown field found
        Assertions.fail("Found unknown field " + fieldName + " in object");
      }
    }

    if (!expectedFieldNames.isEmpty()) {
      // a field has been removed
      Assertions.fail("Detected removed fields: " + String.join(", ", expectedFieldNames));
    }
  }
}
