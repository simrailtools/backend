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

package tools.simrail.backend.api.event.dto;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The types of updates that can be sent out by an update frame.
 */
@Getter
@AllArgsConstructor
public enum EventFrameType {

  /**
   * Updates about server details.
   */
  SERVER("servers"),
  /**
   * Updates about dispatch posts on a server (or specific post).
   */
  DISPATCH_POST("dispatch-posts"),
  /**
   * Updates when the data of a journey changes.
   */
  JOURNEY_DETAILS("journey-details"),
  /**
   * Updates when the position of a journey changes.
   */
  JOURNEY_POSITION("journey-positions"),
  ;

  /**
   * An unmodifiable lookup map for the registration name to the associated event frame type.
   */
  public static final Map<String, EventFrameType> BY_REGISTRATION_NAME = Arrays.stream(EventFrameType.values())
    .collect(Collectors.toUnmodifiableMap(EventFrameType::getRegistrationName, Function.identity()));

  /**
   * The name that must be sent by a client to register a listener for the frame type.
   */
  private final String registrationName;
}
