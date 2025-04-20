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

package tools.simrail.backend.external.feign;

import feign.Response;
import java.util.Optional;
import java.util.SequencedCollection;
import org.jetbrains.annotations.NotNull;

/**
 * Tuple that contains both a feign response object and a deserialized JSON value from the response body.
 *
 * @param response the feign response.
 * @param body     the deserialized JSON body as a java POJO.
 * @param <T>      the type of the deserialized POJO from the response body.
 */
public record FeignJsonResponseTuple<T>(@NotNull Response response, @NotNull T body) {

  /**
   * Convenience method that returns an optional containing the first header string value of the given named (and
   * possibly multivalued) header. If the header is not present, then the returned optional is empty.
   *
   * @param name the name of the header.
   * @return an optional holding the first header value, empty if no such header exists.
   */
  public @NotNull Optional<String> firstHeaderValue(@NotNull String name) {
    var allValues = this.response.headers().get(name);
    if (allValues == null || allValues.isEmpty()) {
      // optimization: if no values are known for the name, there is no need to filter
      return Optional.empty();
    }

    if (allValues instanceof SequencedCollection<String> sc) {
      // use fast getFirst access for supported collection types
      return Optional.of(sc.getFirst());
    } else {
      // slow findFirst way, fallback
      return allValues.stream().findFirst();
    }
  }
}
