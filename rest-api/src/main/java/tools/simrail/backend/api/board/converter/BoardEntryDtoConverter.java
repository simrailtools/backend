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

package tools.simrail.backend.api.board.converter;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.simrail.backend.api.board.data.BoardJourneyProjection;
import tools.simrail.backend.api.board.dto.BoardEntryDto;
import tools.simrail.backend.api.board.dto.BoardStopInfoDto;

/**
 * Converter for a list of board entry projections to board entry dtos.
 */
@Component
public final class BoardEntryDtoConverter implements Function<List<BoardJourneyProjection>, BoardEntryDto> {

  private static final Comparator<BoardJourneyProjection> INDEX_COMPARATOR =
    Comparator.comparingInt(BoardJourneyProjection::getEventIndex);

  private final BoardViaEventDtoConverter viaEventDtoConverter;
  private final BoardTransportDtoConverter transportDtoConverter;

  @Autowired
  public BoardEntryDtoConverter(
    @NonNull BoardViaEventDtoConverter viaEventDtoConverter,
    @NonNull BoardTransportDtoConverter transportDtoConverter
  ) {
    this.viaEventDtoConverter = viaEventDtoConverter;
    this.transportDtoConverter = transportDtoConverter;
  }

  @Override
  public @NonNull BoardEntryDto apply(@NonNull List<BoardJourneyProjection> entries) {
    // sort the entries in their encounter order
    entries.sort(INDEX_COMPARATOR);

    // convert via events
    var root = entries.getFirst();
    var viaEvents = entries.stream().map(this.viaEventDtoConverter).toList();

    // convert stop information
    var scheduledStopInfo = this.buildStopInfo(root.getInitialScheduledPlatform(), root.getInitialScheduledTrack());
    var realtimeStopInfo = this.buildStopInfo(root.getInitialRealtimePlatform(), root.getInitialRealtimeTrack());

    var transport = this.transportDtoConverter.apply(root);
    return new BoardEntryDto(
      root.getJourneyId(),
      root.getInitialEventId(),
      root.isInitialCancelled(),
      root.isInitialAdditional(),
      root.getInitialScheduledTime(),
      root.getInitialRealtimeTime(),
      root.getInitialRealtimeTimeType(),
      root.getInitialStopType(),
      scheduledStopInfo,
      realtimeStopInfo,
      transport,
      viaEvents);
  }

  private @Nullable BoardStopInfoDto buildStopInfo(@Nullable Integer platform, @Nullable Integer track) {
    return platform != null && track != null ? new BoardStopInfoDto(platform, track) : null;
  }
}
