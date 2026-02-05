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
import org.jspecify.annotations.NonNull;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/**
 * Handler for validation exceptions.
 */
@Order(0)
@RestControllerAdvice
final class ValidationExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public @NonNull ResponseEntity<ProblemDetail> handleMethodArgumentNotValidException(
    @NonNull MethodArgumentNotValidException exception
  ) {
    var formattedFieldErrors = exception.getBindingResult().getFieldErrors()
      .stream()
      .map(fieldError -> {
        var fieldErrorProperties = new LinkedHashMap<>();
        fieldErrorProperties.put("property", fieldError.getField());
        fieldErrorProperties.put("value", fieldError.getRejectedValue());
        fieldErrorProperties.put("message", fieldError.getDefaultMessage());
        return fieldErrorProperties;
      })
      .toList();

    var pd = exception.getBody();
    pd.setProperty("errors", formattedFieldErrors);
    return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(pd);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public @NonNull ResponseEntity<ProblemDetail> handleHandlerMethodValidationException(
    @NonNull HandlerMethodValidationException exception
  ) {
    var formattedParamErrors = exception.getParameterValidationResults().stream()
      .map(validationResult -> {
        var errorProperties = new LinkedHashMap<>();
        errorProperties.put("property", validationResult.getMethodParameter().getParameterName());
        errorProperties.put("value", validationResult.getArgument());

        var firstError = CollectionUtils.firstElement(validationResult.getResolvableErrors());
        var errorMessage = firstError == null ? "validation failure" : firstError.getDefaultMessage();
        errorProperties.put("message", errorMessage);

        return errorProperties;
      })
      .toList();

    var pd = exception.getBody();
    pd.setProperty("errors", formattedParamErrors);
    return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(pd);
  }
}
