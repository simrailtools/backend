/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024-2026 Pasqual Koschmieder and contributors
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

package tools.simrail.backend.collector.util;

import org.jspecify.annotations.Nullable;
import tools.simrail.backend.common.proto.EventBusProto;

/**
 * Factory to construct a user instance.
 */
public final class UserFactory {

  private UserFactory() {
    throw new UnsupportedOperationException();
  }

  /**
   * Constructs a user instance from the given input parameters. The input parameters must be groups of a user platform
   * and the user id string.
   *
   * @param params the input parameters to construct the user from.
   * @return the constructed user, possibly null.
   */
  public static EventBusProto.@Nullable User constructUser(@Nullable Object... params) {
    if (params.length % 2 != 0) {
      throw new IllegalArgumentException("Params size must be a power of 2");
    }

    for (int index = 0; index < params.length; index += 2) {
      var platform = (EventBusProto.UserPlatform) params[index];
      var userId = (String) params[index + 1];
      if (userId != null && !userId.equals("null")) {
        return EventBusProto.User.newBuilder().setPlatform(platform).setId(userId).build();
      }
    }

    return null;
  }
}
