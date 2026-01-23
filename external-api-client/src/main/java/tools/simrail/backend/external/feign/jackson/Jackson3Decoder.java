/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024-2026 Pasqual Koschmieder and contributors
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

package tools.simrail.backend.external.feign.jackson;

import feign.FeignException;
import feign.Response;
import feign.Util;
import feign.codec.Decoder;
import java.io.IOException;
import java.lang.reflect.Type;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.MismatchedInputException;

/**
 * Decoder implementation that maps to a java object by using jackson 3.
 *
 * @param objectMapper the object mapper to use for mapping.
 */
public record Jackson3Decoder(@NonNull ObjectMapper objectMapper) implements Decoder {

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nullable Object decode(@NonNull Response response, @NonNull Type type) throws IOException, FeignException {
    // feign-specific decoder rule kept here: if status is 204 or 404 return an empty value of the type
    var statusCode = response.status();
    if (statusCode == 204 || statusCode == 404) {
      return Util.emptyValueOf(type);
    }

    // if the body is null, return null (weird feign-specific implementation; one would expect an empty value here too)
    var body = response.body();
    if (body == null) {
      return null;
    }

    // early return if we know that the body is empty, otherwise pass it to the jackson
    // decoder and just catch the decoding exception that occurs
    var reader = body.asReader(response.charset());
    if (reader.markSupported()) {
      reader.mark(1);
      if (reader.read() == -1) {
        return null;
      }
      reader.reset();
    }

    try {
      var javaType = this.objectMapper.constructType(type);
      return this.objectMapper.readValue(reader, javaType);
    } catch (JacksonIOException exception) {
      // unwrap the underlying i/o exception and rethrow
      throw exception.getCause();
    } catch (JacksonException exception) {
      if (exception instanceof MismatchedInputException mie && mie.getCurrentToken() == null) {
        // end-of-input that we were unable to catch previously
        return null;
      }

      throw exception;
    }
  }
}
