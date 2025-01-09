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

package tools.simrail.backend.collector.journey;

import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.simrail.backend.collector.rpc.InternalRpcEventBusService;

/**
 * Publishes the updated information about a single journey to all listeners.
 */
@Component
final class JourneyUpdateHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(JourneyUpdateHandler.class);

  private final Lock updateSendLock;
  private final InternalRpcEventBusService internalRpcEventBusService;

  @Autowired
  public JourneyUpdateHandler(@Nonnull InternalRpcEventBusService internalRpcEventBusService) {
    this.updateSendLock = new ReentrantLock();
    this.internalRpcEventBusService = internalRpcEventBusService;
  }

  /**
   * Publishes update frames for the given updated journeys.
   *
   * @param updatedJourneys the journeys that were updated.
   */
  public void publishJourneyUpdates(@Nonnull List<JourneyDirtyStateRecorder> updatedJourneys) {
    this.updateSendLock.lock();
    try {
      for (var updatedJourney : updatedJourneys) {
        var updateFrame = updatedJourney.buildUpdateFrame();
        this.internalRpcEventBusService.publishJourneyUpdate(updateFrame);
      }
    } catch (Exception exception) {
      LOGGER.error("Exception sending journey updates to listeners", exception);
    } finally {
      this.updateSendLock.unlock();
    }
  }
}
