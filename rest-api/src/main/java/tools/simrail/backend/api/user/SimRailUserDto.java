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

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for data about a single steam profile.
 */
record SimRailUserDto(
  @Schema(description = "The id of the steam profile")
  @NotNull @Pattern(regexp = "^7656119\\d{10}$") String id,
  @Schema(description = "The name of the steam profile")
  @NotNull @NotBlank String name,
  @Schema(description = "The avatar hash of the profile, can be used to retrieve the image from Steam")
  @NotNull @NotBlank String avatarHash,
  @Schema(description = "The url to the profile")
  @NotNull @NotBlank String profileUrl,
  @Schema(description = "The ISO 3166-1 alpha-2 country code where the user resides, null if not visible or set", types = {"null"})
  @Nullable String countryCode,
  @Schema(description = "Indicates if the steam profile page is publicly visible")
  @NotNull boolean profileVisible
) {

}
