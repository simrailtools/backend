/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024-present Pasqual Koschmieder and contributors
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
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.simrail.backend.collector.server.SimRailServerService;
import tools.simrail.backend.collector.util.PerServerGauge;

/**
 * Task that marks all journeys that didn't spawn within the expected time frame as canceled.
 */
@Component
class JourneyCancelledMarkingTask {

  private static final int MARK_BATCH_SIZE = 100;

  private final SimRailServerService serverService;
  private final CollectorJourneyRepository journeyRepository;

  private final JdbcTemplate jdbcTemplate;
  private final TransactionTemplate transactionTemplate;

  private final PerServerGauge cancelledJourneysCounter;
  private final Meter.MeterProvider<Timer> collectionDurationTimer;

  @Autowired
  JourneyCancelledMarkingTask(
    @NonNull SimRailServerService serverService,
    @NonNull CollectorJourneyRepository journeyRepository,
    @NonNull JdbcTemplate jdbcTemplate,
    @NonNull TransactionTemplate transactionTemplate,
    @NonNull @Qualifier("journey_cancelled_marked_total") PerServerGauge cancelledJourneysCounter,
    @Qualifier("journey_cancellation_mark_duration") Meter.@NonNull MeterProvider<Timer> collectionDurationTimer
  ) {
    this.serverService = serverService;
    this.journeyRepository = journeyRepository;
    this.jdbcTemplate = jdbcTemplate;
    this.transactionTemplate = transactionTemplate;
    this.cancelledJourneysCounter = cancelledJourneysCounter;
    this.collectionDurationTimer = collectionDurationTimer;
  }

  @Scheduled(initialDelay = 2, fixedDelay = 2, timeUnit = TimeUnit.MINUTES, scheduler = "train_cancelled_marker_scheduler")
  public void markJourneysAsCancelled() {
    var servers = this.serverService.getServers();
    for (var server : servers) {
      var span = Timer.start();
      try {
        this.transactionTemplate.executeWithoutResult(_ -> {
          var serverTime = server.currentTime();
          var cancelCutoffTime = serverTime.minusMinutes(10);
          var unspawnedJourneyIds = this.journeyRepository.findJourneysThatDidNotSpawn(server.id(), cancelCutoffTime);
          for (int startIdx = 0; startIdx < unspawnedJourneyIds.size(); startIdx += MARK_BATCH_SIZE) {
            var endIdx = Math.min(startIdx + MARK_BATCH_SIZE, unspawnedJourneyIds.size());
            var batchJourneyIds = unspawnedJourneyIds.subList(startIdx, endIdx);
            var batchJourneyIdArray = batchJourneyIds.toArray(UUID[]::new);

            // mark the journey as canceled in the journeys table
            // language=sql
            var journeyUpdateSql = """
              UPDATE sit_journey j
              SET cancelled = TRUE, update_time = CURRENT_TIMESTAMP
              WHERE j.id = ANY(?)
              """;
            this.jdbcTemplate.update(journeyUpdateSql, (Object) batchJourneyIdArray);

            // mark the journey events of the journey as canceled
            // language=sql
            var journeyEventUpdateSql = """
              UPDATE sit_journey_event je
              SET cancelled = TRUE
              WHERE je.journey_id = ANY(?)
              """;
            this.jdbcTemplate.update(journeyEventUpdateSql, (Object) batchJourneyIdArray);
          }

          this.cancelledJourneysCounter.setValue(server, unspawnedJourneyIds.size());
        });
      } finally {
        var timer = this.collectionDurationTimer.withTag("server_code", server.code());
        span.stop(timer);
      }
    }
  }
}
