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

import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.NonNull;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Encoder implementation that maps from a java object by using jackson 3.
 *
 * @param objectMapper the object mapper to use for mapping.
 */
public record Jackson3Encoder(@NonNull ObjectMapper objectMapper) implements Encoder {

  /**
   * {@inheritDoc}
   */
  @Override
  public void encode(
    @NonNull Object object,
    @NonNull Type bodyType,
    @NonNull RequestTemplate template
  ) throws EncodeException {
    try {
      var type = this.objectMapper.getTypeFactory().constructType(bodyType);
      var serializedBody = this.objectMapper.writerFor(type).writeValueAsBytes(object);
      template.body(serializedBody, StandardCharsets.UTF_8);
    } catch (JacksonException exception) {
      throw new EncodeException("Failed to encode request body", exception);
    }
  }
}
