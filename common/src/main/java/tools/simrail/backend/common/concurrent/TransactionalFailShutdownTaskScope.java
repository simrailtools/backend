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

package tools.simrail.backend.common.concurrent;

import jakarta.annotation.Nonnull;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * A task scope that shuts down when one of the subtasks returns with an error state. All forked tasks will be executed
 * in a new transaction.
 */
public final class TransactionalFailShutdownTaskScope extends StructuredTaskScope<Object> {

  private static final VarHandle FIRST_EXCEPTION_VAR_HANDLE;

  static {
    try {
      FIRST_EXCEPTION_VAR_HANDLE = MethodHandles
        .lookup()
        .findVarHandle(TransactionalFailShutdownTaskScope.class, "firstException", Throwable.class);
    } catch (Exception exception) {
      throw new ExceptionInInitializerError(exception);
    }
  }

  private final TransactionTemplate transactionTemplate;
  private volatile Throwable firstException;

  public TransactionalFailShutdownTaskScope(@Nonnull TransactionTemplate transactionTemplate) {
    this.transactionTemplate = transactionTemplate;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <U> @Nonnull Subtask<U> fork(@Nonnull Callable<? extends U> task) {
    return super.fork(() -> this.transactionTemplate.execute(_ -> {
      try {
        return task.call();
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void handleComplete(@Nonnull Subtask<?> subtask) {
    if (subtask.state() == Subtask.State.FAILED
      && this.firstException == null
      && FIRST_EXCEPTION_VAR_HANDLE.compareAndSet(this, null, subtask.exception())) {
      super.shutdown();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nonnull TransactionalFailShutdownTaskScope join() throws InterruptedException {
    super.join();
    return this;
  }

  /**
   * Get the first exception that was caught from a subtask in this task scope.
   *
   * @return an optional holding the first exception that was caught from a subtask in this task scope, if one exists.
   */
  public @Nonnull Optional<Throwable> firstException() {
    super.ensureOwnerAndJoined();
    return Optional.ofNullable(this.firstException);
  }
}
