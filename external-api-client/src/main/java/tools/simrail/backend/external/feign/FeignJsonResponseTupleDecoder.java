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
import feign.codec.Decoder;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Response decoder that can decode a FeignJsonResponseTuple or pass the request to the given downstream decoder.
 */
public record FeignJsonResponseTupleDecoder(@NotNull Decoder downstream) implements Decoder {

  @Override
  public @Nullable Object decode(@NotNull Response response, @NotNull Type type) throws IOException, FeignException {
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
      var methodKey = response.request().requestTemplate().methodMetadata().configKey();
      throw FeignException.errorStatus(methodKey, response);
    }
  }
}
