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

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class PlayerDbResponseWrapper {

  /**
   * Response code message returned by the api if a player was found.
   */
  public static final String RESPONSE_CODE_SUCCESS = "player.found";

  // error code list can be found here: https://github.com/nodecraft/playerdb/blob/main/src/error_codes
  public static final String RESPONSE_CODE_STEAM_ID_INVALID = "steam.invalid_id";
  public static final String RESPONSE_CODE_STEAM_NON_JSON = "steam.non_json";
  public static final String RESPONSE_CODE_STEAM_API_FAILURE = "steam.api_failure";
  public static final String RESPONSE_CODE_STEAM_API_RATELIMIT = "steam.rate_limited";

  public static final String RESPONSE_CODE_XBOX_NON_JSON = "xbox.non_json";
  public static final String RESPONSE_CODE_XBOX_NOT_FOUND = "xbox.not_found";
  public static final String RESPONSE_CODE_XBOX_API_FAILURE = "xbox.api_failure";
  public static final String RESPONSE_CODE_XBOX_BAD_RESPONSE = "xbox.bad_response";
  public static final String RESPONSE_CODE_XBOX_API_RATELIMIT = "xbox.rate_limited";
  public static final String RESPONSE_CODE_XBOX_BAD_RESPONSE_CODE = "xbox.bad_response_code";

  /**
   * The status code message returned by the api.
   */
  @JsonProperty("code")
  private String code;
  /**
   * The data that was returned by the api.
   */
  @JsonProperty("data")
  private PlayerDbPlayer.Wrapper data;
}
