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

package tools.simrail.backend.api.point;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.hibernate.validator.constraints.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.simrail.backend.api.pagination.PaginatedResponseDto;
import tools.simrail.backend.api.point.dto.PointInfoDto;

@CrossOrigin
@RestController
@RequestMapping("/sit-points/v1/")
@Tag(name = "points-v1", description = "SimRail Point Data APIs (Version 1)")
class SimRailPointV1Controller {

  private final SimRailPointService pointService;

  @Autowired
  public SimRailPointV1Controller(@Nonnull SimRailPointService pointService) {
    this.pointService = pointService;
  }

  /**
   * Lists all points that are known.
   */
  @GetMapping
  @Operation(
    summary = "List all points that are registered",
    parameters = {
      @Parameter(name = "countries", description = "Optional list of countries in which the points may be located"),
      @Parameter(name = "page", description = "The page of elements to return, defaults to 1"),
      @Parameter(name = "limit", description = "The maximum items to return per page, defaults to 20"),
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "The points were successfully resolved based on the given filter parameters"),
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
  public @Nonnull PaginatedResponseDto<PointInfoDto> listPoints(
    @RequestParam(name = "countries", required = false) List<@Pattern(regexp = "[A-Z]{3}") String> countries,
    @RequestParam(name = "page", required = false) @Min(1) Integer page,
    @RequestParam(name = "limit", required = false) @Min(1) Integer limit
  ) {
    return this.pointService.findPointsByCountry(countries, page, limit);
  }

  /**
   * Get a point by its id.
   */
  @GetMapping("/by-id/{id}")
  @Operation(
    summary = "Get the full data of a point by its id",
    parameters = {
      @Parameter(name = "id", description = "The id of the point to get the data of"),
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "The point with the given id was successfully resolved"),
      @ApiResponse(
        responseCode = "400",
        description = "One of the filter parameters is invalid",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "404",
        description = "No point can be found with the given id",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "500",
        description = "An internal error occurred while processing the request",
        content = @Content(schema = @Schema(hidden = true))),
    }
  )
  public @Nonnull Optional<PointInfoDto> findPointById(
    @PathVariable("id") @UUID(version = 4, allowNil = false) String id
  ) {
    return this.pointService.findPointById(java.util.UUID.fromString(id));
  }

  /**
   * Batch request to get up to 250 points by their id at the same time.
   */
  @PostMapping("/by-ids")
  @Operation(
    summary = "Get a batch of points (up to 250) by their id",
    description = """
      Get a batch of points (up to 250) in a single request. If an id is provided which doesn't have a point associated,
      the id is skipped and there will be no reference to the id in the response array
      """,
    requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "An array containing the ids of the points to resolve"
    ),
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "The points that were successfully resolved based on the given input ids"),
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
  public @Nonnull List<PointInfoDto> findPointsById(
    @RequestBody @Size(min = 1, max = 250) List<@UUID(version = 4, allowNil = false) String> ids
  ) {
    return ids.stream()
      .map(java.util.UUID::fromString)
      .map(this.pointService::findPointById)
      .flatMap(Optional::stream)
      .toList();
  }

  /**
   * Get a point by its SimRail point id.
   */
  @GetMapping("/by-point-id/{id}")
  @Operation(
    summary = "Get a point by its SimRail point id",
    description = """
      Gets a point by its SimRail point id". Note that the resulting points are grouped by their operational unit, for
      example '2528' (Małogoszcz) and '5460' (Małogoszcz PZS R35) will both return 'Małogoszcz'. Also note that some
      points might not return any result if they are too close together and one point represents them enough
      (for example the case for 'Zawiercie' and 'Zawiercie GT')
      """,
    parameters = {
      @Parameter(name = "id", description = "The id of the point in the SimRail backend, e.g. 2371")
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "The point with the given id was successfully resolved"),
      @ApiResponse(
        responseCode = "400",
        description = "One of the filter parameters is invalid",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "404",
        description = "No point can be found with the given id",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "500",
        description = "An internal error occurred while processing the request",
        content = @Content(schema = @Schema(hidden = true))),
    }
  )
  public @Nonnull Optional<PointInfoDto> findPointBySimRailPointId(
    @PathVariable("id") @Pattern(regexp = "[0-9]{2,4}") String pointId
  ) {
    return this.pointService.findPointByPointId(pointId);
  }

  /**
   * Finds points whose name are matching the given search query.
   */
  @GetMapping("/by-name/{searchQuery}")
  @Operation(
    summary = "Finds points whose name are matching the given search query",
    description = """
      Fuzzy searches points by the given name search query. For example the search input 'Lazy' will return 'Łazy',
      'Łazy Łc', 'Łazy Ła' etc. Wildcard search patterns are not supported by this endpoint. Results are sorted by match
      with the given search query, DESC (best match first).
      """,
    parameters = {
      @Parameter(name = "searchQuery", description = "The search query for the point (must be between 3 and 35 chars)"),
      @Parameter(name = "countries", description = "Optional list of countries in which the points may be located"),
      @Parameter(name = "limit", description = "The maximum results to return (between 1 and 25, defaults to 10)"),
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "The search was successfully executed based on the given search query"),
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
  public @Nonnull List<PointInfoDto> findPointByName(
    @PathVariable(name = "searchQuery") @NotBlank @Pattern(regexp = "^.{3,35}$") String searchQuery,
    @RequestParam(name = "countries", required = false) List<@Pattern(regexp = "[A-Z]{3}") String> countries,
    @RequestParam(name = "limit", required = false) @Min(1) @Max(25) Integer limit
  ) {
    var responseLimit = Objects.requireNonNullElse(limit, 10);
    return this.pointService.findPointsByName(searchQuery, countries, responseLimit);
  }

  /**
   * Finds points that are located in the specified radius of a given geo position.
   */
  @GetMapping("/by-position")
  @Operation(
    summary = "Finds points that are located in a specified around around a given geo position",
    description = """
      Finds points that are in the given radius around the given geo position. Results are sorted by their distance to
      the given geo point, ASC (nearest point first).
      """,
    parameters = {
      @Parameter(name = "latitude", description = "Latitude from which to search in the given radius"),
      @Parameter(name = "longitude", description = "Longitude from which to search in the given radius"),
      @Parameter(name = "radius", description = "The radius to search around  position, in meters (defaults to 200m)"),
      @Parameter(name = "countries", description = "Optional list of countries in which the points may be located"),
      @Parameter(name = "limit", description = "The maximum results to return (between 1 and 25, defaults to 10)"),
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "The search was successfully executed based on the given filter parameters"),
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
  public @Nonnull List<PointInfoDto> findPointByPosition(
    @RequestParam(name = "latitude") @Min(-90) @Max(90) double latitude,
    @RequestParam(name = "longitude") @Min(-180) @Max(180) double longitude,
    @RequestParam(name = "radius", required = false) @Min(100) @Max(10_000) Integer radius,
    @RequestParam(name = "countries", required = false) List<@Pattern(regexp = "[A-Z]{3}") String> countries,
    @RequestParam(name = "limit", required = false) @Min(1) @Max(25) Integer limit
  ) {
    var responseLimit = Objects.requireNonNullElse(limit, 10);
    var returnRadius = Objects.requireNonNullElse(radius, 200);
    return this.pointService.findPointsAroundPosition(latitude, longitude, returnRadius, countries, responseLimit);
  }
}
