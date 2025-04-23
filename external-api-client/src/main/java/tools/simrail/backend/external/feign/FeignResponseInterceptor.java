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

import feign.FeignException;
import feign.InvocationContext;
import feign.Response;
import feign.ResponseInterceptor;
import feign.Util;
import feign.codec.DecodeException;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Response interceptor that always calls request decoder in context.
 */
public final class FeignResponseInterceptor implements ResponseInterceptor {

  @Override
  public @Nullable Object intercept(@NotNull InvocationContext context, @NotNull Chain chain) {
    var decoder = context.decoder();
    var response = context.response();
    var returnType = context.returnType();

    // can quit early if response is requested as method return type
    if (returnType == Response.class) {
      return response;
    }

    try {
      return decoder.decode(response, returnType);
    } catch (FeignException feignException) {
      // pass feign exception directly through
      throw feignException;
    } catch (RuntimeException | IOException exception) {
      throw new DecodeException(response.status(), exception.getMessage(), response.request(), exception);
    } finally {
      Util.ensureClosed(response.body());
    }
  }
}
