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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.hibernate.validator.constraints.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.simrail.backend.api.board.dto.BoardEntryDto;
import tools.simrail.backend.api.board.request.BoardEntrySortOrder;
import tools.simrail.backend.common.journey.JourneyTransportType;

@CrossOrigin
@RestController
@RequestMapping("/sit-boards/v1/")
@Tag(name = "boards-v1", description = "SimRail Boards Data APIs (Version 1)")
class BoardV1Controller {

  private final BoardService boardService;

  @Autowired
  public BoardV1Controller(@NonNull BoardService boardService) {
    this.boardService = boardService;
  }

  /**
   * Lists all arrivals that are matching the given requests parameters.
   */
  @GetMapping("/arrivals")
  @Operation(
    summary = "Get all arrivals within a specified timespan at a specified point",
    description = """
      Lists all journeys that are arriving at the specified point on the specified server within the given time span.
      By default, a time span of 30 minutes is returned, starting at the current server time, unless otherwise specified.
      Results are sorted by realtime time information by default. A minimum time span of 5 minutes and a maximum time
      span of 6 hours can be requested. The start time can be 1 day in the future and 90 days in the past.
      """,
    parameters = {
      @Parameter(name = "serverId", description = "The id of the server to get the board on"),
      @Parameter(name = "pointId", description = "The id of the point to get the board of"),
      @Parameter(name = "timeStart", description = "The start time for the board, if omitted current server time will be used"),
      @Parameter(name = "timeEnd", description = "The end time for the board, if omitted the start time plus 30 minutes will be used"),
      @Parameter(name = "transportTypes", description = "Filter for transports that should be returned, if omitted all transports will be returned"),
      @Parameter(name = "sortBy", description = "Sort order of board entries, of omitted entries will be sorted by realtime time information"),
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "The arrival board entries were successfully resolved"),
      @ApiResponse(
        responseCode = "400",
        description = "One of the given filter parameters is invalid or doesn't comply to the described constraints",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "500",
        description = "An internal error occurred while processing the request",
        content = @Content(schema = @Schema(hidden = true))),
    }
  )
  public @NonNull List<BoardEntryDto> listBoardArrivals(
    @RequestParam(name = "serverId") @UUID(version = 5, allowNil = false) String serverId,
    @RequestParam(name = "pointId") @UUID(version = 4, allowNil = false) String pointId,
    @RequestParam(name = "timeStart", required = false) LocalDateTime timeStart,
    @RequestParam(name = "timeEnd", required = false) LocalDateTime timeEnd,
    @RequestParam(name = "transportTypes", required = false) Set<JourneyTransportType> transportTypes,
    @RequestParam(name = "sortBy", required = false) BoardEntrySortOrder sortBy
  ) {
    var sortOrder = Objects.requireNonNullElse(sortBy, BoardEntrySortOrder.REALTIME_TIME);
    var requestParams = this.boardService.buildRequestParameters(serverId, pointId, timeStart, timeEnd, transportTypes);

    var arrivals = this.boardService.listArrivals(requestParams);
    arrivals.sort(sortOrder.getComparator());
    return arrivals;
  }

  /**
   * Lists all departures that are matching the given requests parameters.
   */
  @GetMapping("/departures")
  @Operation(
    summary = "Get all departures within a specified timespan at a specified point",
    description = """
      Lists all journeys that are departing from the specified point on the specified server within the given time span.
      By default, a time span of 30 minutes is returned, starting at the current server time, unless otherwise specified.
      Results are sorted by realtime time information by default. A minimum time span of 5 minutes and a maximum time
      span of 6 hours can be requested. The start time can be 1 day in the future and 90 days in the past.
      """,
    parameters = {
      @Parameter(name = "serverId", description = "The id of the server to get the board on"),
      @Parameter(name = "pointId", description = "The id of the point to get the board of"),
      @Parameter(name = "timeStart", description = "The start time for the board, if omitted current server time will be used"),
      @Parameter(name = "timeEnd", description = "The end time for the board, if omitted the start time plus 30 minutes will be used"),
      @Parameter(name = "transportTypes", description = "Filter for transports that should be returned, if omitted all transports will be returned"),
      @Parameter(name = "sortBy", description = "Sort order of board entries, of omitted entries will be sorted by realtime time information"),
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "The departure board entries were successfully resolved"),
      @ApiResponse(
        responseCode = "400",
        description = "One of the given filter parameters is invalid or doesn't comply to the described constraints",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "500",
        description = "An internal error occurred while processing the request",
        content = @Content(schema = @Schema(hidden = true))),
    }
  )
  public @NonNull List<BoardEntryDto> listBoardDepartures(
    @RequestParam(name = "serverId") @UUID(version = 5, allowNil = false) String serverId,
    @RequestParam(name = "pointId") @UUID(version = 4, allowNil = false) String pointId,
    @RequestParam(name = "timeStart", required = false) LocalDateTime timeStart,
    @RequestParam(name = "timeEnd", required = false) LocalDateTime timeEnd,
    @RequestParam(name = "transportTypes", required = false) Set<JourneyTransportType> transportTypes,
    @RequestParam(name = "sortBy", required = false) BoardEntrySortOrder sortBy
  ) {
    var sortOrder = Objects.requireNonNullElse(sortBy, BoardEntrySortOrder.REALTIME_TIME);
    var requestParams = this.boardService.buildRequestParameters(serverId, pointId, timeStart, timeEnd, transportTypes);

    var departures = this.boardService.listDepartures(requestParams);
    departures.sort(sortOrder.getComparator());
    return departures;
  }
}
