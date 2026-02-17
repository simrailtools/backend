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

package tools.simrail.backend.api.advice;

import java.net.URI;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Advice to convert empty optional return values to 404 response status.
 */
@ControllerAdvice
final class EmptyOptionalToNotFoundResponseAdvice implements ResponseBodyAdvice<Object> {

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean supports(
    @NonNull MethodParameter returnType,
    @NonNull Class<? extends HttpMessageConverter<?>> converterType
  ) {
    return returnType.getParameterType() == Optional.class;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Object beforeBodyWrite(
    @Nullable Object body,
    @NonNull MethodParameter returnType,
    @NonNull MediaType selectedContentType,
    @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
    @NonNull ServerHttpRequest request,
    @NonNull ServerHttpResponse response
  ) {
    if (body instanceof Optional<?> optional && optional.isEmpty()) {
      var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "The requested resource does not exist");
      pd.setTitle("Not Found");
      pd.setInstance(URI.create(request.getURI().getPath()));
      response.setStatusCode(HttpStatus.NOT_FOUND);
      response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
      return pd;
    }

    return body;
  }
}
