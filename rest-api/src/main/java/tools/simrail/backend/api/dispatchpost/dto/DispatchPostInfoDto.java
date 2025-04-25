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

package tools.simrail.backend.api.dispatchpost.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * DTO for information about a dispatch post.
 */
public record DispatchPostInfoDto(
  @Schema(description = "The id of the dispatch post")
  @NotNull UUID id,
  @Schema(description = "The name of the dispatch post")
  @NotNull @NotBlank String name,
  @Schema(description = "The id of the point where the dispatch post is located")
  @NotNull UUID pointId,
  @Schema(description = "The id of the server for which the dispatch post info is valid")
  @NotNull UUID serverId,
  @Schema(description = "The time (ISO-8601 with offset) when the dispatch post was last updated")
  @NotNull OffsetDateTime lastUpdated,
  @Schema(description = "The time (ISO-8601 with offset) when the dispatch post was registered in the SimRail backend")
  @NotNull OffsetDateTime registeredSince,
  @Schema(description = "The position where the dispatch post is located")
  @NotNull DispatchPointGeoPositionDto position,
  @Schema(description = "A list of image urls displaying the dispatch post")
  @NotNull Set<String> images,
  @Schema(description = "A list of steam ids of the users that are currently dispatching the dispatch post")
  @NotNull Set<String> dispatchers,
  @Schema(description = "The difficulty rating of the dispatch post, from 1 (easy) to 5 (hard)")
  @NotNull @Min(1) @Max(5) int difficulty,
  @Schema(description = "Indicates if the dispatch post was deleted in the SimRail backend")
  @NotNull boolean deleted
) {

}
