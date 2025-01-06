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

package tools.simrail.backend.api.event.session;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.simrail.backend.api.event.dto.EventFrameType;

/**
 * A single established websocket connection to the server.
 */
public final class EventWebsocketSession {

  private static final int MAX_SECONDS_UNTIL_OBSOLETE = 30;

  private final WebSocketSession webSocketSession;
  private final Map<EventFrameType, List<String>> requestedFrameTypes;

  // the last time a pong was received from the client
  private Instant lastPongReceived;

  EventWebsocketSession(
    @Nonnull WebSocketSession webSocketSession,
    @Nonnull Map<EventFrameType, List<String>> requestedFrameTypes
  ) {
    this.webSocketSession = webSocketSession;
    this.requestedFrameTypes = requestedFrameTypes;
    this.lastPongReceived = Instant.now();
  }

  /**
   * Get if the session requested data of the given type on the given server.
   *
   * @param type     the type of frame to check for requests.
   * @param serverId the id of the server to check if frames are requested for it.
   * @return true of the client requested frames of the given type for the given server, false otherwise.
   */
  public boolean requestedFrame(@Nonnull EventFrameType type, @Nonnull String serverId) {
    var requestedFrames = this.requestedFrameTypes.get(type);
    return requestedFrames != null && requestedFrames.contains(serverId);
  }

  /**
   * Sends out a ping message to the client.
   */
  public void sendPing() throws IOException {
    this.webSocketSession.sendMessage(new PingMessage());
  }

  /**
   * Sends out a pong message to the client.
   */
  public void sendPong() throws IOException {
    this.webSocketSession.sendMessage(new PongMessage());
  }

  /**
   * Sends out a text message with the data "pong".
   */
  public void sendPongText() throws IOException {
    this.sendText("pong");
  }

  /**
   * Sends the given text as a text frame to the websocket connection.
   *
   * @param text the text data to send to the websocket connection.
   */
  public void sendText(@Nonnull String text) throws IOException {
    this.webSocketSession.sendMessage(new TextMessage(text));
  }

  /**
   * Updates the last time when a pong message was received from the client.
   */
  public void handlePongReceive() {
    this.lastPongReceived = Instant.now();
  }

  /**
   * Get if the client connection is obsolete (inactive for 30 seconds).
   *
   * @param now the timestamp to use for the base comparison.
   * @return true if the client is idling and obsolete, false if the client is still active.
   */
  public boolean isObsolete(@Nonnull Instant now) {
    var elapsed = Duration.between(this.lastPongReceived, now);
    return !this.webSocketSession.isOpen() || elapsed.toSeconds() > MAX_SECONDS_UNTIL_OBSOLETE;
  }

  /**
   * Closes the websocket connection to the client with a normal status code.
   */
  public void close() throws Exception {
    this.webSocketSession.close(CloseStatus.NORMAL);
  }
}
