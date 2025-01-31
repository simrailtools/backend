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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import java.time.Duration;
import org.hibernate.validator.constraints.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.simrail.backend.api.map.dto.MapJourneyRouteDto;

@Validated
@CrossOrigin
@RestController
@RequestMapping("/sit-maps/v1/")
@Tag(name = "maps-v1", description = "SimRail Map Data APIs (Version 1)")
class SimRailMapController {

  private final SimRailMapService mapService;

  @Autowired
  public SimRailMapController(@Nonnull SimRailMapService mapService) {
    this.mapService = mapService;
  }

  /**
   * Gets the polyline for a specific journey.
   */
  @GetMapping("/polyline/by-journey/{id}")
  @Operation(
    summary = "Get the polyline for a specific journey",
    parameters = {
      @Parameter(name = "id", description = "The id of the journey to get the polyline for"),
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "The polyline for the requested journey was successfully resolved"),
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
  public @Nonnull ResponseEntity<MapJourneyRouteDto> polylineByJourney(
    @PathVariable("id") @UUID(version = 5, allowNil = false) String id
  ) {
    var journeyId = java.util.UUID.fromString(id);
    return this.mapService.polylineByJourneyId(journeyId)
      .map(routeInfo -> ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(Duration.ofDays(1)))
        .body(routeInfo))
      .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
