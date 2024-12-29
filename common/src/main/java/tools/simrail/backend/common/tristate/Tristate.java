/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024 Pasqual Koschmieder and contributors
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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.annotation.Nonnull;

/**
 * A simple class with 3 states that can be serialized with json. The undefined type is skipped during serialization,
 * the null value indicates that the field is null, the defined value holds a defined value.
 */
@SuppressWarnings({"unused", "unchecked"})
@JsonSerialize(using = TristateJsonSerializer.class)
public sealed interface Tristate<T> {

  static @Nonnull <T> Tristate<T> undefined() {
    return (Tristate<T>) Undefined.INSTANCE;
  }

  static @Nonnull <T> Tristate<T> holdingNull() {
    return (Tristate<T>) Null.INSTANCE;
  }

  static @Nonnull <T> Tristate<T> holdingValue(@Nonnull T value) {
    return new Defined<>(value);
  }

  /**
   * Implementation of tristate representing undefined.
   */
  enum Undefined implements Tristate<Object> {
    INSTANCE,
  }

  /**
   * Implementation of tristate representing a value holder which holds null.
   */
  enum Null implements Tristate<Object> {
    INSTANCE,
  }

  /**
   * Implementation of a tristate that holds a defined value.
   */
  record Defined<T>(@Nonnull T value) implements Tristate<T> {

  }
}
