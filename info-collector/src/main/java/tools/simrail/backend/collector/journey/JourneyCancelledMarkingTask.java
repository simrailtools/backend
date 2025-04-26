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
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.simrail.backend.collector.server.SimRailServerService;

/**
 * Task that marks all journeys that didn't spawn within the expected time frame as canceled.
 */
@Component
class JourneyCancelledMarkingTask {

  private static final Logger LOGGER = LoggerFactory.getLogger(JourneyCancelledMarkingTask.class);

  private final SimRailServerService serverService;
  private final CollectorJourneyRepository journeyRepository;

  public JourneyCancelledMarkingTask(
    @Nonnull SimRailServerService serverService,
    @Nonnull CollectorJourneyRepository journeyRepository
  ) {
    this.serverService = serverService;
    this.journeyRepository = journeyRepository;
  }

  @Transactional
  @Scheduled(initialDelay = 2, fixedDelay = 2, timeUnit = TimeUnit.MINUTES, scheduler = "train_cancelled_marker_scheduler")
  public void markJourneysAsCancelled() {
    for (var server : this.serverService.getServers()) {
      var startTime = Instant.now();
      var currentServerTime = server.currentTime();
      var nonSpawnedTrainIds = this.journeyRepository.findJourneysThatDidNotSpawn(currentServerTime, server.id());
      if (!nonSpawnedTrainIds.isEmpty()) {
        this.journeyRepository.markJourneysAsCancelled(nonSpawnedTrainIds);
        this.journeyRepository.markJourneyEventsAsCancelled(nonSpawnedTrainIds);

        var firstMarkedJourney = nonSpawnedTrainIds.getFirst();
        var elapsedTime = Duration.between(startTime, Instant.now()).toSeconds();
        LOGGER.info(
          "Marked {} journeys (1.: {}) on server {} as cancelled in {} seconds",
          nonSpawnedTrainIds.size(), firstMarkedJourney, server.code(), elapsedTime);
      }
    }
  }
}
