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

package tools.simrail.backend.api.exception;

import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Consumer;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Contains the global handlers for exceptions.
 */
@ControllerAdvice
public final class GlobalExceptionHandler {

  // different types of problems that are returned
  private static final URI NOT_FOUND = URI.create("not-found");
  private static final URI BAD_PARAMETER_TYPE = URI.create("bad-parameter");
  private static final URI INTERNAL_ERROR_TYPE = URI.create("internal-error");
  private static final URI CONSTRAINT_VIOLATION_TYPE = URI.create("constraint-violation");

  private final URI problemInstance;

  public GlobalExceptionHandler(@Value("${info.app.version:dev-local}") String applicationVersion) {
    this.problemInstance = URI.create("sit-di-" + applicationVersion);
  }

  /**
   * Prettifies the path of a constraint violation.
   *
   * @param propertyPath the path to be prettified.
   * @return the prettified path of the constraint violation.
   */
  private static @Nonnull String prettifyPropertyPath(@Nonnull Path propertyPath) {
    // construct a joiner and use an empty string as the default value for further checks
    var joiner = new StringJoiner(".");
    joiner.setEmptyValue("");

    for (var node : propertyPath) {
      // skip some types of node
      var kind = node.getKind();
      if (kind == ElementKind.METHOD
        || kind == ElementKind.CONSTRUCTOR
        || kind == ElementKind.CROSS_PARAMETER
        || kind == ElementKind.RETURN_VALUE) {
        continue;
      }

      // append the node if it helps for the path understanding
      var nodeAsString = node.toString();
      if (!nodeAsString.isBlank()) {
        joiner.add(nodeAsString);
      }
    }

    // return the default path in case no elements were added to the string joiner
    // note that this only works due to the fact that the empty value of the joiner is an empty string, as the joiner
    // returns the length of the empty value in case nothing was added
    if (joiner.length() == 0) {
      return propertyPath.toString();
    } else {
      return joiner.toString();
    }
  }

  /**
   * Formats the request URI of the given request to optionally include the query string.
   *
   * @param request the request to format the URI of.
   * @return the formatted URI of the given request.
   */
  private static @Nonnull String formatRequestUri(@Nonnull HttpServletRequest request) {
    var requestUri = request.getRequestURI();
    var queryString = request.getQueryString();
    if (queryString != null && !queryString.isBlank()) {
      requestUri += '?' + queryString;
    }

    return requestUri;
  }

  /**
   * Utility method to apply base configuration to a bad request problem detail.
   *
   * @param request   the request that caused the problem.
   * @param decorator a decorator function to apply additional properties to the problem detail.
   * @return the fully configured problem detail.
   */
  private @Nonnull ProblemDetail configureBadRequest(
    @Nonnull HttpServletRequest request,
    @Nonnull Consumer<ProblemDetail> decorator
  ) {
    var problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problemDetail.setTitle("Bad Request");
    problemDetail.setInstance(this.problemInstance);
    problemDetail.setProperty("timestamp", OffsetDateTime.now());
    problemDetail.setProperty("request-uri", formatRequestUri(request));
    decorator.accept(problemDetail);
    return problemDetail;
  }

  /**
   * Fallback exception handler for all exceptions that are not handled by any other handler.
   */
  @ExceptionHandler(Exception.class)
  public @Nonnull ProblemDetail handleUnhandledExceptions(@Nonnull HttpServletRequest request) {
    var problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    problemDetail.setType(INTERNAL_ERROR_TYPE);
    problemDetail.setTitle("Internal Server Error");
    problemDetail.setInstance(this.problemInstance);
    problemDetail.setProperty("timestamp", OffsetDateTime.now());
    problemDetail.setProperty("request-uri", formatRequestUri(request));
    problemDetail.setDetail("An internal error occurred while processing the request");
    return problemDetail;
  }

  /**
   * Handles the case when a requested resource does not exist and resolves it to a 404 response.
   */
  @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
  public @Nonnull ProblemDetail handleUnknownResource(@Nonnull HttpServletRequest request) {
    var problemDetail = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
    problemDetail.setType(NOT_FOUND);
    problemDetail.setTitle("Resource not found");
    problemDetail.setInstance(this.problemInstance);
    problemDetail.setProperty("timestamp", OffsetDateTime.now());
    problemDetail.setProperty("request-uri", formatRequestUri(request));
    problemDetail.setDetail("The requested resource does not exist");
    return problemDetail;
  }

  /**
   * Handler for constraint violations of requests.
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public @Nonnull ProblemDetail handleConstraintViolationException(
    @Nonnull ConstraintViolationException exception,
    @Nonnull HttpServletRequest request
  ) {
    return this.configureBadRequest(request, problemDetail -> {
      problemDetail.setType(CONSTRAINT_VIOLATION_TYPE);
      problemDetail.setDetail("Parameter(s) did not pass constraint validation");

      var violations = exception.getConstraintViolations();
      if (violations != null && !violations.isEmpty()) {
        var formattedViolations = violations.stream()
          .filter(Objects::nonNull)
          .map(cv -> prettifyPropertyPath(cv.getPropertyPath()) + ": " + cv.getMessage())
          .toList();
        problemDetail.setProperty("violations", formattedViolations);
      }
    });
  }

  /**
   * Handles the illegal request parameter exception, directly using the message as the problem detail message.
   */
  @ExceptionHandler(IllegalRequestParameterException.class)
  public @Nonnull ProblemDetail handleIllegalRequestParameterException(
    @Nonnull IllegalRequestParameterException exception,
    @Nonnull HttpServletRequest request
  ) {
    return this.configureBadRequest(request, problemDetail -> {
      problemDetail.setType(BAD_PARAMETER_TYPE);
      problemDetail.setDetail(exception.getMessage());
    });
  }

  /**
   * Handles the case where a required request parameter is not provided.
   */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public @Nonnull ProblemDetail handleMissingServletRequestParameterException(
    @Nonnull MissingServletRequestParameterException exception,
    @Nonnull HttpServletRequest request
  ) {
    return this.configureBadRequest(request, problemDetail -> {
      problemDetail.setType(BAD_PARAMETER_TYPE);
      problemDetail.setDetail("The request parameter " + exception.getParameterName() + " is missing but required");
    });
  }

  /**
   * Handles the case where a type cannot be deserialized from a given input.
   */
  @ExceptionHandler(TypeMismatchException.class)
  public @Nonnull ProblemDetail handleTypeMismatchException(
    @Nonnull TypeMismatchException exception,
    @Nonnull HttpServletRequest request
  ) {
    return this.configureBadRequest(request, problemDetail -> {
      var prop = exception.getPropertyName();
      var detail = (prop != null ? prop + ": " : "") + "cannot convert from input '" + exception.getValue() + "'";
      problemDetail.setDetail(detail);
      problemDetail.setType(BAD_PARAMETER_TYPE);
    });
  }
}
