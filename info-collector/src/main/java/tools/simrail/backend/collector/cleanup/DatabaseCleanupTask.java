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

package tools.simrail.backend.collector.cleanup;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Nonnull;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Task that cleans up old data from the database to free up disk space.
 */
@Component
class DatabaseCleanupTask {

  private final CleanupJourneyRepository journeyRepository;
  private final CleanupJourneyEventRepository journeyEventRepository;
  private final CleanupJourneyVehicleRepository journeyVehicleRepository;

  private final Timer cleanupDurationTimer;
  private final Counter cleanupDeletionsTotalCounter;

  @Autowired
  public DatabaseCleanupTask(
    @Nonnull CleanupJourneyRepository journeyRepository,
    @Nonnull CleanupJourneyEventRepository journeyEventRepository,
    @Nonnull CleanupJourneyVehicleRepository journeyVehicleRepository,
    @Nonnull @Qualifier("db_cleanup_duration_seconds") Timer cleanupDurationTimer,
    @Nonnull @Qualifier("db_cleanup_deletions_total") Counter cleanupDeletionsTotalCounter
  ) {
    this.journeyRepository = journeyRepository;
    this.journeyEventRepository = journeyEventRepository;
    this.journeyVehicleRepository = journeyVehicleRepository;

    this.cleanupDurationTimer = cleanupDurationTimer;
    this.cleanupDeletionsTotalCounter = cleanupDeletionsTotalCounter;
  }

  @Transactional
  @Scheduled(scheduler = "database_cleanup_scheduler", cron = "0 0 5 * * *")
  public void cleanupDatabase() {
    this.cleanupDurationTimer.record(() -> {
      // find the journeys without a data update in the last three months, remove all associated events & vehicles as well
      var cleanupStartDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(90).truncatedTo(ChronoUnit.DAYS);
      var journeyIdsToRemove = this.journeyRepository.findJourneyIdsByCleanupStartDate(cleanupStartDate);
      if (!journeyIdsToRemove.isEmpty()) {
        this.journeyEventRepository.deleteAllByJourneyIdIn(journeyIdsToRemove);
        this.journeyVehicleRepository.deleteAllByJourneyIdIn(journeyIdsToRemove);
        this.journeyRepository.deleteAllById(journeyIdsToRemove);
      }

      this.cleanupDeletionsTotalCounter.increment(journeyIdsToRemove.size());
    });
  }
}
