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

package tools.simrail.backend.api.advice;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Optional;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Advice to convert empty optional return values to 404 response status.
 */
@ControllerAdvice
final class EmptyOptionalToNotFoundResponseAdvice implements ResponseBodyAdvice<Optional<?>> {

  @Override
  public boolean supports(
    @Nonnull MethodParameter returnType,
    @Nonnull Class<? extends HttpMessageConverter<?>> converterType
  ) {
    return Optional.class.isAssignableFrom(returnType.getParameterType());
  }

  @Override
  @SuppressWarnings("OptionalAssignedToNull")
  public @Nullable Optional<?> beforeBodyWrite(
    @Nullable Optional<?> body,
    @Nonnull MethodParameter returnType,
    @Nonnull MediaType selectedContentType,
    @Nonnull Class<? extends HttpMessageConverter<?>> selectedConverterType,
    @Nonnull ServerHttpRequest request,
    @Nonnull ServerHttpResponse response
  ) {
    if (body == null || body.isEmpty()) {
      response.setStatusCode(HttpStatus.NOT_FOUND);
      return null;
    } else {
      return body;
    }
  }
}
