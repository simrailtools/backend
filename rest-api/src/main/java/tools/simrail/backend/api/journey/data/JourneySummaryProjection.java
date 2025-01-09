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

package tools.simrail.backend.api.journey.data;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Projection of a journey entity that only contains fields needed for summary requests (e.g. by-relation).
 */
public interface JourneySummaryProjection {

  /**
   * Get the id of the journey.
   *
   * @return the id of the journey.
   */
  @Nonnull
  UUID getId();

  /**
   * Get the id of the server on which the journey happens.
   *
   * @return the id of the server on which the journey happens.
   */
  @Nonnull
  UUID getServerId();

  /**
   * Get the time when the journey was first active, null if the journey wasn't active yet.
   *
   * @return the time when the journey was first active.
   */
  @Nullable
  OffsetDateTime getFirstSeenTime();

  /**
   * Get the time when the journey was last active, null if the journey wasn't active yet or is still active.
   *
   * @return the time when the journey was last active.
   */
  @Nullable
  OffsetDateTime getLastSeenTime();

  /**
   * Get if the complete journey is cancelled and will not happen.
   *
   * @return if the complete journey is cancelled and will not happen.
   */
  boolean isCancelled();
}
