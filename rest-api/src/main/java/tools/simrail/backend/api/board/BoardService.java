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

package tools.simrail.backend.api.board;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
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

@Service
class BoardService {

  private static final Set<JourneyTransportType> ALL_TRANSPORT_TYPES = EnumSet.allOf(JourneyTransportType.class);

  private static final int MIN_TIME_SPAN_MINUTES = 5;
  private static final long MAX_TIME_SPAN_MINUTES = TimeUnit.HOURS.toMinutes(6);

  private static final int MAX_DAYS_IN_PAST = -89;
  private static final int MAX_DAYS_IN_FUTURE = 1;

  private final SimRailPointProvider pointProvider;
  private final SimRailServerTimeService serverTimeService;

  private final BoardJourneyRepository boardJourneyRepository;
  private final BoardEntryDtoConverter boardEntryDtoConverter;

  @Autowired
  BoardService(
    @NonNull SimRailPointProvider pointProvider,
    @NonNull SimRailServerTimeService serverTimeService,
    @NonNull BoardJourneyRepository boardJourneyRepository,
    @NonNull BoardEntryDtoConverter boardEntryDtoConverter
  ) {
    this.pointProvider = pointProvider;
    this.serverTimeService = serverTimeService;
    this.boardJourneyRepository = boardJourneyRepository;
    this.boardEntryDtoConverter = boardEntryDtoConverter;
  }

  /**
   * Validates the given input requests parameters and builds a board request parameters object which can subsequently
   * be used to request an arrival or departure board.
   *
   * @param serverId       the requested server id.
   * @param pointId        the requested point id.
   * @param timeStart      the requested time frame start.
   * @param timeEnd        the requested time frame end.
   * @param transportTypes the requested transport types.
   * @return an object holding the given request parameters for further requesting of data.
   */
  public @NonNull BoardRequestParameters buildRequestParameters(
    @NonNull String serverId,
    @NonNull String pointId,
    @Nullable LocalDateTime timeStart,
    @Nullable LocalDateTime timeEnd,
    @Nullable Set<JourneyTransportType> transportTypes
  ) {
    // resolve and verify server & point id
    var serverTime = this.serverTimeService.resolveServerTime(serverId)
      .orElseThrow(() -> new IllegalRequestParameterException("Invalid server id provided"));
    var point = this.pointProvider.findPointByIntId(UUID.fromString(pointId))
      .orElseThrow(() -> new IllegalRequestParameterException("Invalid point id provided"));

    // default start time to server time and end time to start + 30 min if not provided
    if (timeStart == null) {
      timeStart = serverTime;
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
    var daysDiffToServerTime = Duration.between(serverTime, timeStart).toDays();
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

    var parsedServerId = UUID.fromString(serverId);
    var transportTypeArray = transportTypes.stream().map(Enum::name).toArray(String[]::new);
    return new BoardRequestParameters(parsedServerId, point.getId(), timeStart, timeEnd, transportTypeArray);
  }

  /**
   * Lists all arrivals that are matching the given request parameters.
   *
   * @param requestParameters the request parameters.
   * @return all arrivals that are matching the given request parameters.
   */
  @Cacheable(cacheNames = "boards_cache", key = "'arr_' + #requestParameters")
  public @NonNull List<BoardEntryDto> listArrivals(@NonNull BoardRequestParameters requestParameters) {
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
   * Lists all departures that are matching the given request parameters.
   *
   * @param requestParameters the request parameters.
   * @return all departures that are matching the given request parameters.
   */
  @Cacheable(cacheNames = "boards_cache", key = "'dep_' + #requestParameters")
  public @NonNull List<BoardEntryDto> listDepartures(@NonNull BoardRequestParameters requestParameters) {
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
