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
import tools.simrail.backend.api.event.dto.EventFrameDto;
import tools.simrail.backend.api.event.dto.EventFrameType;
import tools.simrail.backend.api.event.dto.EventFrameUpdateType;
import tools.simrail.backend.api.event.dto.EventJourneyDataUpdateDto;
import tools.simrail.backend.api.event.session.EventWebsocketSession;
import tools.simrail.backend.api.eventbus.dto.EventbusJourneySnapshotDto;
import tools.simrail.backend.common.rpc.JourneyUpdateFrame;

/**
 * Event frame registration for updates of journey data.
 */
public final class JourneyDataEventFrameRegistration
  extends EventFrameRegistration<EventbusJourneySnapshotDto, JourneyUpdateFrame> {

  public JourneyDataEventFrameRegistration(@Nonnull EventWebsocketSession session) {
    super(session);
  }

  @Override
  public boolean hasRegistration(@Nonnull JourneyUpdateFrame frame) {
    return frame.getEventUpdated() && this.hasRegistration(frame.getServerId(), frame.getJourneyId());
  }

  @Override
  public void publishUpdateFrame(
    @Nonnull EventbusJourneySnapshotDto snapshot,
    @Nonnull JourneyUpdateFrame frame
  ) throws Exception {
    var frameData = EventJourneyDataUpdateDto.fromJourneyUpdateFrame(frame);
    var updateFrame = new EventFrameDto<>(EventFrameType.JOURNEY_DETAILS, EventFrameUpdateType.UPDATE, frameData);
    this.session.sendJson(updateFrame);
  }
}
