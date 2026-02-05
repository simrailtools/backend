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

package tools.simrail.backend.api.exception;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.server.ResponseStatusException;

/**
 * Common class to use for all custom error responses.
 */
abstract class ErrorResponseException extends ResponseStatusException {

  private final Map<String, Object> properties = new LinkedHashMap<>(2, 0.9f);

  ErrorResponseException(@NonNull HttpStatusCode status, @NonNull String reason) {
    super(status, reason);
  }

  /**
   * Adds an extra property to this response, returned within the extensions block in the response.
   *
   * @param key   the key of the property.
   * @param value the value of the property.
   */
  protected final void setProperty(@NonNull String key, @Nullable Object value) {
    this.properties.put(key, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull ProblemDetail updateAndGetBody(@Nullable MessageSource messageSource, @NonNull Locale locale) {
    var problemDetail = super.updateAndGetBody(messageSource, locale);
    problemDetail.setProperty("extensions", this.properties);
    return problemDetail;
  }
}
