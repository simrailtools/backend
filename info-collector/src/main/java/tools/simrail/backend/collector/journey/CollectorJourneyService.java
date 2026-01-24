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

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.simrail.backend.common.cache.DataCache;
import tools.simrail.backend.common.journey.JourneyEntity;
import tools.simrail.backend.common.journey.JourneyEventRepository;
import tools.simrail.backend.common.proto.CacheProto;
import tools.simrail.backend.common.proto.EventBusProto;
import tools.simrail.backend.common.util.ObjectChecksumGenerator;

/**
 * Service for accessing and updating journeys.
 */
@Service
class CollectorJourneyService {

  private final CollectorJourneyRepository journeyRepository;
  private final JourneyEventRepository journeyEventRepository;
  private final DataCache<CacheProto.JourneyChecksumData> journeyChecksumCache;
  private final DataCache<EventBusProto.JourneyUpdateFrame> journeyRealtimeDataCache;

  @Autowired
  public CollectorJourneyService(
    @NonNull CollectorJourneyRepository journeyRepository,
    @NonNull JourneyEventRepository journeyEventRepository,
    @NonNull @Qualifier("journey_checksum_cache") DataCache<CacheProto.JourneyChecksumData> journeyChecksumCache,
    @NonNull @Qualifier("journey_realtime_cache") DataCache<EventBusProto.JourneyUpdateFrame> journeyRealtimeDataCache
  ) {
    this.journeyRepository = journeyRepository;
    this.journeyEventRepository = journeyEventRepository;
    this.journeyChecksumCache = journeyChecksumCache;
    this.journeyRealtimeDataCache = journeyRealtimeDataCache;
  }

  @Transactional
  @SuppressWarnings("DataFlowIssue") // just for IJ to shut up
  public void persistScheduledUpdatedJourneys(@NonNull Collection<JourneyEntity> journeys) {
    var relevantJourneys = journeys.stream()
      .filter(journey -> {
        // check if the journey is not already active on a server, in this case
        // we should not update the journey to prevent losing realtime event data.
        // note that this check is only a quick check for active journeys, we later
        // do an additional check for past journeys, they shouldn't be updated either...
        var realtimeData = this.journeyRealtimeDataCache.findByPrimaryKey(journey.getId().toString());
        return realtimeData == null;
      })
      .map(journey -> {
        // first find the journeys that actually changed since the last collection
        var journeyChecksum = ObjectChecksumGenerator.generateChecksum(journey);
        var cachedChecksumData = this.journeyChecksumCache.findByPrimaryKey(journey.getForeignRunId().toString());
        return cachedChecksumData != null && cachedChecksumData.getChecksum().equals(journeyChecksum)
          ? null
          : Map.entry(journey, journeyChecksum);
      })
      .filter(Objects::nonNull)
      .toList();

    // delete all the journeys from the database which did not spawn yet.
    // the db returns the run ids of the journeys that were actually removed
    var relevantRunIds = relevantJourneys.stream()
      .map(entry -> entry.getKey().getForeignRunId())
      .toList();
    if (!relevantRunIds.isEmpty()) {
      this.journeyRepository.deleteUnstartedJourneysByRunIds(relevantRunIds);
    }

    var remainingRunIds = this.journeyRepository.findAllRunIdsWhereRunIdIn(relevantRunIds);
    var updatableRunIds = new HashSet<>(relevantRunIds);
    remainingRunIds.forEach(updatableRunIds::remove);

    // re-store all journeys into the db that were actually updated and are not active
    for (var journeyEntry : relevantJourneys) {
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

  @Transactional
  public void markJourneyAsFirstSeen(@NonNull UUID journeyId) {
    this.journeyRepository.findById(journeyId)
      .filter(journey -> journey.getFirstSeenTime() == null)
      .ifPresent(journey -> {
        journey.setFirstSeenTime(Instant.now());
        journey.setLastSeenTime(null);
        journey.setCancelled(false);
        this.journeyRepository.save(journey);
      });
  }

  @Transactional
  public void markJourneyAsLastSeen(@NonNull Collection<UUID> journeyIds) {
    var now = Instant.now();
    var journeys = this.journeyRepository.findAllById(journeyIds);
    for (var journey : journeys) {
      journey.setLastSeenTime(now);
      this.journeyRepository.save(journey);
    }
  }
}
