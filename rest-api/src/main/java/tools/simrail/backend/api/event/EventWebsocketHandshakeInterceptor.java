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

package tools.simrail.backend.api.event;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;
import tools.simrail.backend.api.event.dto.EventFrameType;

/**
 * Interceptor to validate a websocket request before it gets upgraded.
 */
@Component
final class EventWebsocketHandshakeInterceptor implements HandshakeInterceptor {

  static final String UPDATE_FRAMES_ATTRIBUTE = "requested_event_frames";

  @Override
  public boolean beforeHandshake(
    @Nonnull ServerHttpRequest request,
    @Nonnull ServerHttpResponse response,
    @Nonnull WebSocketHandler wsHandler,
    @Nonnull Map<String, Object> attributes
  ) {
    // check that the client doesn't open too many connections
    var remoteAddress = request.getRemoteAddress();
    if (false) { // todo: check connection count of address
      response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
      return false;
    }

    // check if the requested events are present in the uri
    var requestUriComponents = UriComponentsBuilder.fromUri(request.getURI()).build();
    var requestQueryParams = requestUriComponents.getQueryParams();
    var requestedEventTypes = requestQueryParams.get("event");
    if (requestedEventTypes == null || requestedEventTypes.isEmpty()) {
      response.setStatusCode(HttpStatus.BAD_REQUEST);
      return false;
    }

    try {
      //
      Map<EventFrameType, List<String>> requestedTypes = new HashMap<>();
      for (var requestedEventType : requestedEventTypes) {
        var parts = requestedEventType.split(",");
        if (parts.length == 0) {
          response.setStatusCode(HttpStatus.BAD_REQUEST);
          return false;
        }

        var frameType = switch (parts[0].toLowerCase(Locale.ROOT)) {
          case "server" -> parts.length > 1 ? EventFrameType.SERVER : null;
          case "journey" -> parts.length == 2 ? EventFrameType.JOURNEY : null;
          case "dispatch_post" -> parts.length == 2 ? EventFrameType.DISPATCH_POST : null;
          default -> null;
        };
        if (frameType == null || requestedTypes.containsKey(frameType)) {
          response.setStatusCode(HttpStatus.BAD_REQUEST);
          return false;
        }

        var ids = IntStream.range(1, parts.length)
          .mapToObj(index -> UUID.fromString(parts[index]))
          .filter(id -> id.version() == 5 && id.getMostSignificantBits() != 0 && id.getLeastSignificantBits() != 0)
          .map(UUID::toString)
          .toList();
        if (ids.isEmpty()) {
          response.setStatusCode(HttpStatus.BAD_REQUEST);
          return false;
        }

        requestedTypes.put(frameType, ids);
      }

      // register the requested types as an attribute
      attributes.put(UPDATE_FRAMES_ATTRIBUTE, requestedTypes);
      return true;
    } catch (Exception exception) {
      // some sort of issue during request parsing, just reject the request
      response.setStatusCode(HttpStatus.BAD_REQUEST);
      return false;
    }
  }

  @Override
  public void afterHandshake(
    @Nonnull ServerHttpRequest request,
    @Nonnull ServerHttpResponse response,
    @Nonnull WebSocketHandler wsHandler,
    @Nullable Exception exception
  ) {
    // no-op
  }
}
