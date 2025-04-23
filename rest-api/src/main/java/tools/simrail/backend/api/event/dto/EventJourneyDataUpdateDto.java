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

import jakarta.annotation.Nonnull;
import tools.simrail.backend.common.rpc.JourneyUpdateFrame;

/**
 * DTO for updating the event data of a journey, only contains the fields that can be updated.
 */
public record EventJourneyDataUpdateDto(@Nonnull String journeyId) {

  /**
   * Constructs an update dto from the given frame data.
   *
   * @param frame the frame data to construct the dto from.
   * @return a new journey data update frame based on the given journey update frame.
   */
  public static @Nonnull EventJourneyDataUpdateDto fromJourneyUpdateFrame(@Nonnull JourneyUpdateFrame frame) {
    return new EventJourneyDataUpdateDto(frame.getJourneyId());
  }
}
