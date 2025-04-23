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

package tools.simrail.backend.api.event;

import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.simrail.backend.api.event.registration.EventFrameRegistrationRequest;
import tools.simrail.backend.api.event.session.EventSessionManager;

/**
 * Handler for a single fully upgraded websocket session, executing the event listening and message handling.
 */
@Component
final class EventWebsocketHandler implements WebSocketHandler {

  private final EventSessionManager sessionManager;

  @Autowired
  public EventWebsocketHandler(@Nonnull EventSessionManager sessionManager) {
    this.sessionManager = sessionManager;
  }

  @Override
  public void afterConnectionEstablished(@Nonnull WebSocketSession session) {
    session.setBinaryMessageSizeLimit(0);
    session.setTextMessageSizeLimit(EventFrameRegistrationRequest.MAX_REQUEST_TEXT_LENGTH);
    this.sessionManager.registerSession(session);
  }

  @Override
  public void handleMessage(@Nonnull WebSocketSession session, @Nonnull WebSocketMessage<?> message) throws Exception {
    var eventSession = this.sessionManager.findSessionById(session.getId()).orElse(null);
    if (eventSession == null) {
      // open session that sends messages which is not registered on the server...
      // this should usually not happen, but protect against it by closing the connection
      session.close(CloseStatus.SERVER_ERROR);
      return;
    }

    switch (message) {
      // ping-pong handling for keeping the connection alive
      case PingMessage _ -> eventSession.sendPong();
      case PongMessage _ -> eventSession.handlePongReceive();
      case TextMessage tm when tm.getPayload().equals("ping") -> eventSession.sendPongText();
      // handle the text message for the associated session
      case TextMessage tm -> eventSession.handleTextMessage(tm.getPayload());
      // reject all other type of websocket messages being sent by the client
      default -> session.close(CloseStatus.NOT_ACCEPTABLE);
    }
  }

  @Override
  public void afterConnectionClosed(@Nonnull WebSocketSession session, @Nonnull CloseStatus status) {
    this.sessionManager.unregisterSession(session.getId());
  }

  @Override
  public void handleTransportError(@Nonnull WebSocketSession session, @Nonnull Throwable exception) {
    // no-op
  }

  @Override
  public boolean supportsPartialMessages() {
    return false;
  }
}
