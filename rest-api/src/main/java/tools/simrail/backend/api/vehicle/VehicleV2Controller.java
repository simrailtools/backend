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

package tools.simrail.backend.api.vehicle;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.hibernate.validator.constraints.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.simrail.backend.api.vehicle.dto.VehicleSequenceDto;

@CrossOrigin
@RestController
@RequestMapping("/sit-vehicles/v2/")
@Tag(name = "vehicles-v2", description = "SimRail Server Vehicle APIs (Version 2)")
class VehicleV2Controller {

  private final VehicleService vehicleService;

  @Autowired
  VehicleV2Controller(@NonNull VehicleService vehicleService) {
    this.vehicleService = vehicleService;
  }

  /**
   * Get the vehicle composition of a journey.
   */
  @GetMapping("/by-journey/{id}")
  @Operation(
    summary = "Get the vehicle composition of a journey",
    parameters = {
      @Parameter(name = "id", description = "The id of the journey to get the vehicle composition of"),
      @Parameter(
        name = "If-Modified-Since",
        in = ParameterIn.HEADER,
        description = "If provided the response body is empty in case the data didn't change since the given date",
        schema = @Schema(type = "string", format = "date-time", examples = "Wed, 21 Oct 2015 07:28:00 GMT")),
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "The vehicle sequence for the journey with the given id was successfully resolved"),
      @ApiResponse(
        responseCode = "304",
        description = "The request was successful but the content was not modified since the last request",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "400",
        description = "The given journey id is invalid",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "404",
        description = "No vehicle sequence for the journey with the given id is known",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "500",
        description = "An internal error occurred while processing the request",
        content = @Content(schema = @Schema(hidden = true))),
    }
  )
  public @NonNull ResponseEntity<VehicleSequenceDto> findVehicleCompositionByJourneyId(
    @PathVariable("id") @UUID(version = 5, allowNil = false) String id
  ) {
    return this.vehicleService.findByJourneyId(java.util.UUID.fromString(id))
      .map(vehicleComposition -> ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .lastModified(vehicleComposition.lastUpdated())
        .body(vehicleComposition))
      .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
