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

package tools.simrail.backend.common.update;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A field that can be updated. Updates to the field are done via the {@link #updateValue(Object)} method. This
 * underlying field value is only updated if the current value of the field does not {@code equals} the new given value.
 * Fields can optionally allow null values, by default null values passed to {@link #updateValue(Object)} are ignored.
 *
 * @param <T> the type carried by the field.
 */
public final class UpdatableField<T> {

  private final boolean allowNulls;
  private final UpdatableFieldGroup group;

  private T value;
  private boolean dirty;

  UpdatableField(@NonNull UpdatableFieldGroup group, boolean allowNulls) {
    this.group = group;
    this.allowNulls = allowNulls;
  }

  /**
   * Consumes the state of the dirty flag of this field. This means that the method returns the current state of the
   * field and resets it to false.
   *
   * @return true if the value associated with this field changed, false otherwise.
   */
  public boolean consumeDirty() {
    var isDirty = this.dirty;
    this.dirty = false;
    return isDirty;
  }

  /**
   * Get the current value of this field. Note that the value can only be null if the field wasn't initialized to any
   * value yet or if null values are explicitly allowed.
   *
   * @return the current value associated with this field, possibly {@code null}.
   */
  public T currentValue() {
    return this.value;
  }

  /**
   * Updates the current value carried by this field if it is not equal to the current value. If the carried value of
   * this field was updated as a result of the method call, the dirty state of this field and the associated group is
   * set.
   *
   * @param newValue the value to possibly update the carried field value to.
   */
  public void updateValue(@Nullable T newValue) {
    if ((newValue != null || this.allowNulls) && !Objects.equals(this.value, newValue)) {
      this.value = newValue;
      this.dirty = true;
      this.group.notifyDirty();
    }
  }
}
