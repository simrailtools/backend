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

package tools.simrail.backend.api.map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import org.hibernate.validator.constraints.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.simrail.backend.api.map.dto.MapJourneyRouteDto;
import tools.simrail.backend.api.map.geojson.MapJourneyRouteGeoJsonConverter;

@CrossOrigin
@RestController
@RequestMapping("/sit-maps/v1/")
@Tag(name = "maps-v1", description = "SimRail Map Data APIs (Version 1)")
class SimRailMapController {

  /**
   * Media type for geojson as specified in RFC7946.
   */
  private static final MediaType APPLICATION_GEO_JSON = new MediaType("application", "geo+json");

  private final SimRailMapService mapService;
  private final MapJourneyRouteGeoJsonConverter journeyRouteGeoJsonConverter;

  @Autowired
  public SimRailMapController(
    @NonNull SimRailMapService mapService,
    @NonNull MapJourneyRouteGeoJsonConverter journeyRouteGeoJsonConverter
  ) {
    this.mapService = mapService;
    this.journeyRouteGeoJsonConverter = journeyRouteGeoJsonConverter;
  }

  /**
   * Gets the polyline for a specific journey.
   */
  @GetMapping("/polyline/by-journey/{id}")
  @Operation(
    summary = "Get the polyline for a specific journey",
    parameters = {
      @Parameter(name = "id", description = "The id of the journey to get the polyline for"),
      @Parameter(name = "includeCancelled", description = "If cancelled events should be included in the polyline"),
      @Parameter(name = "includeAdditional", description = "If additional events should be included in the polyline"),
      @Parameter(name = "allowFallbackComputation", description = "If a fallback polyline should be returned if a proper one is unavailable"),
      @Parameter(
        in = ParameterIn.HEADER,
        name = "Accept",
        description = """
          Sets the content type to return, defaults to application/json but can be set
          to application/geo+json to get the polyline as a geojson feature collection
          """,
        schema = @Schema(
          defaultValue = "application/json",
          allowableValues = {"application/json", "application/geo+json"}))
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "The polyline for the requested journey was successfully resolved",
        content = {
          @Content(
            mediaType = "application/json",
            schema = @Schema(implementation = MapJourneyRouteDto.class)),
          @Content(
            mediaType = "application/geo+json",
            schema = @Schema(hidden = true)),
        }),
      @ApiResponse(
        responseCode = "400",
        description = "One of the filter parameters is invalid",
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
  public @NonNull ResponseEntity<?> findMapPolylineByJourney(
    @PathVariable("id") @UUID(version = 5, allowNil = false) String id,
    @RequestParam(value = "includeCancelled", required = false) boolean includeCancelled,
    @RequestParam(value = "includeAdditional", required = false) boolean includeAdditional,
    @RequestParam(value = "allowFallbackComputation", required = false) boolean allowFallbackComputation,
    @RequestHeader(value = "Accept", defaultValue = "application/json") String acceptHeader
  ) {
    var journeyId = java.util.UUID.fromString(id);
    return this.mapService.polylineByJourneyId(journeyId, includeCancelled, includeAdditional, allowFallbackComputation)
      .map(routeInfo -> {
        // if geojson was requested instead of normal JSON
        var geoJsonRequested = acceptHeader.equalsIgnoreCase("application/geo+json");

        // scheduled polyline (without additional events) cannot change, can be cached for a day by the caller
        // however additional events might change, therefore the result shouldn't be cached by the caller
        var cacheControl = includeCancelled || includeAdditional
          ? CacheControl.noCache()
          : CacheControl.maxAge(Duration.ofDays(1));

        // convert the response to geojson if requested, in all other cases just respond with JSON
        if (geoJsonRequested) {
          var responseAsGeojson = this.journeyRouteGeoJsonConverter.convertToGeojson(routeInfo);
          return ResponseEntity.ok()
            .cacheControl(cacheControl)
            .contentType(APPLICATION_GEO_JSON)
            .body(responseAsGeojson);
        } else {
          return ResponseEntity.ok()
            .cacheControl(cacheControl)
            .contentType(MediaType.APPLICATION_JSON)
            .body(routeInfo);
        }
      })
      .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
