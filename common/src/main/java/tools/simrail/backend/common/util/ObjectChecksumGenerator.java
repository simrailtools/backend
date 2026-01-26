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

import com.fasterxml.jackson.annotation.JsonInclude;
import java.io.OutputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Generates the checksum for some input object using a jackson mapper that produces reproducible JSON.
 */
public final class ObjectChecksumGenerator {

  // JSON mapper that writes values in a reproducible order (as good as possible)
  private static final JsonMapper JSON_MAPPER = JsonMapper.builder()
    .disable(SerializationFeature.INDENT_OUTPUT)
    .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
    .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
    .changeDefaultPropertyInclusion(value -> value.withValueInclusion(JsonInclude.Include.NON_NULL))
    .build();

  // an output stream instance that just does nothing. note that the builtin
  // OutputStream.nullOutputStream() variant isn't used because it always returns
  // a new stream & does bound checks when writing a byte array to it
  private static final OutputStream NULL_OUTPUT_STREAM = new OutputStream() {
    @Override
    public void write(int b) {
      // no-op
    }

    @Override
    public void write(byte @NonNull [] b, int off, int len) {
      // no-op
    }

    @Override
    public void close() {
      // no-op
    }
  };

  private ObjectChecksumGenerator() {
    throw new UnsupportedOperationException();
  }

  /**
   * Generates a checksum for the given object.
   *
   * @param value the object to generate the checksum for.
   * @return the checksum for the given object.
   */
  public static @NonNull String generateChecksum(@NonNull Object value) {
    try {
      var messageDigest = MessageDigest.getInstance("SHA-256");
      try (var digestOutputStream = new DigestOutputStream(NULL_OUTPUT_STREAM, messageDigest)) {
        JSON_MAPPER.writeValue(digestOutputStream, value);
      }

      var digest = messageDigest.digest();
      return HexFormat.of().formatHex(digest);
    } catch (Exception exception) {
      throw new RuntimeException("Failed to generate checksum for object", exception);
    }
  }
}
