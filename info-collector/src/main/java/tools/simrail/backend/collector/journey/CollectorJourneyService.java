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

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.simrail.backend.common.cache.DataCache;
import tools.simrail.backend.common.journey.JourneyEntity;
import tools.simrail.backend.common.journey.JourneyEventRepository;
import tools.simrail.backend.common.proto.CacheProto;
import tools.simrail.backend.common.util.ObjectChecksumGenerator;

/**
 * Service for accessing and updating journeys.
 */
@Service
class CollectorJourneyService {

  private static final int JOURNEY_INSERT_BATCH_SIZE = 100;

  private final JdbcTemplate jdbcTemplate;
  private final CollectorJourneyRepository journeyRepository;
  private final JourneyEventRepository journeyEventRepository;
  private final DataCache<CacheProto.JourneyChecksumData> journeyChecksumCache;

  @Autowired
  public CollectorJourneyService(
    @NonNull JdbcTemplate jdbcTemplate,
    @NonNull CollectorJourneyRepository journeyRepository,
    @NonNull JourneyEventRepository journeyEventRepository,
    @NonNull @Qualifier("journey_checksum_cache") DataCache<CacheProto.JourneyChecksumData> journeyChecksumCache
  ) {
    this.jdbcTemplate = jdbcTemplate;
    this.journeyRepository = journeyRepository;
    this.journeyEventRepository = journeyEventRepository;
    this.journeyChecksumCache = journeyChecksumCache;
  }

  @Transactional
  @SuppressWarnings("DataFlowIssue") // just for IJ to shut up
  public void persistScheduledUpdatedJourneys(@NonNull Collection<JourneyEntity> journeys) {
    var relevantJourneys = journeys.stream()
      .map(journey -> {
        // find the journeys that actually changed since the last collection
        var journeyChecksum = ObjectChecksumGenerator.generateChecksum(journey);
        var cachedChecksumData = this.journeyChecksumCache.findByPrimaryKey(journey.getForeignRunId().toString());
        if (cachedChecksumData == null || !cachedChecksumData.getChecksum().equals(journeyChecksum)) {
          return Map.entry(journey, journeyChecksum);
        }

        return null;
      })
      .filter(Objects::nonNull)
      .toList();
    if (relevantJourneys.isEmpty()) {
      return;
    }

    for (var startIdx = 0; startIdx < relevantJourneys.size(); startIdx += JOURNEY_INSERT_BATCH_SIZE) {
      var endIdx = Math.min(startIdx + JOURNEY_INSERT_BATCH_SIZE, relevantJourneys.size());
      var journeyBatch = relevantJourneys.subList(startIdx, endIdx);

      // delete all the journeys from the database which did not spawn yet
      var batchRunIds = journeyBatch.stream().map(entry -> entry.getKey().getForeignRunId()).toList();
      this.journeyRepository.deleteUnstartedJourneysByRunIds(batchRunIds);

      // re-store all journeys into the db that either do not exist or did not spawn yet
      var updatableRunIds = this.journeyRepository.findMissingRunIds(batchRunIds.toArray(UUID[]::new));
      for (var journeyEntry : journeyBatch) {
        var journey = journeyEntry.getKey();
        var journeyChecksum = journeyEntry.getValue();
        if (updatableRunIds.contains(journey.getForeignRunId())) {
          this.journeyRepository.save(journey);
          this.journeyEventRepository.saveAll(journey.getEvents());

          var checksumData = CacheProto.JourneyChecksumData.newBuilder()
            .setChecksum(journeyChecksum)
            .setForeignRunId(journey.getForeignRunId().toString())
            .build();
          this.journeyChecksumCache.setCachedValue(checksumData);
        }
      }
    }
  }

  // impl note: called within train collect, should execute as fast as possible
  @Transactional
  public void markJourneyAsFirstSeen(@NonNull UUID journeyId) {
    // language=sql
    var updateSql = """
      UPDATE sit_journey j
      SET update_time = CURRENT_TIMESTAMP, first_seen_time = CURRENT_TIMESTAMP, last_seen_time = NULL, cancelled = FALSE
      WHERE j.id = ?
      """;
    this.jdbcTemplate.update(updateSql, journeyId);
  }

  // impl note: called within train collect, should execute as fast as possible
  @Transactional
  public void markJourneyAsLastSeen(@NonNull Collection<UUID> journeyIds) {
    // language=sql
    var updateSql = """
      UPDATE sit_journey j
      SET update_time = CURRENT_TIMESTAMP, last_seen_time = CURRENT_TIMESTAMP
      WHERE j.id = ANY(?)
      """;
    this.jdbcTemplate.update(updateSql, (Object) journeyIds.toArray(UUID[]::new));
  }
}
