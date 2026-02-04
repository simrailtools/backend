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

package tools.simrail.backend.api.railcar;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.hibernate.validator.constraints.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.simrail.backend.common.railcar.RailcarProvider;

@CrossOrigin
@RestController
@RequestMapping("/sit-railcars/v1/")
@Tag(name = "railcars-v1", description = "SimRail Railcar APIs (Version 1)")
class RailcarV1Controller {

  private final RailcarProvider railcarProvider;
  private final RailcarDtoConverter railcarConverter;

  @Autowired
  RailcarV1Controller(
    @NonNull RailcarProvider railcarProvider,
    @NonNull RailcarDtoConverter railcarConverter
  ) {
    this.railcarProvider = railcarProvider;
    this.railcarConverter = railcarConverter;
  }

  /**
   * Fetch a single railcar using the given id.
   */
  @GetMapping("/by-id/{id}")
  @Operation(
    summary = "Finds a single railcar using the given id",
    parameters = {
      @Parameter(name = "id", description = "The id of the railcar to get the data of"),
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "The railcar with the given id was successfully resolved"),
      @ApiResponse(
        responseCode = "400",
        description = "One of the filter parameters is invalid",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "404",
        description = "No railcar can be found with the given id",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "500",
        description = "An internal error occurred while processing the request",
        content = @Content(schema = @Schema(hidden = true))),
    }
  )
  public @NonNull Optional<RailcarDto> findRailcarById(
    @PathVariable("id") @UUID(version = 4, allowNil = false) String id
  ) {
    return this.railcarProvider.findRailcarById(java.util.UUID.fromString(id)).map(this.railcarConverter);
  }

  /**
   * Fetch a single railcar using the given SimRail api id.
   */
  @GetMapping("/by-simrail-id")
  @Operation(
    summary = "Finds a single railcar using the given SimRail api id",
    parameters = {
      @Parameter(name = "id", description = "The SimRail api id of the railcar to get the data of"),
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "The railcar with the given id was successfully resolved"),
      @ApiResponse(
        responseCode = "400",
        description = "One of the filter parameters is invalid",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "404",
        description = "No railcar can be found with the given id",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "500",
        description = "An internal error occurred while processing the request",
        content = @Content(schema = @Schema(hidden = true))),
    }
  )
  public @NonNull Optional<RailcarDto> findRailcarByApiName(@RequestParam("id") @NotBlank String id) {
    return this.railcarProvider.findRailcarByApiId(id).map(this.railcarConverter);
  }

  /**
   * Fetches multiple (up to 250) railcars at the same time using their ids.
   */
  @PostMapping("/by-ids")
  @Operation(
    summary = "Get a batch of railcars (up to 250) by their id",
    requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "An array containing the ids of the railcars to resolve"
    ),
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "The railcars that were successfully resolved based on the given input ids"),
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
  public @NonNull List<RailcarDto> findRailcarsByIds(
    @RequestBody @Size(min = 1, max = 250) Set<@UUID(version = 4, allowNil = false) String> ids
  ) {
    return ids.stream()
      .map(java.util.UUID::fromString)
      .map(this.railcarProvider::findRailcarById)
      .flatMap(Optional::stream)
      .map(this.railcarConverter)
      .toList();
  }
}
