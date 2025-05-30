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

package tools.simrail.backend.api.server;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import tools.simrail.backend.common.server.SimRailServerRegion;
import tools.simrail.backend.common.server.SimRailServerScenery;

/**
 * DTO for SimRail servers.
 */
record SimRailServerDto(
  @Schema(description = "The id of the server")
  @NotNull UUID id,
  @Schema(description = "The code of the server")
  @NotNull @NotBlank String code,
  @Schema(description = "The timezone id of the server according to the ISO-8601 specification")
  @NotNull @NotBlank String timezoneId,
  @Schema(description = "The difference in hours between the UTC time and the time on the server")
  @NotNull int utcOffsetHours,
  @Schema(description = "The region where the server is located")
  @NotNull SimRailServerRegion region,
  @Schema(description = "Tags of the server, for example providing detail information about the moderation status")
  @NotNull List<String> tags,
  @Schema(description = "The language spoken on the server, null if the server is international and not specialised", types = {"null"})
  @Nullable String spokenLanguage,
  @Schema(description = "The scenery (map part) that is available on the server")
  @NotNull SimRailServerScenery scenery,
  @Schema(description = "The time (ISO-8601 with offset) when the data of the server was last updated")
  @NotNull OffsetDateTime lastUpdated,
  @Schema(description = "The time (ISO-8601 with offset) when the server was initially registered in the SimRail backend")
  @NotNull OffsetDateTime registeredSince,
  @Schema(description = "If the server was online during the last collection")
  @NotNull boolean online,
  @Schema(description = "If the server is no longer registered in the SimRail backend")
  @NotNull boolean deleted
) {

}
