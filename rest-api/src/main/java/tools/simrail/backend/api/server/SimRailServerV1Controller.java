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

package tools.simrail.backend.api.server;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import org.hibernate.validator.constraints.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@CrossOrigin
@RestController
@RequestMapping("/sit-servers/v1/")
@Tag(name = "servers-v1", description = "SimRail Server Data APIs (Version 1)")
class SimRailServerV1Controller {

  private final SimRailServerService serverService;

  @Autowired
  public SimRailServerV1Controller(@Nonnull SimRailServerService serverService) {
    this.serverService = serverService;
  }

  /**
   * Endpoint to get all registered servers. This endpoint does not support pagination as we don't expect a lot of
   * servers to be registered simultaneously currently.
   */
  @GetMapping
  @Operation(
    summary = "List all registered servers",
    parameters = {
      @Parameter(name = "includeOffline", description = "If servers that are offline should be included in the response"),
      @Parameter(name = "includeDeleted", description = "If servers that are deleted should be included in the response"),
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        content = @Content(
          mediaType = "application/json",
          array = @ArraySchema(schema = @Schema(implementation = SimRailServerDto.class))
        )),
      @ApiResponse(
        responseCode = "400",
        description = "One of the given filter parameters is invalid",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "500",
        description = "An internal error occurred while processing the request",
        content = @Content(schema = @Schema(hidden = true))),
    }
  )
  public @Nonnull List<SimRailServerDto> listServers(
    @RequestParam(value = "includeOffline", defaultValue = "false") boolean includeOffline,
    @RequestParam(value = "includeDeleted", defaultValue = "false") boolean includeDeleted
  ) {
    return this.serverService.listServers(includeOffline, includeDeleted);
  }

  /**
   * Endpoint to get data about a single server, by id.
   */
  @GetMapping("/by-id/{id}")
  @Operation(
    summary = "Get detail data about a single server by ID",
    parameters = {
      @Parameter(name = "id", description = "The id of the server to get"),
      @Parameter(
        name = "If-Modified-Since",
        in = ParameterIn.HEADER,
        description = "If provided the response body is empty in case the data didn't change since the given date"),
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = SimRailServerDto.class)
        )),
      @ApiResponse(
        responseCode = "304",
        description = "The request was successful but the content was not modified since the last request",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "400",
        description = "The given server ID is invalid",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "404",
        description = "No server with the given ID was found",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "500",
        description = "An internal error occurred while processing the request",
        content = @Content(schema = @Schema(hidden = true))),
    }
  )
  public @Nonnull ResponseEntity<SimRailServerDto> findServerById(
    @PathVariable("id") @UUID(version = 5, allowNil = false) String serverId
  ) {
    return this.serverService.findServerById(java.util.UUID.fromString(serverId))
      .map(server -> ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .lastModified(server.lastUpdated().toInstant())
        .body(server))
      .orElseGet(() -> ResponseEntity.notFound().build());
  }

  /**
   * Endpoint to get data about a single server, by code.
   */
  @GetMapping("/by-code/{code}")
  @Operation(
    summary = "Get detail data about a single server by code",
    parameters = {
      @Parameter(name = "code", description = "The code of the server to get"),
      @Parameter(
        name = "If-Modified-Since",
        in = ParameterIn.HEADER,
        description = "If provided the response body is empty in case the data didn't change since the given date"),
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        content = @Content(
          mediaType = "application/json",
          schema = @Schema(implementation = SimRailServerDto.class)
        )),
      @ApiResponse(
        responseCode = "304",
        description = "The request was successful but the content was not modified since the last request",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "400",
        description = "The given server code is invalid",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "404",
        description = "No server with the given code was found",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "500",
        description = "An internal error occurred while processing the request",
        content = @Content(schema = @Schema(hidden = true))),
    }
  )
  public @Nonnull ResponseEntity<SimRailServerDto> findServerByCode(
    @PathVariable("code") @NotEmpty String serverCode
  ) {
    return this.serverService.findServerByCode(serverCode)
      .map(server -> ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .lastModified(server.lastUpdated().toInstant())
        .body(server))
      .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
