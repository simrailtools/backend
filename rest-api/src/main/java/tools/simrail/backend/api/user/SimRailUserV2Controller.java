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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.simrail.backend.api.shared.UserDto;

@CrossOrigin
@RestController
@RequestMapping("/sit-users/v2/")
@Tag(name = "users-v1", description = "SimRail User Data APIs (Version 1)")
class SimRailUserV2Controller {

  private final SimRailUserService userService;

  @Autowired
  SimRailUserV2Controller(@NonNull SimRailUserService userService) {
    this.userService = userService;
  }

  /**
   * Get a batch of detailed user information (up to 250).
   */
  @PostMapping("/batch")
  @Operation(
    summary = "Get a batch of detailed user information (up to 250)",
    description = """
      Get a batch of detailed user information (up to 250) in a single request. If an id is provided which can't be
      resolved to a user info, the id is skipped and there will be no reference to the id in the response array.
      """,
    requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "An array containing the base information (platform and id) of the users to resolve"
    ),
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "The request was processed successfully"),
      @ApiResponse(
        responseCode = "400",
        description = "The given request body is invalid",
        content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(
        responseCode = "500",
        description = "An internal error occurred while processing the request",
        content = @Content(schema = @Schema(hidden = true))),
    }
  )
  public @NonNull List<SimRailUserDto> findUserDetails(
    @RequestBody @Size(min = 1, max = 250) Set<@NotNull UserDto> steamIds
  ) {
    return this.userService.findUserDetails(steamIds);
  }
}
