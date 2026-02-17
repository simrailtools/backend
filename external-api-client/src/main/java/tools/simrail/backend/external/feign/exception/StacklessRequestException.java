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

package tools.simrail.backend.external.feign.exception;

import feign.Request;
import java.io.Serial;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Stackless exception to throw when an http request fails.
 */
public final class StacklessRequestException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 5818614088289966585L;

  private static final Pattern CONTROL_CHARS_PATTERN = Pattern.compile("\\p{C}");

  /**
   * Constructs a stackless request exception with a message based on the given request and original exception.
   *
   * @param request           the request that failed.
   * @param originalException the original exception that was thrown.
   */
  public StacklessRequestException(@NonNull Request request, @NonNull Throwable originalException) {
    var origExMsg = originalException.getMessage();
    var origExMsgCleaned = cleanExceptionMessage(origExMsg);
    var message = String.format(
      "Caught %s[message=%s] while executing Request[method=%s; uri=%s; ver=%s]",
      originalException.getClass().getSimpleName(), origExMsgCleaned,
      request.httpMethod(), request.url(), request.protocolVersion());
    super(message);
  }

  /**
   * Removes all control chars from the given string.
   *
   * @param message the string to remove all control chars from.
   * @return the string with all control chars removed, or {@code null} if the given string was {@code null}.
   */
  private static @Nullable String cleanExceptionMessage(@Nullable String message) {
    if (message == null || message.isBlank()) {
      return null;
    }

    return CONTROL_CHARS_PATTERN.matcher(message).replaceAll("");
  }

  @Override
  public @NonNull Throwable initCause(@Nullable Throwable cause) {
    return this;
  }

  @Override
  public @NonNull Throwable fillInStackTrace() {
    return this;
  }

  @Override
  public void setStackTrace(@NonNull StackTraceElement[] stackTrace) {
  }
}
