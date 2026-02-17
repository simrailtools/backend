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

package tools.simrail.backend.common.cache;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.ToLongFunction;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A single node in a cache.
 *
 * @param <T> the type of element being held by the cache node.
 */
final class DataCacheNode<T> {

  private static final VarHandle VALUE_HANDLE;
  private static final VarHandle REMOVED_HANDLE;

  static {
    try {
      var lookup = MethodHandles.lookup();
      VALUE_HANDLE = lookup.findVarHandle(DataCacheNode.class, "value", Object.class).withInvokeExactBehavior();
      REMOVED_HANDLE = lookup.findVarHandle(DataCacheNode.class, "removed", boolean.class).withInvokeExactBehavior();
    } catch (NoSuchFieldException | IllegalAccessException exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  private final ToLongFunction<T> versionExtractor;

  // the value currently wrapped in this node
  volatile T value;
  long lastWriteNanos;

  // if this node was removed from the underlying cache
  volatile boolean removed;

  DataCacheNode(@NonNull ToLongFunction<T> versionExtractor, @NonNull T value) {
    this.versionExtractor = versionExtractor;

    this.value = value;
    this.lastWriteNanos = System.nanoTime();
  }

  /**
   * Tries to swap the current version to the given new version. If the current version is already newer than the given
   * one, {@code null} is returned and nothing changes. Otherwise, the replaced old value is returned (which is never
   * {@code null}).
   *
   * @param newValue the new version to swap to.
   * @return the old version if the swap was successful, {@code null} if nothing changed.
   */
  public @Nullable T swapToNewVersion(@NonNull T newValue) {
    var newVersion = this.versionExtractor.applyAsLong(newValue);
    while (true) {
      if (this.removed) {
        // this node was removed, probably some orphaned request from somewhere, ignore
        return null;
      }

      var currentValue = this.value;
      var currentVersion = this.versionExtractor.applyAsLong(currentValue);
      if (currentVersion >= newVersion) {
        // a new version is already present, no need to do anything
        return null;
      }

      var swapped = VALUE_HANDLE.compareAndSet(this, currentValue, newValue);
      if (swapped) {
        // new value was successfully applied
        this.lastWriteNanos = System.nanoTime(); // no need to be 100% atomic on this field
        return currentValue;
      }
    }
  }

  /**
   * Marks this node as removed, scheduling it for removal from the owning cache.
   *
   * @return true if the node was marked for removal, false if the node was already marked.
   */
  public boolean markRemoved() {
    var wasMarkedForRemoval = REMOVED_HANDLE.compareAndSet(this, false, true);
    if (wasMarkedForRemoval) {
      this.lastWriteNanos = System.nanoTime();
      return true;
    }

    return false;
  }
}
