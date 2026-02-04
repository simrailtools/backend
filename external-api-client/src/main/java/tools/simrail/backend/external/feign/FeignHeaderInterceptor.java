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

import feign.RequestInterceptor;
import feign.RequestTemplate;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;

/**
 * Interceptor to add additional headers to a feign request.
 *
 * @param headers the headers to add the request.
 */
public record FeignHeaderInterceptor(@NonNull Map<String, String> headers) implements RequestInterceptor {

  /**
   * {@inheritDoc}
   */
  @Override
  public void apply(@NonNull RequestTemplate template) {
    for (var entry : this.headers.entrySet()) {
      template.header(entry.getKey(), List.of()); // drop the current header value
      template.header(entry.getKey(), entry.getValue());
    }
  }
}
