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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.simrail.backend.api.board.data.BoardJourneyProjection;
import tools.simrail.backend.api.board.data.BoardJourneyRepository;

@Validated
@CrossOrigin
@RestController
@RequestMapping("/sit-boards/v1/")
@Tag(name = "boards-v1", description = "SimRail Boards Data APIs (Version 1)")
class BoardV1Controller {

  @Autowired
  private BoardJourneyRepository repository;

  @GetMapping("/departures")
  public String arrivals() {
    var sid = UUID.fromString("9db9b77d-a5ff-5385-89c3-6c6224e0824f");
    var pid = UUID.fromString("6840caa1-ef46-4cc4-93fb-0f26abf7c7d9");
    var start = OffsetDateTime.of(LocalDate.now(), LocalTime.of(16, 0), ZoneOffset.ofHours(2));
    var end = OffsetDateTime.of(LocalDate.now(), LocalTime.of(16, 30), ZoneOffset.ofHours(2));
    var arrivals = this.repository.getDepartures(sid, pid, start, end).stream().collect(Collectors.collectingAndThen(
      Collectors.groupingBy(BoardJourneyProjection::getJourneyId),
      grouped -> {
        grouped.forEach((_, v) -> v.sort(Comparator.comparing(BoardJourneyProjection::getEventIndex)));
        return grouped;
      }
    ));

    var l = LoggerFactory.getLogger(BoardV1Controller.class);
    for (Map.Entry<UUID, List<BoardJourneyProjection>> entry : arrivals.entrySet()) {
      var jid = entry.getKey();
      var via = entry.getValue();
      var first = via.getFirst();
      l.info("--> {} [{} - {}]", jid, first.getInitialTransportCategory(), first.getInitialTransportNumber());
      l.info("  - type: {}", first.getInitialTransportType());
      l.info("  - scheduled: {}", first.getInitialScheduledTime());
      l.info("  - realtime: {} @ {}", first.getInitialRealtimeTime(), first.getInitialRealtimeTimeType());
      l.info("  - via:");
      for (BoardJourneyProjection v : via) {
        l.info("    - {} @ {}", v.getPointName(), v.getPointId());
      }
    }

    return "arrivals";
  }
}
