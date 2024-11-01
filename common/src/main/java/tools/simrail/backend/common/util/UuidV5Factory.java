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

import jakarta.annotation.Nonnull;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * A utility to generate universally unique identifiers version 5. A UUIDv5 consists of a namespace uuid and an SHA-1
 * encoded name.
 */
public final class UuidV5Factory {

  private static final VarHandle BYTE_ARRAY_LONG_VIEW_HANDLE =
    MethodHandles.byteArrayViewVarHandle(Long.TYPE.arrayType(), ByteOrder.BIG_ENDIAN);

  private final byte[] namespace;

  /**
   * Constructs a new UUIDv5 factory for the given namespace uuid.
   *
   * @param namespace the uuid to use for namespacing.
   */
  public UuidV5Factory(@Nonnull UUID namespace) {
    this.namespace = encodeUuid(namespace);
  }

  /**
   * Encodes the given uuid into a byte array by putting the most significant bits to the index 0-7 and the least
   * significant bits to the index 8-15. The returned byte array always has a length of 16.
   *
   * @param uuid the uuid to encode into a byte array.
   * @return the given uuid encoded into a byte array.
   */
  public static byte[] encodeUuid(@Nonnull UUID uuid) {
    var result = new byte[16];
    BYTE_ARRAY_LONG_VIEW_HANDLE.set(result, 0, uuid.getMostSignificantBits());
    BYTE_ARRAY_LONG_VIEW_HANDLE.set(result, 8, uuid.getLeastSignificantBits());
    return result;
  }

  /**
   * Decodes an uuid from the given byte array, by reading the most significant bits from the index 0-7 and the least
   * significant bits from the index 8-15. The input byte array must have a length of at least 16.
   *
   * @param bytes the byte array to decode the uuid from.
   * @return the decoded byte array.
   */
  public static @Nonnull UUID decodeUuid(@Nonnull byte[] bytes) {
    var msb = (long) BYTE_ARRAY_LONG_VIEW_HANDLE.get(bytes, 0);
    var lsb = (long) BYTE_ARRAY_LONG_VIEW_HANDLE.get(bytes, 8);
    return new UUID(msb, lsb);
  }

  /**
   * Creates an SHA-1 message digest instance, throwing an exception in case the algorithm isn't available.
   *
   * @return a new SHA-1 message digest instance.
   * @throws IllegalStateException if the SHA-1 algorithm is unavailable.
   */
  private static @Nonnull MessageDigest createSha1MessageDigest() {
    try {
      return MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("Unable to obtain SHA-1 message digest", exception);
    }
  }

  /**
   * Creates a new UUIDv5 from the given name and the provided uuid namespace during construction.
   *
   * @param name the name to embed into the creates uuid.
   * @return an encoded uuid v5 based on the provided namespace and given name.
   */
  public @Nonnull UUID create(@Nonnull String name) {
    var nameBytes = name.getBytes(StandardCharsets.UTF_8);
    return this.createInternal(nameBytes);
  }

  /**
   * Internal helper method which actually constructs the uuid from the provided namespace and given name bytes.
   *
   * @param nameBytes the name bytes to embed into the uuid.
   * @return an encoded uuid v5 based on the provided namespace and given name bytes.
   */
  private @Nonnull UUID createInternal(byte[] nameBytes) {
    // put in namespace and name
    var digest = createSha1MessageDigest();
    digest.update(this.namespace);
    digest.update(nameBytes);

    // compute hash and set version & variant
    var hash = digest.digest();
    hash[6] = (byte) ((hash[6] & 0x0F) | 0x50); // set version to 5
    hash[8] = (byte) ((hash[8] & 0x3F) | 0x80); // set variant to IETF

    // byte array now contains the raw bytes to build an uuid, decode it from that
    return decodeUuid(hash);
  }
}
