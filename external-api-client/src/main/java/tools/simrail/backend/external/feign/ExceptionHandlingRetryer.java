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

import feign.RetryableException;
import feign.Retryer;
import java.net.SocketException;
import java.net.http.HttpTimeoutException;
import javax.net.ssl.SSLException;
import org.jspecify.annotations.NonNull;
import tools.simrail.backend.external.feign.exception.StacklessRequestException;

/**
 * Exception handler that prevents specific connection-related exceptions (e.g., timeouts or ssl issues) from printing
 * huge stacktraces.
 */
public final class ExceptionHandlingRetryer implements Retryer {

  public static final ExceptionHandlingRetryer INSTANCE = new ExceptionHandlingRetryer();

  private ExceptionHandlingRetryer() {
  }

  @Override
  public void continueOrPropagate(@NonNull RetryableException exception) {
    var cause = exception.getCause();
    if (cause instanceof HttpTimeoutException
      || cause instanceof SocketException
      || cause instanceof SSLException) {
      throw new StacklessRequestException(exception.request(), cause);
    }

    throw exception;
  }

  @Override
  public @NonNull Retryer clone() {
    return INSTANCE;
  }
}
