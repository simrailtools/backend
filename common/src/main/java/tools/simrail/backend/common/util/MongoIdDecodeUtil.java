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

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.time.Instant;
import java.util.HexFormat;
import org.jspecify.annotations.NonNull;

/**
 * A utility to decode mongo ids.
 */
public final class MongoIdDecodeUtil {

  private static final VarHandle BYTE_ARRAY_INT_VIEW_HANDLE =
    MethodHandles.byteArrayViewVarHandle(Integer.TYPE.arrayType(), ByteOrder.BIG_ENDIAN);

  private MongoIdDecodeUtil() {
    throw new UnsupportedOperationException();
  }

  /**
   * Decodes a mongo object identifier to extract the embedded timestamp of it.
   *
   * @param mongoId the mongo id to extract the timestamp of.
   * @return the timestamp embedded in the given mongo id.
   */
  // id format: https://www.mongodb.com/docs/manual/reference/method/ObjectId/#description
  public static @NonNull Instant parseMongoId(@NonNull String mongoId) {
    var decodedTimePart = HexFormat.of().parseHex(mongoId, 0, 8);
    var secondsSinceEpoch = (int) BYTE_ARRAY_INT_VIEW_HANDLE.get(decodedTimePart, 0);
    return Instant.ofEpochSecond(secondsSinceEpoch);
  }
}
