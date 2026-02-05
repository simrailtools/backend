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

package tools.simrail.backend.common.update;

import org.jspecify.annotations.NonNull;

/**
 * Holder for a group of updatable fields.
 */
public final class UpdatableFieldGroup {

  private boolean anyDirty;

  /**
   * Constructs a new field associated with this field group. Null values are silently ignored by the field.
   *
   * @param <T> the type carried by the field.
   * @return a new field of this group.
   */
  public @NonNull <T> UpdatableField<T> createField() {
    return new UpdatableField<>(this, false);
  }

  /**
   * Constructs a new field associated with this field group. Null values are allowed for the field.
   *
   * @param <T> the type carried by the field.
   * @return a new field of this group.
   */
  public @NonNull <T> UpdatableField<T> createNullableField() {
    return new UpdatableField<>(this, true);
  }

  /**
   * Consumes the state of the dirty flag of this field group. This means that the method returns the current state of
   * the field and resets it to false.
   *
   * @return true if any field associated with this group changed, false otherwise.
   */
  public boolean consumeAnyDirty() {
    var isDirty = this.anyDirty;
    this.anyDirty = false;
    return isDirty;
  }

  /**
   * Called by a field associated with this group to indicate that any field is dirty.
   */
  void notifyDirty() {
    this.anyDirty = true;
  }
}
