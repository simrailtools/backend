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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import tools.simrail.backend.api.event.dto.EventDispatchPostUpdateDto;
import tools.simrail.backend.api.event.dto.EventFrameDto;
import tools.simrail.backend.api.event.dto.EventFrameType;
import tools.simrail.backend.api.event.dto.EventFrameUpdateType;
import tools.simrail.backend.api.event.dto.EventJourneyUpdateDto;
import tools.simrail.backend.api.event.dto.EventServerUpdateDto;
import tools.simrail.backend.api.eventbus.cache.SitSnapshotCache;
import tools.simrail.backend.api.eventbus.dto.EventbusDispatchPostSnapshotDto;
import tools.simrail.backend.api.eventbus.dto.EventbusJourneySnapshotDto;
import tools.simrail.backend.api.eventbus.dto.EventbusServerSnapshotDto;
import tools.simrail.backend.common.rpc.DispatchPostUpdateFrame;
import tools.simrail.backend.common.rpc.JourneyUpdateFrame;
import tools.simrail.backend.common.rpc.ServerUpdateFrame;
import tools.simrail.backend.common.rpc.UpdateType;

/**
 * Manager for the established websocket connections by clients to the api.
 */
@Component
public class EventSessionManager {

  private static final int SEND_TIME_LIMIT = (int) TimeUnit.SECONDS.toMillis(30);
  private static final int SEND_BUFFER_SIZE_LIMIT = (int) DataSize.ofMegabytes(5).toBytes();

  private final ObjectMapper objectMapper;
  private final SitSnapshotCache snapshotCache;
  private final Map<String, EventWebsocketSession> sessions = new ConcurrentHashMap<>();

  @Autowired
  public EventSessionManager(@Nonnull ObjectMapper objectMapper, @Nonnull SitSnapshotCache snapshotCache) {
    this.objectMapper = objectMapper;
    this.snapshotCache = snapshotCache;
  }

  /**
   * Registers the given websocket session in this session manager.
   *
   * @param session             the session to register.
   * @param requestedFrameTypes the update frame types that were requested by the client.
   */
  public void registerSession(
    @Nonnull WebSocketSession session,
    @Nonnull Map<EventFrameType, List<String>> requestedFrameTypes
  ) {
    var wrappedSession = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT, SEND_BUFFER_SIZE_LIMIT);
    var eventWsSession = new EventWebsocketSession(wrappedSession, requestedFrameTypes);
    this.sendInitialSnapshots(eventWsSession);
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
   * Sends an update frame to all clients that subscribed for updates of the given server.
   *
   * @param frame    the update frame received from the collector.
   * @param snapshot the locally cached server snapshot that was updated.
   */
  public void handleServerUpdate(@Nonnull ServerUpdateFrame frame, @Nonnull EventbusServerSnapshotDto snapshot) {
    // sends out the server update frame to all subscribed clients, the serialized frame is
    // eagerly initialized when one session is detected that wants that handle the frame
    String serializedFrame = null;
    for (var session : this.sessions.values()) {
      if (session.requestedFrame(EventFrameType.SERVER, frame.getServerId())) {
        try {
          serializedFrame = Objects.requireNonNullElseGet(
            serializedFrame,
            () -> this.serializeServerFrame(frame, snapshot));
          session.sendText(serializedFrame);
        } catch (IOException _) {
        }
      }
    }
  }

  /**
   * Serializes the data of the given server update frame as a string to be sent to a client.
   *
   * @param frame    the received update frame.
   * @param snapshot the locally cached server snapshot.
   * @return the serialized frame data to send to the client.
   */
  private @Nonnull String serializeServerFrame(
    @Nonnull ServerUpdateFrame frame,
    @Nonnull EventbusServerSnapshotDto snapshot
  ) {
    try {
      var updateType = EventFrameUpdateType.fromInternalType(frame.getUpdateType());
      var frameData = frame.getUpdateType() == UpdateType.ADD
        ? snapshot
        : EventServerUpdateDto.fromServerUpdateFrame(frame);
      var updateFrame = new EventFrameDto<>(EventFrameType.SERVER, updateType, frameData);
      return this.objectMapper.writeValueAsString(updateFrame);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize update frame", exception);
    }
  }

  /**
   * Sends an update frame to all clients that subscribed for updates of the given journey.
   *
   * @param frame    the update frame received from the collector.
   * @param snapshot the locally cached journey snapshot that was updated.
   */
  public void handleJourneyUpdate(@Nonnull JourneyUpdateFrame frame, @Nonnull EventbusJourneySnapshotDto snapshot) {
    // sends out the journey update frame to all subscribed clients, the serialized frame is
    // eagerly initialized when one session is detected that wants that handle the frame
    String serializedFrame = null;
    for (var session : this.sessions.values()) {
      if (session.requestedFrame(EventFrameType.JOURNEY, frame.getServerId())) {
        try {
          serializedFrame = Objects.requireNonNullElseGet(
            serializedFrame,
            () -> this.serializeJourneyFrame(frame, snapshot));
          session.sendText(serializedFrame);
        } catch (IOException _) {
        }
      }
    }
  }

