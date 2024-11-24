/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024 Pasqual Koschmieder and contributors
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

package tools.simrail.backend.api.journey;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.time.OffsetDateTime;
import org.hibernate.validator.constraints.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.simrail.backend.api.journey.dto.JourneySummaryDto;
import tools.simrail.backend.api.pagination.PaginatedResponseDto;

@Validated
@CrossOrigin
@RestController
@RequestMapping("/sit-journeys/v1/")
@Tag(name = "journeys-v1", description = "SimRail Journey Data APIs (Version 1)")
class JourneyV1Controller {

  @Autowired
  private JourneyService journeyService;

  /**
   *
   */
  @GetMapping("/by-relation")
  @Operation(
    summary = "Find journeys based on its journey relation",
    description = """
      """,
    parameters = {
      @Parameter(name = "page", description = "The page of elements to return, defaults to 1"),
      @Parameter(name = "limit", description = "The maximum items to return per page, defaults to 20"),
      @Parameter(name = "serverId", description = "The id of the server to filter journeys on, by default all servers are considered"),
      @Parameter(name = "startTime", description = "The scheduled time when the journey departs from the first station"),
      @Parameter(name = "startStationId", description = "The id of the station where the journey is scheduled to depart"),
      @Parameter(name = "startJourneyNumber", description = "The number of the journey at the first station"),
      @Parameter(name = "startJourneyCategory", description = "The category of the journey at the first station"),
      @Parameter(name = "endTime", description = "The scheduled time when the journey arrives at the last station"),
      @Parameter(name = "endStationId", description = "The id of the station where the journey is scheduled to arrive"),
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        useReturnTypeSchema = true,
        description = "The journeys were successfully resolved based on the given filter parameters",
        content = @Content(mediaType = "application/json")),
      @ApiResponse(
        responseCode = "400",
        description = "One of the filter parameters is invalid or doesn't match the described grouping requirements",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "500",
        description = "An internal error occurred while processing the request",
        content = @Content(schema = @Schema(hidden = true))),
    }
  )
  public @Nonnull ResponseEntity<PaginatedResponseDto<JourneySummaryDto>> byRelation(
    @RequestParam(name = "page", required = false) @Min(1) Integer page,
    @RequestParam(name = "limit", required = false) @Min(1) @Max(100) Integer limit,
    @RequestParam(name = "serverId", required = false) @UUID(version = 5, allowNil = false) String serverId,
    @RequestParam(name = "startTime", required = false) OffsetDateTime startTime,
    @RequestParam(name = "startStationId", required = false) @UUID(version = 4, allowNil = false) String startStationId,
    @RequestParam(name = "startJourneyNumber", required = false) @Pattern(regexp = ".+") String startJourneyNumber,
    @RequestParam(name = "startJourneyCategory", required = false) @Pattern(regexp = "[A-Z]+") String startJourneyCategory,
    @RequestParam(name = "endTime", required = false) OffsetDateTime endTime,
    @RequestParam(name = "endStationId", required = false) @UUID(version = 4, allowNil = false) String endStationId
  ) {
    var serverIdFilter = serverId == null ? null : java.util.UUID.fromString(serverId);
    var stationStationIdFilter = startStationId == null ? null : java.util.UUID.fromString(startStationId);
    var endStationIdFilter = endStationId == null ? null : java.util.UUID.fromString(endStationId);
    var response = this.journeyService.findByRelation(
      page,
      limit,
      serverIdFilter,
      startTime,
      stationStationIdFilter,
      startJourneyNumber,
      startJourneyCategory,
      endTime,
      endStationIdFilter);
    return ResponseEntity.ok(response);
  }
}
