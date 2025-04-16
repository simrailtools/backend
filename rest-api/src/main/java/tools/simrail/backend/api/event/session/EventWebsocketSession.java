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
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.simrail.backend.api.event.dto.EventFrameType;
import tools.simrail.backend.api.event.registration.DispatchPostFrameRegistration;
import tools.simrail.backend.api.event.registration.EventFrameRegistration;
import tools.simrail.backend.api.event.registration.EventFrameRegistrationRequest;
import tools.simrail.backend.api.event.registration.JourneyDataEventFrameRegistration;
import tools.simrail.backend.api.event.registration.JourneyPositionEventFrameRegistration;
import tools.simrail.backend.api.event.registration.ServerEventFrameRegistration;
import tools.simrail.backend.common.rpc.DispatchPostUpdateFrame;
import tools.simrail.backend.common.rpc.JourneyUpdateFrame;
import tools.simrail.backend.common.rpc.ServerUpdateFrame;

/**
 * A single established websocket connection to the server.
 */
public final class EventWebsocketSession {

  private static final int MAX_SECONDS_UNTIL_OBSOLETE = 30;
  private static final Logger LOGGER = LoggerFactory.getLogger(EventWebsocketSession.class);

  private final ObjectMapper objectMapper;
  private final WebSocketSession webSocketSession;
  private final EventSessionInitialDataSender initialDataSender;
  private final Map<EventFrameType, EventFrameRegistration<?, ?>> requestedFrameTypes;

  // the last time a pong was received from the client
  private Instant lastPongReceived;

  EventWebsocketSession(
    @Nonnull ObjectMapper objectMapper,
    @Nonnull WebSocketSession webSocketSession,
    @Nonnull EventSessionInitialDataSender initialDataSender
  ) {
    this.objectMapper = objectMapper;
    this.webSocketSession = webSocketSession;
    this.initialDataSender = initialDataSender;

    this.lastPongReceived = Instant.now();
    this.requestedFrameTypes = new ConcurrentHashMap<>(6, 0.75f, 1);
  }

  /**
   * Publishes an update frame to the underlying websocket connection if the client did subscribe to updates.
   *
   * @param snapshot    the locally cached snapshot of the data that was updated.
   * @param updateFrame the update frame for the data that was received from the collector.
   */
  public void publishUpdateFrame(@Nonnull Object snapshot, @Nonnull Object updateFrame) {
    switch (updateFrame) {
      case ServerUpdateFrame suf -> this.publishUpdateFrame(EventFrameType.SERVER, snapshot, suf);
      case DispatchPostUpdateFrame dpf -> this.publishUpdateFrame(EventFrameType.DISPATCH_POST, snapshot, dpf);
      case JourneyUpdateFrame juf -> {
        this.publishUpdateFrame(EventFrameType.JOURNEY_POSITION, snapshot, updateFrame);
        if (juf.getEventUpdated()) {
          // special extra case: also publish if an event along the journey route was updated
          this.publishUpdateFrame(EventFrameType.JOURNEY_DETAILS, snapshot, updateFrame);
        }
      }
      default -> {
        // ignore, unknown update frame
      }
    }
  }

  /**
   * Publishes an update frame to the underlying websocket connection if the client did subscribe to updates.
   *
   * @param frameType the type of frame that was received from the collector.
   * @param snapshot  the locally cached snapshot of the data that was updated.
   * @param frame     the update frame for the data that was received from the collector.
   * @param <S>       the type of the locally cached snapshots.
   * @param <F>       the type of the update frames sent by the collector.
   */
  @SuppressWarnings("unchecked")
  private <S, F> void publishUpdateFrame(@Nonnull EventFrameType frameType, @Nonnull S snapshot, @Nonnull F frame) {
    try {
      var registration = (EventFrameRegistration<S, F>) this.requestedFrameTypes.get(frameType);
      if (registration != null && registration.hasRegistration(frame)) {
        registration.publishUpdateFrame(snapshot, frame);
      }
    } catch (Exception exception) {
      LOGGER.warn("Failed to send update frame to client", exception);
    }
  }

  /**
   * Handles an incoming text messages from the websocket connection.
   *
   * @param text the text being sent by the client.
   */
  public void handleTextMessage(@Nonnull String text) throws Exception {
    // parse a frame request from the given text message, close the connection if an invalid request was sent
    var request = EventFrameRegistrationRequest.parseFromText(text);
    if (request == null) {
      this.webSocketSession.close(CloseStatus.NOT_ACCEPTABLE);
      return;
    }

    switch (request.action()) {
      case SUBSCRIBE -> {
        var registration = this.requestedFrameTypes.computeIfAbsent(request.frameType(), type -> switch (type) {
          case SERVER -> new ServerEventFrameRegistration(this);
          case DISPATCH_POST -> new DispatchPostFrameRegistration(this);
          case JOURNEY_DETAILS -> new JourneyDataEventFrameRegistration(this);
          case JOURNEY_POSITION -> new JourneyPositionEventFrameRegistration(this);
        });
        var didRegister = registration.subscribe(request);
        if (didRegister) {
          this.initialDataSender.sendInitialDataFrames(this, request);
        }
      }
      case UNSUBSCRIBE -> {
        var registration = this.requestedFrameTypes.get(request.frameType());
        if (registration != null) {
          registration.unsubscribe(request);
        }
      }
    }
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
   * Serializes the given data into a json string and sends it as a text frame to the connection.
   *
   * @param data the data to serialize as json and send to the websocket connection.
   */
  public void sendJson(@Nonnull Object data) throws IOException {
    var encodedJson = this.objectMapper.writeValueAsString(data);
    this.sendText(encodedJson);
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
