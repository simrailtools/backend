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
import feign.Response;
import feign.Util;
import feign.codec.Decoder;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tools.simrail.backend.external.feign.exception.StacklessRequestException;

/**
 * Response decoder that can decode a FeignJsonResponseTuple or pass the request to the given downstream decoder.
 */
public record FeignJsonResponseTupleDecoder(@NonNull Decoder downstream) implements Decoder {

  private static final int MAX_BODY_CHARS_IN_ERR_MESSAGE = 250;

  /**
   * Builds a helpful error message from the given response, indicating why the decoding process was skipped.
   *
   * @param response the response that was received and caused the decoding to be skipped.
   * @return a string with a helpful error message to indicate why the decoding was skipped.
   */
  private static @NonNull String buildErrorMessage(@NonNull Response response) {
    String bodyAsString = null;
    var body = response.body();
    if (body != null) {
      try {
        var bodyBytes = Util.toByteArray(body.asInputStream());
        bodyAsString = new String(bodyBytes, StandardCharsets.UTF_8);
        if (bodyAsString.length() > MAX_BODY_CHARS_IN_ERR_MESSAGE) {
          bodyAsString = bodyAsString.substring(0, MAX_BODY_CHARS_IN_ERR_MESSAGE) + "...";
        }
      } catch (IOException _) {
      }
    }

    return String.format(
      "Skipping decode due to receiving Response[status=%s; body=%s]",
      response.status(), bodyAsString);
  }

  @Override
  public @Nullable Object decode(@NonNull Response response, @NonNull Type type) throws IOException, FeignException {
    if (type instanceof ParameterizedType pt
      && pt.getRawType() instanceof Class<?> rawType
      && rawType == FeignJsonResponseTuple.class) {
      var bodyType = pt.getActualTypeArguments()[0];
      var body = this.downstream.decode(response, bodyType);
      return new FeignJsonResponseTuple<>(response, body);
    }

    // only call downstream decoder if the request was successful
    var shouldDecode = response.status() >= 200 && response.status() < 300;
    if (shouldDecode) {
      return this.downstream.decode(response, type);
    } else {
      var message = buildErrorMessage(response);
      var originalException = new IllegalStateException(message);
      throw new StacklessRequestException(response.request(), originalException);
    }
  }
}
