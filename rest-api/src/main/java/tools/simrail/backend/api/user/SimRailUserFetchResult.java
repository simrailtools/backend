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

package tools.simrail.backend.api.user;

import jakarta.annotation.Nonnull;

/**
 * Fetch result wrapper of a user fetch request.
 *
 * @param <T> the user type that was requested.
 */
@SuppressWarnings("unused") // generic type param T is unused but actually required for strict typing
sealed interface SimRailUserFetchResult<T> {

  /**
   * Fetch result to indicate that the user was fetched successfully.
   *
   * @param user the fetched user.
   * @param <T>  the user type that was fetched.
   */
  record Success<T>(@Nonnull T user) implements SimRailUserFetchResult<T> {

  }

  /**
   * Fetch result to indicate that the requested user was not found.
   *
   * @param userId the id of the user that was requested.
   */
  record NotFound<T>(@Nonnull String userId) implements SimRailUserFetchResult<T> {

  }

  /**
   * Fetch result to indicate that the user fetch request couldn't be fulfilled due to an error.
   */
  record Failure<T>() implements SimRailUserFetchResult<T> {

  }
}
