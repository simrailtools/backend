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

package tools.simrail.backend.api.board;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.simrail.backend.api.board.converter.BoardEntryDtoConverter;
import tools.simrail.backend.api.board.data.BoardJourneyProjection;
import tools.simrail.backend.api.board.data.BoardJourneyRepository;
import tools.simrail.backend.api.board.dto.BoardEntryDto;
import tools.simrail.backend.api.board.request.BoardRequestParameters;
import tools.simrail.backend.api.exception.IllegalRequestParameterException;
import tools.simrail.backend.api.server.SimRailServerTimeService;
import tools.simrail.backend.common.journey.JourneyTransportType;
import tools.simrail.backend.common.point.SimRailPointProvider;

/**
 *
 */
@Service
class BoardService {

  private static final List<JourneyTransportType> ALL_TRANSPORT_TYPES =
    List.copyOf(EnumSet.allOf(JourneyTransportType.class));

  private static final int MIN_TIME_SPAN_MINUTES = 5;
  private static final long MAX_TIME_SPAN_MINUTES = TimeUnit.HOURS.toMinutes(6);

  private static final int MAX_DAYS_IN_PAST = -89;
  private static final int MAX_DAYS_IN_FUTURE = 1;

  private final SimRailPointProvider pointProvider;
  private final SimRailServerTimeService serverTimeService;

  private final BoardJourneyRepository boardJourneyRepository;
  private final BoardEntryDtoConverter boardEntryDtoConverter;

  @Autowired
  public BoardService(
    @Nonnull SimRailPointProvider pointProvider,
    @Nonnull SimRailServerTimeService serverTimeService,
    @Nonnull BoardJourneyRepository boardJourneyRepository,
    @Nonnull BoardEntryDtoConverter boardEntryDtoConverter
  ) {
    this.pointProvider = pointProvider;
    this.serverTimeService = serverTimeService;
    this.boardJourneyRepository = boardJourneyRepository;
    this.boardEntryDtoConverter = boardEntryDtoConverter;
  }

  /**
   * @param serverId
   * @param pointId
   * @param timeStart
   * @param timeEnd
   * @param transportTypes
   * @return
   */
  public @Nonnull BoardRequestParameters buildRequestParameters(
    @Nonnull String serverId,
    @Nonnull String pointId,
    @Nullable OffsetDateTime timeStart,
    @Nullable OffsetDateTime timeEnd,
    @Nullable List<JourneyTransportType> transportTypes
  ) {
    // resolve and verify server & point id
    var serverIdAndTime = this.serverTimeService.resolveServerTime(serverId)
      .orElseThrow(() -> new IllegalArgumentException("Invalid server id provided"));
    var point = this.pointProvider.findPointByIntId(UUID.fromString(pointId))
      .orElseThrow(() -> new IllegalRequestParameterException("Invalid point id provided"));

    // default start time to server time and end time to start + 30 min if not provided
    if (timeStart == null) {
      timeStart = serverIdAndTime.getSecond();
    }
    if (timeEnd == null) {
      timeEnd = timeStart.plusMinutes(30);
    }

    // ensure that the given time span is not too short/long or otherwise invalid
    var timeSpanMinutes = Duration.between(timeStart, timeEnd).toMinutes();
    if (timeSpanMinutes < MIN_TIME_SPAN_MINUTES
      || timeSpanMinutes > MAX_TIME_SPAN_MINUTES
      || timeStart.isAfter(timeEnd)) {
      throw new IllegalRequestParameterException("Invalid time span provided");
    }

    // check that the start time is actually within the bounds of data availability
    var daysDiffToServerTime = Duration.between(serverIdAndTime.getSecond(), timeStart).toDays();
    if (daysDiffToServerTime < MAX_DAYS_IN_PAST || daysDiffToServerTime > MAX_DAYS_IN_FUTURE) {
      throw new IllegalRequestParameterException("Start time is out of permitted range");
    }

    // default to all transport types if not provided
    if (transportTypes == null || transportTypes.isEmpty()) {
      transportTypes = ALL_TRANSPORT_TYPES;
    }

    // strip seconds / milliseconds parts from start/end time
    timeStart = timeStart.truncatedTo(ChronoUnit.MINUTES);
    timeEnd = timeEnd.truncatedTo(ChronoUnit.MINUTES);
    return new BoardRequestParameters(serverIdAndTime.getFirst(), point.getId(), timeStart, timeEnd, transportTypes);
  }

  /**
   * @param requestParameters
   * @return
   */
  public @Nonnull List<BoardEntryDto> listArrivals(@Nonnull BoardRequestParameters requestParameters) {
    var entriesByJourney = this.boardJourneyRepository.getArrivals(
        requestParameters.serverId(),
        requestParameters.pointId(),
        requestParameters.timeStart(),
        requestParameters.timeEnd(),
        requestParameters.transportTypes())
      .stream()
      .collect(Collectors.groupingBy(BoardJourneyProjection::getJourneyId));
    return entriesByJourney.values().stream().map(this.boardEntryDtoConverter).collect(Collectors.toList());
  }

  /**
   * @param requestParameters
   * @return
   */
  public @Nonnull List<BoardEntryDto> listDepartures(@Nonnull BoardRequestParameters requestParameters) {
    var entriesByJourney = this.boardJourneyRepository.getDepartures(
        requestParameters.serverId(),
        requestParameters.pointId(),
        requestParameters.timeStart(),
        requestParameters.timeEnd(),
        requestParameters.transportTypes())
      .stream()
      .collect(Collectors.groupingBy(BoardJourneyProjection::getJourneyId));
    return entriesByJourney.values().stream().map(this.boardEntryDtoConverter).collect(Collectors.toList());
  }
}
