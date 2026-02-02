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

package tools.simrail.backend.api.board.request;

import java.time.LocalDateTime;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import tools.simrail.backend.common.journey.JourneyTransportType;

/**
 * Extracted and parsed request parameters for an arrival/departure board.
 *
 * @param serverId       the id of the server on which the board should be retrieved.
 * @param pointId        the id of the point to return the point of.
 * @param timeStart      the start time of the board time range.
 * @param timeEnd        the end time of the board time range.
 * @param transportTypes the transport types to include in the board.
 */
public record BoardRequestParameters(
  @NonNull UUID serverId,
  @NonNull UUID pointId,
  @NonNull LocalDateTime timeStart,
  @NonNull LocalDateTime timeEnd,
  @NonNull JourneyTransportType[] transportTypes
) {

}
