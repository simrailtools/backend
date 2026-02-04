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

package tools.simrail.backend.external.playerdb.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.node.ObjectNode;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class PlayerDbPlayer {

  /**
   * The platform-specific id of the player.
   */
  @JsonProperty("id")
  private String id;
  /**
   * The display name of the player.
   */
  @JsonProperty("username")
  private String username;
  /**
   * The url to the player avatar image.
   */
  @JsonProperty("avatar")
  private String avatarUrl;

  /**
   * Additional metadata returned by the api.
   */
  @JsonProperty("meta")
  private ObjectNode metadata;

  /**
   * Wrapper object returned by the api around the actual player data.
   */
  public record Wrapper(@JsonProperty("player") PlayerDbPlayer player, @JsonProperty("status") Integer status) {

  }
}
