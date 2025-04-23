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

package tools.simrail.backend.api.journey;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.hibernate.validator.constraints.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.simrail.backend.api.exception.IllegalRequestParameterException;
import tools.simrail.backend.api.journey.dto.JourneyActiveDto;
import tools.simrail.backend.api.journey.dto.JourneyDto;
import tools.simrail.backend.api.journey.dto.JourneySummaryDto;
import tools.simrail.backend.api.journey.dto.JourneySummaryWithPlayableEventDto;
import tools.simrail.backend.api.pagination.PaginatedResponseDto;
import tools.simrail.backend.api.server.SimRailServerTimeService;
import tools.simrail.backend.common.journey.JourneyTransportType;

@Validated
@CrossOrigin
@RestController
@RequestMapping("/sit-journeys/v1/")
@Tag(name = "journeys-v1", description = "SimRail Journey Data APIs (Version 1)")
class JourneyV1Controller {

  // all transport types in a list
  private static final List<JourneyTransportType> ALL_TRANSPORT_TYPES =
    List.copyOf(EnumSet.allOf(JourneyTransportType.class));

  private final JourneyService journeyService;
  private final SimRailServerTimeService serverTimeService;

  @Autowired
  public JourneyV1Controller(
    @Nonnull JourneyService journeyService,
    @Nonnull SimRailServerTimeService serverTimeService
  ) {
    this.journeyService = journeyService;
    this.serverTimeService = serverTimeService;
  }

  /**
   * Get the parsed server id and current server time for the server with the given id. If the server doesn't exist, an
   * exception is thrown instead.
   *
   * @param serverId the id of the server to get the snapshot and time of.
   * @return a pair holding the parsed server id and time of the server with the given id.
   * @throws IllegalRequestParameterException if no server with the given id exists.
   */
  private @Nonnull Pair<java.util.UUID, OffsetDateTime> getServerIdAndTime(@Nonnull String serverId) {
    return this.serverTimeService
      .resolveServerTime(serverId)
      .orElseThrow(() -> new IllegalRequestParameterException("Invalid server id provided"));
  }

