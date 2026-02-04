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

package tools.simrail.backend.api.user.loader;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import tools.simrail.backend.api.shared.UserPlatform;
import tools.simrail.backend.api.user.SimRailUserDto;
import tools.simrail.backend.external.playerdb.PlayerDbApiClient;
import tools.simrail.backend.external.playerdb.model.PlayerDbResponseWrapper;

@Component
final class XBoxUserCacheLoader implements UserCacheLoader {

  /**
   * Regex to validate that an input is in the correct xuid format.
   */
  private static final Pattern XUID_PATTERN = Pattern.compile("^\\d{15,16}$");
  /**
   * Url prefix to an xbox profile, the username must be appended as the suffix to it.
   */
  private static final String XBOX_PROFILE_URL_PREFIX = "https://xbox.com/play/user/";

  private final PlayerDbApiClient playerDbApiClient;

  @Autowired
  public XBoxUserCacheLoader(@NonNull PlayerDbApiClient playerDbApiClient) {
    this.playerDbApiClient = playerDbApiClient;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull UserPlatform targetPlatform() {
    return UserPlatform.XBOX;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull Optional<SimRailUserDto> load(@NonNull String key) {
    var idMatcher = XUID_PATTERN.matcher(key);
    if (!idMatcher.matches()) {
      throw new IllegalArgumentException("Invalid xuid: " + key);
    }

    var response = this.playerDbApiClient.getXboxPlayer(key);
    var responseCode = response.getCode();
    if (!responseCode.equals(PlayerDbResponseWrapper.RESPONSE_CODE_SUCCESS)) {
      // we already validated the id, so the only reason to get an "invalid id" response is that the user does not exist
      var doesNotExist = responseCode.equals(PlayerDbResponseWrapper.RESPONSE_CODE_XBOX_BAD_RESPONSE_CODE)
        && Objects.equals(response.getData().status(), HttpStatus.BAD_REQUEST.value());
      if (doesNotExist) {
        return Optional.empty();
      }

      throw new IllegalStateException("Upstream api returned error code " + responseCode);
    }

    var playerData = response.getData().player();
    var locationRaw = playerData.getMetadata().path("location").stringValue(null);
    var location = StringUtils.hasLength(locationRaw) ? locationRaw : null;
    var profileUrl = XBOX_PROFILE_URL_PREFIX + playerData.getUsername();
    var user = new SimRailUserDto(key, playerData.getUsername(), profileUrl, playerData.getAvatarUrl(), location);
    return Optional.of(user);
  }
}
