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
  @NotNull UUID id,
  @NotNull @NotBlank String name,
  @NotNull UUID pointId,
  @NotNull UUID serverId,
  @NotNull OffsetDateTime lastUpdated,
  @NotNull OffsetDateTime registeredSince,
  @NotNull DispatchPointGeoPositionDto position,
  @NotNull Set<String> images,
  @NotNull Set<String> dispatchers,
  @NotNull @Min(1) @Max(5) int difficulty,
  @NotNull boolean deleted
) {

}
