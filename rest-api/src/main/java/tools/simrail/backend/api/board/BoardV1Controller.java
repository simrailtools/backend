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

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.hibernate.validator.constraints.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.simrail.backend.api.board.dto.BoardEntryDto;
import tools.simrail.backend.api.board.request.BoardEntrySortOrder;
import tools.simrail.backend.common.journey.JourneyTransportType;

@Validated
@CrossOrigin
@RestController
@RequestMapping("/sit-boards/v1/")
@Tag(name = "boards-v1", description = "SimRail Boards Data APIs (Version 1)")
class BoardV1Controller {

  private final BoardService boardService;

  @Autowired
  public BoardV1Controller(@Nonnull BoardService boardService) {
    this.boardService = boardService;
  }

  @GetMapping("/arrivals")
  public @Nonnull List<BoardEntryDto> listArrivals(
    @RequestParam(name = "serverId") @UUID(version = 5, allowNil = false) String serverId,
    @RequestParam(name = "pointId") @UUID(version = 4, allowNil = false) String pointId,
    @RequestParam(name = "timeStart", required = false) OffsetDateTime timeStart,
    @RequestParam(name = "timeEnd", required = false) OffsetDateTime timeEnd,
    @RequestParam(name = "transportTypes", required = false) List<JourneyTransportType> transportTypes,
    @RequestParam(name = "sortBy", required = false) BoardEntrySortOrder sortBy
  ) {
    var sortOrder = Objects.requireNonNullElse(sortBy, BoardEntrySortOrder.REALTIME_TIME);
    var requestParams = this.boardService.buildRequestParameters(serverId, pointId, timeStart, timeEnd, transportTypes);

    var arrivals = this.boardService.listArrivals(requestParams);
    arrivals.sort(sortOrder.getComparator());
    return arrivals;
  }

  @GetMapping("/departures")
  public @Nonnull List<BoardEntryDto> listDepartures(
    @RequestParam(name = "serverId") @UUID(version = 5, allowNil = false) String serverId,
    @RequestParam(name = "pointId") @UUID(version = 4, allowNil = false) String pointId,
    @RequestParam(name = "timeStart", required = false) OffsetDateTime timeStart,
    @RequestParam(name = "timeEnd", required = false) OffsetDateTime timeEnd,
    @RequestParam(name = "transportTypes", required = false) List<JourneyTransportType> transportTypes,
    @RequestParam(name = "sortBy", required = false) BoardEntrySortOrder sortBy
  ) {
    var sortOrder = Objects.requireNonNullElse(sortBy, BoardEntrySortOrder.REALTIME_TIME);
    var requestParams = this.boardService.buildRequestParameters(serverId, pointId, timeStart, timeEnd, transportTypes);

    var departures = this.boardService.listDepartures(requestParams);
    departures.sort(sortOrder.getComparator());
    return departures;
  }
}
