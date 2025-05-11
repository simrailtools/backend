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

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Nonnull;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.simrail.backend.collector.metric.PerServerGauge;
import tools.simrail.backend.collector.server.SimRailServerService;

/**
 * Task that marks all journeys that didn't spawn within the expected time frame as canceled.
 */
@Component
class JourneyCancelledMarkingTask {

  private final SimRailServerService serverService;
  private final CollectorJourneyRepository journeyRepository;

  private final PerServerGauge cancelledJourneysCounter;
  private final Meter.MeterProvider<Timer> collectionDurationTimer;

  public JourneyCancelledMarkingTask(
    @Nonnull SimRailServerService serverService,
    @Nonnull CollectorJourneyRepository journeyRepository,
    @Nonnull @Qualifier("journey_cancelled_marked_total") PerServerGauge cancelledJourneysCounter,
    @Nonnull @Qualifier("journey_cancellation_mark_duration") Meter.MeterProvider<Timer> collectionDurationTimer
  ) {
    this.serverService = serverService;
    this.journeyRepository = journeyRepository;
    this.cancelledJourneysCounter = cancelledJourneysCounter;
    this.collectionDurationTimer = collectionDurationTimer;
  }

  @Transactional
  @Scheduled(initialDelay = 2, fixedDelay = 2, timeUnit = TimeUnit.MINUTES, scheduler = "train_cancelled_marker_scheduler")
  public void markJourneysAsCancelled() {
    for (var server : this.serverService.getServers()) {
      var span = Timer.start();
      var currentServerTime = server.currentTime();
      var nonSpawnedTrainIds = this.journeyRepository.findJourneysThatDidNotSpawn(currentServerTime, server.id());
      if (!nonSpawnedTrainIds.isEmpty()) {
        this.journeyRepository.markJourneysAsCancelled(OffsetDateTime.now(), nonSpawnedTrainIds);
        this.journeyRepository.markJourneyEventsAsCancelled(nonSpawnedTrainIds);

        this.cancelledJourneysCounter.setValue(server, nonSpawnedTrainIds.size());
        span.stop(this.collectionDurationTimer.withTag("server_code", server.code()));
      }
    }
  }
}
