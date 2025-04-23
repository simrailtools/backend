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

package tools.simrail.backend.api.event.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

/**
 * Manager for the established websocket connections by clients to the api.
 */
@Component
public class EventSessionManager {

  private static final int SEND_TIME_LIMIT = (int) TimeUnit.SECONDS.toMillis(30);
  private static final int SEND_BUFFER_SIZE_LIMIT = (int) DataSize.ofMegabytes(5).toBytes();

  private final ObjectMapper objectMapper;
  private final EventSessionInitialDataSender initialDataSender;
  private final Map<String, EventWebsocketSession> sessions = new ConcurrentHashMap<>();

  @Autowired
  EventSessionManager(
    @Nonnull ObjectMapper objectMapper,
    @Nonnull EventSessionInitialDataSender initialDataSender
  ) {
    this.objectMapper = objectMapper;
    this.initialDataSender = initialDataSender;
  }

  /**
   * Registers the given websocket session in this session manager.
   *
   * @param session the session to register.
   */
  public void registerSession(@Nonnull WebSocketSession session) {
    var wrappedSession = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT, SEND_BUFFER_SIZE_LIMIT);
    var eventWsSession = new EventWebsocketSession(this.objectMapper, wrappedSession, this.initialDataSender);
    this.sessions.put(session.getId(), eventWsSession);
  }

  /**
   * Unregisters the session with the given id.
   *
   * @param sessionId the id of the session to unregister.
   */
  public void unregisterSession(@Nonnull String sessionId) {
    this.sessions.remove(sessionId);
  }

  /**
   * Get a single registered session by its id, if one is registered.
   *
   * @param id the id of the session to get.
   * @return the session with the given id.
   */
  public @Nonnull Optional<EventWebsocketSession> findSessionById(@Nonnull String id) {
    return Optional.ofNullable(this.sessions.get(id));
  }

  /**
   * Sends an update frame to all clients that subscribed for updates of the associated data.
   *
   * @param snapshot    the locally cached snapshot that was updated.
   * @param updateFrame the update frame received from the collector.
   */
  public void publishUpdateFrame(@Nonnull Object snapshot, @Nonnull Object updateFrame) {
    this.sessions.forEach((_, session) -> session.publishUpdateFrame(snapshot, updateFrame));
  }

  /**
   * Sends a ping to all clients every 15 seconds and terminates all connections that did not send a pong in the last 30
   * seconds.
   */
  @Scheduled(fixedDelay = 15, timeUnit = TimeUnit.SECONDS)
  void sendPingAndTerminateStaleConnections() throws Exception {
    // send out a ping message to all connections that are still active,
    // close all sessions which didn't send a pong message in the last 30 seconds
    var now = Instant.now();
    var iterator = this.sessions.values().iterator();
    while (iterator.hasNext()) {
      var session = iterator.next();
      if (session.isObsolete(now)) {
        session.close();
        iterator.remove();
      } else {
        session.sendPing();
      }
    }
  }
}
