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

package tools.simrail.backend.external.steam.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.ApiStatus;

/**
 * The summary data returned about a player from the steam web api endpoint.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public final class SteamUserSummary {

  /**
   * The steam id of the user.
   */
  @JsonProperty("steamid")
  private String id;
  /**
   * The display name of the player.
   */
  @JsonProperty("personaname")
  private String displayName;

  /**
   * The country code where the player lives.
   */
  @JsonProperty("loccountrycode")
  private String countryCode;
  /**
   * The state code where the player lives.
   */
  @JsonProperty("locstatecode")
  private String stateCode;

  /**
   * Url to the player steeam community profile page.
   */
  @JsonProperty("profileurl")
  private String communityProfileUrl;
  /**
   * Indicates if the user has a community profile set up.
   */
  private boolean communityProfileSetup;
  /**
   * Indicates if the community profile of the user is publicly visible.
   */
  private boolean communityProfileVisible;

  /**
   * The hash of the avatar used by the profile.
   */
  @JsonProperty("avatarhash")
  private String avatarHash;
  /**
   * Url to the player avatar image in 32x32 ratio.
   */
  @JsonProperty("avatar")
  private String avatarSmallUrl;
  /**
   * Url to the player avatar image in 64x64 ratio.
   */
  @JsonProperty("avatarmedium")
  private String avatarMediumUrl;
  /**
   * Url to the player avatar image in 184x184 ratio.
   */
  @JsonProperty("avatarfull")
  private String avatarFullUrl;

  /**
   * Deserializes the profile state value returned from the api. For internal use only.
   *
   * @param profileState the profile state. Can be {@code 1} or {@code 0}.
   */
  @ApiStatus.Internal
  @JsonProperty("profilestate")
  public void deserializeProfileState(int profileState) {
    this.communityProfileSetup = profileState == 1;
  }

  /**
   * Deserializes the community profile visibility state returned from the api. For internal use only.
   *
   * @param communityVisibilityState the visibility state. Can be {@code 1} (private) or {@code 3} (public).
   */
  @ApiStatus.Internal
  @JsonProperty("communityvisibilitystate")
  public void deserializeCommunityVisibilityState(int communityVisibilityState) {
    this.communityProfileVisible = communityVisibilityState == 3;
  }
}
