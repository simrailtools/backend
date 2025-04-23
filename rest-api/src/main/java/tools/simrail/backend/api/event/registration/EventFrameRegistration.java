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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import tools.simrail.backend.api.event.session.EventWebsocketSession;

/**
 * Registration holder for a specific type of event frame being sent.
 */
public abstract sealed class EventFrameRegistration<S, F> permits
  JourneyPositionEventFrameRegistration, JourneyDataEventFrameRegistration,
  ServerEventFrameRegistration, DispatchPostFrameRegistration {

  protected final EventWebsocketSession session;
  protected final Map<String, Set<String>> registrationsByServer;

  protected EventFrameRegistration(@Nonnull EventWebsocketSession session) {
    this.session = session;
    this.registrationsByServer = new ConcurrentHashMap<>(6, 0.75f, 1);
  }

  /**
   * Subscribes to the event frames requested in the given request.
   *
   * @param request the request holding the information to subscribe to.
   * @return true if the registration was done, false if the client is already subscribed.
   */
  public boolean subscribe(@Nonnull EventFrameRegistrationRequest request) {
    var registrations = this.registrationsByServer.computeIfAbsent(
      request.serverId().toString(),
      _ -> ConcurrentHashMap.newKeySet());

    var dataId = request.dataId();
    var dataIdString = dataId == null ? "+" : dataId.toString();
    return registrations.add(dataIdString);
  }

  /**
   * Removes the subscription of the client based on the given request.
   *
   * @param request the request containing the information what to unsubscribe.
   */
  public void unsubscribe(@Nonnull EventFrameRegistrationRequest request) {
    var registrations = this.registrationsByServer.get(request.serverId().toString());
    if (registrations != null) {
      var dataId = request.dataId();
      var dataIdString = dataId == null ? "+" : dataId.toString();
      registrations.remove(dataIdString);
    }
  }

  /**
   * Checks if a registration for the given update frame exists.
   *
   * @param frame the frame that was sent to indicate the update.
   * @return true if a registration for the frame data exists, false otherwise.
   */
  public abstract boolean hasRegistration(@Nonnull F frame);

  /**
   * Checks if a registration for the given server and data id exists.
   *
   * @param serverId the server id to check.
   * @param dataId   the data id to check.
   * @return true if a registration for the server and data id exists, false otherwise.
   */
  public boolean hasRegistration(@Nonnull String serverId, @Nonnull String dataId) {
    var registrations = this.registrationsByServer.get(serverId);
    return registrations != null && (registrations.contains("+") || registrations.contains(dataId));
  }

  /**
   * Sends a constructed update frame to the associated websocket session.
   *
   * @param snapshot the snapshot of the data that was updated.
   * @param frame    the update frame that was sent for the updated data.
   */
  public abstract void publishUpdateFrame(@Nonnull S snapshot, @Nonnull F frame) throws Exception;
}
