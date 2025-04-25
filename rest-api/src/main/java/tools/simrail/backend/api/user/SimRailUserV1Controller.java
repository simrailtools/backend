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

package tools.simrail.backend.api.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@CrossOrigin
@RestController
@RequestMapping("/sit-users/v1/")
@Tag(name = "users-v1", description = "SimRail User Data APIs (Version 1)")
class SimRailUserV1Controller {

  private final SimRailUserService userService;

  @Autowired
  public SimRailUserV1Controller(@Nonnull SimRailUserService userService) {
    this.userService = userService;
  }

  /**
   * Finds the user information of all provided steam ids.
   */
  @PostMapping("/by-steam-ids")
  @Operation(
    summary = "Get a batch of users (up to 100) by their steam id",
    description = """
      Get a batch of users (up to 100) in a single request. If an id is provided which can't be resolved to a user info,
      the id is skipped and there will be no reference to the id in the response array. The provided ids must all be in
      the SteamID64 format.
      """,
    requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "An array containing the steam ids of the users to resolve"
    ),
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "The users that were successfully resolved based on the given input ids"),
      @ApiResponse(
        responseCode = "400",
        description = "One of the given steam ids is in an incorrect format",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "500",
        description = "An internal error occurred while processing the request",
        content = @Content(schema = @Schema(hidden = true))),
    }
  )
  public @Nonnull List<SimRailUserDto> findUsersBySteamIds(
    @RequestBody @Size(min = 1, max = 100) Set<@Pattern(regexp = "^7656119\\d{10}$") String> steamIds
  ) {
    return this.userService.findUsersBySteamIds(steamIds);
  }
}
