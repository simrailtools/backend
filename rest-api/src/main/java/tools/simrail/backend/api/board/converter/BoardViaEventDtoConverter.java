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

package tools.simrail.backend.api.board.converter;

import jakarta.annotation.Nonnull;
import java.util.NoSuchElementException;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.simrail.backend.api.board.data.BoardJourneyProjection;
import tools.simrail.backend.api.board.dto.BoardViaEventDto;
import tools.simrail.backend.common.point.SimRailPointProvider;

/**
 * Converter for journey projections to a via event dto.
 */
@Component
public final class BoardViaEventDtoConverter implements Function<BoardJourneyProjection, BoardViaEventDto> {

  private final SimRailPointProvider pointProvider;

  @Autowired
  public BoardViaEventDtoConverter(@Nonnull SimRailPointProvider pointProvider) {
    this.pointProvider = pointProvider;
  }

  @Override
  public @Nonnull BoardViaEventDto apply(@Nonnull BoardJourneyProjection projection) {
    var point = this.pointProvider
      .findPointByIntId(projection.getPointId())
      .orElseThrow(() -> new NoSuchElementException("missing point for stop " + projection.getPointId()));
    return new BoardViaEventDto(
      point.getId(),
      point.getName(),
      projection.isCancelled(),
      projection.isAdditional(),
      projection.getScheduledPlatform() != null);
  }
}
