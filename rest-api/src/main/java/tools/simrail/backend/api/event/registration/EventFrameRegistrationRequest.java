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

package tools.simrail.backend.api.event.registration;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.UUID;
import java.util.regex.Pattern;
import tools.simrail.backend.api.event.dto.EventFrameType;

/**
 * A request sent by a client to register or unregister for an event frame.
 *
 * @param action    the action that was requested (register or unregister).
 * @param frameType the frame type that is associated with the current request.
 * @param serverId  the server id that is associated with the current request.
 * @param dataId    the optional specific data id, null if the id was replaced by a wildcard.
 * @param version   the version of the data that was requested.
 */
public record EventFrameRegistrationRequest(
  @Nonnull Action action,
  @Nonnull EventFrameType frameType,
  @Nonnull UUID serverId,
  @Nullable UUID dataId,
  int version
) {

  /**
   * The maximum number of chars a registration request text can have.
   */
  public static final int MAX_REQUEST_TEXT_LENGTH = 132;
  /**
   * Regex to parse a text string into a frame registration request.
   */
  // https://regex101.com/r/jrImy8/1
  private static final Pattern REQUEST_TEXT_PATTERN = Pattern.compile(
    "^sit-events/(subscribe|unsubscribe)/([a-z-]{1,30})/v(\\d{1,3})/([0-9a-fA-F-]{36})/(\\+|[0-9a-fA-F-]{36})$");

  /**
   * Parses the given input text into a frame registration request, returning null if the request is malformed.
   *
   * @param text the text to parse into a request.
   * @return the parsed request from the given input or null if the request is malformed.
   */
  public static @Nullable EventFrameRegistrationRequest parseFromText(@Nonnull String text) {
    var matcher = REQUEST_TEXT_PATTERN.matcher(text);
    if (matcher.matches()) {
      // parse action
      var actionInput = matcher.group(1);
      var action = actionInput.equals("subscribe") ? Action.SUBSCRIBE : Action.UNSUBSCRIBE;

      // parse requested frame type
      var frameTypeInput = matcher.group(2);
      var frameType = EventFrameType.BY_REGISTRATION_NAME.get(frameTypeInput);
      if (frameType == null) {
        return null;
      }

      // parse requested data version
      var versionInput = matcher.group(3);
      var version = Integer.parseInt(versionInput);

      // parse & validate requested server id
      var serverIdInput = matcher.group(4);
      var serverId = parseUuidSafe(serverIdInput);
      if (serverId == null || serverId.version() != 5) {
        return null;
      }

      // parse requested data id
      var dataIdInput = matcher.group(5);
      var isWildcardId = dataIdInput.equals("+");
      var dataId = isWildcardId ? null : parseUuidSafe(dataIdInput);
      if (dataId == null && !isWildcardId) {
        return null;
      }

      return new EventFrameRegistrationRequest(action, frameType, serverId, dataId, version);
    }

    return null;
  }

  /**
   * Tries to parse the given string into an uuid, returning null if the input is malformed.
   *
   * @param input the input to parse into an uuid.
   * @return the parsed uuid or null if the input is malformed.
   */
  private static @Nullable UUID parseUuidSafe(@Nonnull String input) {
    try {
      return UUID.fromString(input);
    } catch (IllegalArgumentException _) {
      return null;
    }
  }

  /**
   * The possible actions that a client can request.
   */
  public enum Action {
    SUBSCRIBE,
    UNSUBSCRIBE,
  }
}
