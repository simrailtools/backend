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

package tools.simrail.backend.api.dispatchpost;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.hibernate.validator.constraints.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tools.simrail.backend.api.dispatchpost.dto.DispatchPostInfoDto;
import tools.simrail.backend.api.pagination.PaginatedResponseDto;

@CrossOrigin
@RestController
@RequestMapping("/sit-dispatch-posts/v2/")
@Tag(name = "dispatch-post-v1", description = "SimRail Dispatch Post Data APIs (Version 1)")
class DispatchPostV2Controller {

  private final DispatchPostService dispatchPostService;

  @Autowired
  DispatchPostV2Controller(@NonNull DispatchPostService dispatchPostService) {
    this.dispatchPostService = dispatchPostService;
  }

  /**
   * Get the detail information of a single dispatch post by its id.
   */
  @GetMapping("/by-id/{id}")
  @Operation(
    summary = "Get the detail information of a single dispatch post by its id",
    parameters = {
      @Parameter(name = "id", description = "The id of the dispatch post to get"),
      @Parameter(
        name = "If-Modified-Since",
        in = ParameterIn.HEADER,
        description = "If provided the response body is empty in case the data didn't change since the given date",
        schema = @Schema(type = "string", format = "date-time", examples = "Wed, 21 Oct 2015 07:28:00 GMT")),
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "The dispatch post was successfully resolved based on the given id"),
      @ApiResponse(
        responseCode = "304",
        description = "The request was successful but the content was not modified since the last request",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "400",
        description = "The given dispatch post id is invalid",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "404",
        description = "No dispatch post with the given id exists",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "500",
        description = "An internal error occurred while processing the request",
        content = @Content(schema = @Schema(hidden = true))),
    }
  )
  public @NonNull ResponseEntity<DispatchPostInfoDto> findDispatchPostById(
    @PathVariable("id") @UUID(version = 5, allowNil = false) String id
  ) {
    return this.dispatchPostService.findById(java.util.UUID.fromString(id))
      .map(point -> ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .lastModified(point.lastUpdated())
        .body(point))
      .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /**
   * Finds a dispatch post using the given filter parameters. If multiple filter parameters are given, they are chained
   * with a logical AND operation.
   */
  @GetMapping("/find")
  @Operation(
    summary = "Find dispatch posts based on the given filter parameters",
    description = """
      Finds dispatch posts paginated based on the given filter parameters. If multiple filter parameters are provided,
      they get chained into a logical AND chain. If no filter parameters are provided, this endpoint acts like a
      listing endpoint.
      """,
    parameters = {
      @Parameter(name = "serverId", description = "The id of the server on which the dispatch posts should be located"),
      @Parameter(name = "difficulty", description = "The difficulty of the dispatch posts to return (1-5)"),
      @Parameter(name = "pointId", description = "The id of the point with which the dispatch post is associated"),
      @Parameter(name = "deleted", description = "If the post should be deleted (removed from the SimRail backend)"),
      @Parameter(name = "page", description = "The page of elements to return, defaults to 1"),
      @Parameter(name = "limit", description = "The maximum items to return per page, defaults to 20"),
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "The dispatch posts were successfully resolved based on the given filter parameters"),
      @ApiResponse(
        responseCode = "400",
        description = "One or multiple of the given filter parameters were invalid",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "500",
        description = "An internal error occurred while processing the request",
        content = @Content(schema = @Schema(hidden = true))),
    }
  )
  public @NonNull PaginatedResponseDto<DispatchPostInfoDto> findDispatchPosts(
    @RequestParam(name = "serverId", required = false) @UUID(version = 5, allowNil = false) String serverId,
    @RequestParam(name = "difficulty", required = false) @Min(1) @Max(5) Integer difficulty,
    @RequestParam(name = "pointId", required = false) @UUID(version = 4, allowNil = false) String pointId,
    @RequestParam(name = "deleted", required = false) Boolean deleted,
    @RequestParam(name = "page", required = false) @Min(1) Integer page,
    @RequestParam(name = "limit", required = false) @Min(1) @Max(100) Integer limit
  ) {
    var serverIdFilter = serverId == null ? null : java.util.UUID.fromString(serverId);
    var pointIdFilter = pointId == null ? null : java.util.UUID.fromString(pointId);
    return this.dispatchPostService.find(serverIdFilter, difficulty, pointIdFilter, deleted, limit, page);
  }
}