  /**
   * Returns the details of a single journey by its id.
   */
  @GetMapping("/by-id/{id}")
  @Operation(
    summary = "Returns a single journey by the given id",
    parameters = {
      @Parameter(name = "id", description = "The id of the journey to return"),
      @Parameter(
        name = "If-Modified-Since",
        in = ParameterIn.HEADER,
        description = "If provided the response body is empty in case the data didn't change since the given date",
        schema = @Schema(type = "string", format = "date-time", examples = "Wed, 21 Oct 2015 07:28:00 GMT")),
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "The journey with the given id was successfully resolved"),
      @ApiResponse(
        responseCode = "304",
        description = "The request was successful but the content was not modified since the last request",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "400",
        description = "The given id or one of the filter parameters did not match the requirements",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "404",
        description = "No journey can be found with the given journey id",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "500",
        description = "An internal error occurred while processing the request",
        content = @Content(schema = @Schema(hidden = true))),
    }
  )
  public @Nonnull ResponseEntity<JourneyDto> byId(@PathVariable("id") @UUID(version = 5, allowNil = false) String id) {
    return this.journeyService.findById(java.util.UUID.fromString(id))
      .map(journey -> ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .lastModified(journey.lastUpdated().toInstant())
        .body(journey))
      .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /**
   * Fetches multiple (up to 250) journeys at the same time using their ids.
   */
  @PostMapping("/by-ids")
  @Operation(
    summary = "Get a batch of journeys (up to 250) by their id",
    requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "An array containing the ids of the journeys to resolve"
    ),
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "The journeys that were successfully resolved based on the given input ids"),
      @ApiResponse(
        responseCode = "400",
        description = "One of the filter parameters is invalid",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "500",
        description = "An internal error occurred while processing the request",
        content = @Content(schema = @Schema(hidden = true))),
    }
  )
  public @Nonnull List<JourneyDto> byIds(
    @RequestBody @Size(min = 1, max = 250) Set<@UUID(version = 5, allowNil = false) String> ids
  ) {
    // sort ids to prevent cache misses when the same request is sent twice with a different id order
    var journeyIds = ids.stream().map(java.util.UUID::fromString).sorted().toList();
    return this.journeyService.findByIds(journeyIds);
  }

  /**
   * Get all journeys that are currently on the server with the given id.
   */
  @GetMapping("/active")
  @Operation(
    summary = "Get all journeys that are currently active on a server",
    description = """
      Get descriptive information about all journeys that are currently active on a server. Data returned by this
      endpoint updates every 15 seconds. This endpoint shouldn't be used to poll journey updates, use the event system
      SIT-Events instead.
      """,
    parameters = {
      @Parameter(name = "serverId", description = "The id of the server to filter journeys on"),
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "The active journeys of the given server were successfully resolved"),
      @ApiResponse(
        responseCode = "400",
        description = "One of the filter parameters is invalid",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "500",
        description = "An internal error occurred while processing the request",
        content = @Content(schema = @Schema(hidden = true))),
    }
  )
  public @Nonnull List<JourneyActiveDto> active(
    @RequestParam(name = "serverId") @UUID(version = 5, allowNil = false) String serverId
  ) {
    var serverIdFilter = this.getServerIdAndTime(serverId).getFirst();
    return this.journeyService.findActiveJourneys(serverIdFilter);
  }

  /**
   * Finds journeys based on their tail events.
   */
  @GetMapping("/by-tail")
  @Operation(
    summary = "Find journeys based on its tails",
    description = """
      Filters journeys based on its start and end events, where the start event data is required for filtering and the
      end event data can optionally be supplied for further narrowing.
      """,
    parameters = {
      @Parameter(name = "page", description = "The page of elements to return, defaults to 1"),
      @Parameter(name = "limit", description = "The maximum items to return per page, defaults to 20"),
      @Parameter(name = "serverId", description = "The id of the server to filter journeys on"),
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
        description = "The journeys were successfully resolved based on the given filter parameters"),
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
  public @Nonnull PaginatedResponseDto<JourneySummaryDto> byTail(
    @RequestParam(name = "page", required = false) @Min(1) Integer page,
    @RequestParam(name = "limit", required = false) @Min(1) @Max(100) Integer limit,
    @RequestParam(name = "serverId") @UUID(version = 5, allowNil = false) String serverId,
    @RequestParam(name = "startTime") OffsetDateTime startTime,
    @RequestParam(name = "startStationId") @UUID(version = 4, allowNil = false) String startStationId,
    @RequestParam(name = "startJourneyNumber") @Pattern(regexp = ".+") String startJourneyNumber,
    @RequestParam(name = "startJourneyCategory") @Pattern(regexp = "[A-Z]+") String startJourneyCategory,
    @RequestParam(name = "endTime", required = false) OffsetDateTime endTime,
    @RequestParam(name = "endStationId", required = false) @UUID(version = 4, allowNil = false) String endStationId
  ) {
    var serverIdFilter = this.getServerIdAndTime(serverId).getFirst();
    var stationStationIdFilter = java.util.UUID.fromString(startStationId);
    var endStationIdFilter = endStationId == null ? null : java.util.UUID.fromString(endStationId);
    return this.journeyService.findByTail(
      page,
      limit,
      serverIdFilter,
      startTime,
      stationStationIdFilter,
      startJourneyNumber,
      startJourneyCategory,
      endTime,
      endStationIdFilter);
  }

  /**
   * Finds journeys based on one event along its route matching the given search criteria.
   */
  @GetMapping("/by-event")
  @Operation(
    summary = "Find journeys based on one journey event matching the given search criteria",
    description = """
      Find journeys based on one journey event matching the given search criteria, for example:
      - Searching for journey number '40180' will return 'EIP 40180', 'ROJ 40180' etc.
      - Searching for 'PWJ 146051' will also return 'ROJ 19369' that starts as 'ROJ' but switches to 'PWJ' along its route
      - Searching for 'RE1' at '2024-12-06' will also return journeys that start at '2024-12-05' and continue on '2024-12-06'
      
      Multiple filter parameter can be provided and are linked in a logical AND chain. Ensure that at least the
      journey number or journey line is provided.
      """,
    parameters = {
      @Parameter(name = "page", description = "The page of elements to return, defaults to 1"),
      @Parameter(name = "limit", description = "The maximum items to return per page, defaults to 20"),
      @Parameter(name = "serverId", description = "The id of the server to filter journeys on"),
      @Parameter(name = "date", description = "The date of an event (ISO-8601 without timezone), defaults to the current server date if omitted"),
      @Parameter(name = "line", description = "The line at an event, at least journeyNumber or line must be provided"),
      @Parameter(name = "journeyNumber", description = "The number at an event, at least journeyNumber or line must be provided"),
      @Parameter(name = "journeyCategory", description = "The category at an event"),
      @Parameter(name = "transportTypes", description = "The transport types that are returned, defaults to all types if omitted"),
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "The journeys were successfully resolved based on the given filter parameters"),
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
  public @Nonnull PaginatedResponseDto<JourneySummaryDto> byEvent(
    @RequestParam(name = "page", required = false) @Min(1) Integer page,
    @RequestParam(name = "limit", required = false) @Min(1) @Max(100) Integer limit,
    @RequestParam(name = "serverId") @UUID(version = 5, allowNil = false) String serverId,
    @RequestParam(name = "date", required = false) LocalDate date,
    @RequestParam(name = "line", required = false) @Pattern(regexp = ".+") String line,
    @RequestParam(name = "journeyNumber", required = false) @Pattern(regexp = ".+") String journeyNumber,
    @RequestParam(name = "journeyCategory", required = false) @Pattern(regexp = "[A-Z]+") String journeyCategory,
    @RequestParam(name = "transportTypes", required = false) List<JourneyTransportType> transportTypes
  ) {
    // at least the journey number or line has to be given
    if (journeyNumber == null && line == null) {
      throw new IllegalRequestParameterException("Either journey number or line must be provided");
    }

    var serverAndTime = this.getServerIdAndTime(serverId);
    if (date == null) {
      // default the requested date to the current date on the requested server
      date = serverAndTime.getSecond().toLocalDate();
    }

    if (transportTypes == null || transportTypes.isEmpty()) {
      // default the transport types to all if not given
      transportTypes = ALL_TRANSPORT_TYPES;
    }

    var serverIdFilter = serverAndTime.getFirst();
    return this.journeyService.findByEvent(
      page,
      limit,
      serverIdFilter,
      date,
      line,
      journeyNumber,
      journeyCategory,
      transportTypes);
  }

  /**
   * Finds journeys that are becoming playable in the given time range.
   */
  @GetMapping("/by-playable-departure")
  @Operation(
    summary = "Find journeys that become playable in the provided time range",
    description = """
      Finds journeys that become playable in the provided time range. Optionally additional filter parameters can be
      provided to narrow down the results. The provided time range must be at least 1 minute and at most 60 minutes
      long. If the start time is omitted it defaults to the current server time, if the end time is omitted it defaults
      to the start time plus 15 minutes.
      """,
    parameters = {
      @Parameter(name = "page", description = "The page of elements to return, defaults to 1"),
      @Parameter(name = "limit", description = "The maximum items to return per page, defaults to 20"),
      @Parameter(name = "serverId", description = "The id of the server to filter journeys on"),
      @Parameter(name = "timeStart", description = "The start of the time range (ISO-8601 with offset), defaults to the current server time if omitted"),
      @Parameter(name = "timeEnd", description = "The end of the time range (ISO-8601 with offset), defaults to start plus 15 minutes if omitted"),
      @Parameter(name = "line", description = "The line of the journey at the first playable event"),
      @Parameter(name = "journeyCategory", description = "The category of the journey at the first playable event"),
      @Parameter(name = "transportTypes", description = "The transport types that are returned, defaults to all types if omitted"),
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "The journeys were successfully resolved based on the given filter parameters"),
      @ApiResponse(
        responseCode = "400",
        description = "One of the filter parameters is invalid or doesn't match the described requirements",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "500",
        description = "An internal error occurred while processing the request",
        content = @Content(schema = @Schema(hidden = true))),
    }
  )
  public @Nonnull PaginatedResponseDto<JourneySummaryWithPlayableEventDto> byPlayableDeparture(
    @RequestParam(name = "page", required = false) @Min(1) Integer page,
    @RequestParam(name = "limit", required = false) @Min(1) @Max(100) Integer limit,
    @RequestParam(name = "serverId") @UUID(version = 5, allowNil = false) String serverId,
    @RequestParam(name = "timeStart", required = false) OffsetDateTime timeStart,
    @RequestParam(name = "timeEnd", required = false) OffsetDateTime timeEnd,
    @RequestParam(name = "line", required = false) @Pattern(regexp = ".+") String line,
    @RequestParam(name = "journeyCategory", required = false) @Pattern(regexp = "[A-Z]+") String journeyCategory,
    @RequestParam(name = "transportTypes", required = false) List<JourneyTransportType> transportTypes
  ) {
    var serverAndTime = this.getServerIdAndTime(serverId);
    if (timeStart == null) {
      // default the requested time start to the current time on the requested server
      timeStart = serverAndTime.getSecond();
    }

    if (timeEnd == null) {
      // default the requested time end to 15 minutes after the requested time start if not provided
      timeEnd = timeStart.plusMinutes(15);
    }

    // ensure that the time span is at least 1 minute and at most 60 minutes long
    // this also validates that the given timeStart is before the given timeEnd
    var requestedSpanMinutes = Duration.between(timeStart, timeEnd).toMinutes();
    if (requestedSpanMinutes < 1 || requestedSpanMinutes > 60) {
      throw new IllegalRequestParameterException("The requested time span must be between 1 and 60 minutes long");
    }

    if (transportTypes == null || transportTypes.isEmpty()) {
      // default the transport types to all if not given
      transportTypes = ALL_TRANSPORT_TYPES;
    }

    var serverIdFilter = serverAndTime.getFirst();
    var truncatedStart = timeStart.truncatedTo(ChronoUnit.MINUTES);
    var truncatedEnd = timeEnd.truncatedTo(ChronoUnit.MINUTES);
    return this.journeyService.findByPlayableDeparture(
      page,
      limit,
      serverIdFilter,
      truncatedStart,
      truncatedEnd,
      line,
      journeyCategory,
      transportTypes);
  }

  /**
   * Finds journeys that have the given railcar in their vehicle composition.
   */
  @GetMapping("/by-vehicle")
  @Operation(
    summary = "Finds journeys that are using the given railcar in their vehicle composition",
    description = """
      Finds journeys that use the given railcar in their vehicle composition on the given date. The results might be
      incomplete or incorrect for journeys that were not active yet, as the result data will be based on predictions
      and not the real composition of the journey.
      """,
    parameters = {
      @Parameter(name = "page", description = "The page of elements to return, defaults to 1"),
      @Parameter(name = "limit", description = "The maximum items to return per page, defaults to 20"),
      @Parameter(name = "serverId", description = "The id of the server to filter journeys on"),
      @Parameter(name = "date", description = "The date of an event (ISO-8601 without timezone), defaults to the current server date if omitted"),
      @Parameter(name = "railcar", description = "The id of the railcar that must be included in the vehicle composition of the journey"),
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "The journeys were successfully resolved based on the given filter parameters"),
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
  public @Nonnull PaginatedResponseDto<JourneySummaryDto> byRailcar(
    @RequestParam(name = "page", required = false) @Min(1) Integer page,
    @RequestParam(name = "limit", required = false) @Min(1) @Max(100) Integer limit,
    @RequestParam(name = "serverId") @UUID(version = 5, allowNil = false) String serverId,
    @RequestParam(name = "date", required = false) LocalDate date,
    @RequestParam(name = "railcar") @UUID(version = 4, allowNil = false) String railcarId
  ) {
    var serverAndTime = this.getServerIdAndTime(serverId);
    if (date == null) {
      // default the requested date to the current date on the requested server
      date = serverAndTime.getSecond().toLocalDate();
    }

    var serverIdFilter = serverAndTime.getFirst();
    var railcarIdFilter = java.util.UUID.fromString(railcarId);
    return this.journeyService.findByRailcar(page, limit, serverIdFilter, date, railcarIdFilter);
  }
}