  /**
   * Serializes the data of the given journey update frame as a string to be sent to a client.
   *
   * @param frame    the received update frame.
   * @param snapshot the locally cached journey snapshot.
   * @return the serialized frame data to send to the client.
   */
  private @Nonnull String serializeJourneyFrame(
    @Nonnull JourneyUpdateFrame frame,
    @Nonnull EventbusJourneySnapshotDto snapshot
  ) {
    try {
      var updateType = EventFrameUpdateType.fromInternalType(frame.getUpdateType());
      var frameData = frame.getUpdateType() == UpdateType.ADD
        ? snapshot
        : EventJourneyUpdateDto.fromJourneyUpdateFrame(frame);
      var updateFrame = new EventFrameDto<>(EventFrameType.JOURNEY, updateType, frameData);
      return this.objectMapper.writeValueAsString(updateFrame);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize update frame", exception);
    }
  }

  /**
   * Sends an update frame to all clients that subscribed for updates of the given dispatch post.
   *
   * @param frame    the update frame received from the collector.
   * @param snapshot the locally cached dispatch post snapshot that was updated.
   */
  public void handleDispatchPostUpdate(
    @Nonnull DispatchPostUpdateFrame frame,
    @Nonnull EventbusDispatchPostSnapshotDto snapshot
  ) {
    // sends out the dispatch post update frame to all subscribed clients, the serialized frame is
    // eagerly initialized when one session is detected that wants that handle the frame
    String serializedFrame = null;
    for (var session : this.sessions.values()) {
      if (session.requestedFrame(EventFrameType.DISPATCH_POST, frame.getServerId())) {
        try {
          serializedFrame = Objects.requireNonNullElseGet(
            serializedFrame,
            () -> this.serializeDispatchPostFrame(frame, snapshot));
          session.sendText(serializedFrame);
        } catch (IOException _) {
        }
      }
    }
  }

  /**
   * Serializes the data of the given dispatch post update frame as a string to be sent to a client.
   *
   * @param frame    the received update frame.
   * @param snapshot the locally cached dispatch post snapshot.
   * @return the serialized frame data to send to the client.
   */
  private @Nonnull String serializeDispatchPostFrame(
    @Nonnull DispatchPostUpdateFrame frame,
    @Nonnull EventbusDispatchPostSnapshotDto snapshot
  ) {
    try {
      var updateType = EventFrameUpdateType.fromInternalType(frame.getUpdateType());
      var frameData = frame.getUpdateType() == UpdateType.ADD
        ? snapshot
        : EventDispatchPostUpdateDto.fromDispatchPostUpdateFrame(frame);
      var updateFrame = new EventFrameDto<>(EventFrameType.DISPATCH_POST, updateType, frameData);
      return this.objectMapper.writeValueAsString(updateFrame);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize update frame", exception);
    }
  }

  /**
   * Sends out all the cached snapshots to the client if a subscription request was sent to populate the client cache
   * before receiving update frames.
   *
   * @param session the session to which the initial snapshots should be sent.
   */
  public void sendInitialSnapshots(@Nonnull EventWebsocketSession session) {
    try {
      // send out server snapshots
      for (var serverSnapshot : this.snapshotCache.getCachedServerSnapshots()) {
        if (session.requestedFrame(EventFrameType.SERVER, serverSnapshot.getServerId().toString())) {
          var fakeUpdateFrame = ServerUpdateFrame.newBuilder().setUpdateType(UpdateType.ADD).buildPartial();
          var serializedFrame = this.serializeServerFrame(fakeUpdateFrame, serverSnapshot);
          session.sendText(serializedFrame);
        }
      }

      // send out journey snapshots
      for (var journeySnapshot : this.snapshotCache.getCachedJourneySnapshots()) {
        if (session.requestedFrame(EventFrameType.JOURNEY, journeySnapshot.getServerId().toString())) {
          var fakeUpdateFrame = JourneyUpdateFrame.newBuilder().setUpdateType(UpdateType.ADD).buildPartial();
          var serializedFrame = this.serializeJourneyFrame(fakeUpdateFrame, journeySnapshot);
          session.sendText(serializedFrame);
        }
      }

      // send out dispatch post snapshots
      for (var dispatchPostSnapshot : this.snapshotCache.getCachedDispatchPostSnapshots()) {
        if (session.requestedFrame(EventFrameType.DISPATCH_POST, dispatchPostSnapshot.getServerId().toString())) {
          var fakeUpdateFrame = DispatchPostUpdateFrame.newBuilder().setUpdateType(UpdateType.ADD).buildPartial();
          var serializedFrame = this.serializeDispatchPostFrame(fakeUpdateFrame, dispatchPostSnapshot);
          session.sendText(serializedFrame);
        }
      }
    } catch (IOException _) {
    }
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
