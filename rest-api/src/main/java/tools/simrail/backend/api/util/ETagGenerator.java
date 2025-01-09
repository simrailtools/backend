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

package tools.simrail.backend.api.util;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.util.function.Function;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

/**
 * Generator function implementation for an E-Tag header value based on spring resources.
 */
@Component
public final class ETagGenerator implements Function<Resource, String> {

  @Override
  public @Nonnull String apply(@Nonnull Resource resource) {
    try (var inputStream = resource.getInputStream()) {
      // 35 is the length of "0 + 32bits md5 hash + "
      var stringBuilder = new StringBuilder(35);
      stringBuilder.append("\"0");
      DigestUtils.appendMd5DigestAsHex(inputStream, stringBuilder);
      stringBuilder.append('"');
      return stringBuilder.toString();
    } catch (IOException exception) {
      throw new IllegalStateException("Unable to generate ETag for resource", exception);
    }
  }
}
