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

package tools.simrail.backend.api.eventbus.cache;

import jakarta.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.simrail.backend.api.eventbus.SitEventbusListener;
import tools.simrail.backend.api.eventbus.dto.EventbusJourneySnapshotDto;
import tools.simrail.backend.common.rpc.JourneyUpdateFrame;
import tools.simrail.backend.common.rpc.UpdateType;

/**
 * Cleanup task for the sit snapshot cache, removing unnecessary entries. For now, it's only removing journeys that are
 * no longer active (due to high changing frequencies there might be bad data after restarts).
 */
@Component
class CacheMaintenanceTask {

  private static final Logger LOGGER = LoggerFactory.getLogger(CacheMaintenanceTask.class);

  private final SitSnapshotCache snapshotCache;
  private final List<SitEventbusListener> eventBusListeners;

  private final EventbusJourneyRepository journeyRepository;

  @Autowired
  public CacheMaintenanceTask(
    @Nonnull SitSnapshotCache snapshotCache,
    @Nonnull List<SitEventbusListener> eventBusListeners,
    @Nonnull EventbusJourneyRepository journeyRepository
  ) {
    this.snapshotCache = snapshotCache;
    this.eventBusListeners = eventBusListeners;
    this.journeyRepository = journeyRepository;
  }

  @Scheduled(initialDelay = 30, fixedDelay = 150, timeUnit = TimeUnit.SECONDS)
  public void cleanupSnapshotCache() {
    var activeJourneyIds = this.journeyRepository.findSnapshotsOfAllActiveJourneys()
      .stream()
      .map(EventbusJourneySnapshotDto::getJourneyId)
      .collect(Collectors.toUnmodifiableSet());
    var removedJourneys = this.snapshotCache.cleanupJourneySnapshots(activeJourneyIds);
    for (var journey : removedJourneys) {
      var frame = JourneyUpdateFrame.newBuilder()
        .setUpdateType(UpdateType.REMOVE)
        .setServerId(journey.getServerId().toString())
        .setJourneyId(journey.getJourneyId().toString())
        .buildPartial();
      this.eventBusListeners.forEach(listener -> listener.handleJourneyUpdate(frame, journey));
    }

    if (!removedJourneys.isEmpty()) {
      LOGGER.info("Cleaned {} journeys from local snapshot cache", removedJourneys.size());
    }
  }
}
