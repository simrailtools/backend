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

package tools.simrail.backend.common.tristate;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import jakarta.annotation.Nonnull;
import java.io.IOException;

/**
 * Json serializer for tristate.
 */
public final class TristateJsonSerializer extends JsonSerializer<Tristate<?>> {

  @Override
  public void serialize(
    @Nonnull Tristate<?> input,
    @Nonnull JsonGenerator gen,
    @Nonnull SerializerProvider serializers
  ) throws IOException {
    switch (input) {
      case Tristate.Null _ -> gen.writeNull();
      case Tristate.Undefined _ -> {
      }
      case Tristate.Defined(var value) -> serializers.defaultSerializeValue(value, gen);
    }
  }

  @Override
  public boolean isEmpty(@Nonnull SerializerProvider provider, @Nonnull Tristate<?> value) {
    return value == Tristate.Undefined.INSTANCE;
  }
}
