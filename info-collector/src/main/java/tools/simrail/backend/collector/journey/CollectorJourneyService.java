/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024 Pasqual Koschmieder and contributors
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
import jakarta.persistence.EntityManager;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.simrail.backend.common.journey.JourneyEntity;
import tools.simrail.backend.common.journey.JourneyEventEntity;
import tools.simrail.backend.common.journey.JourneyEventRepository;
import tools.simrail.backend.common.journey.JourneyRepository;

/**
 * Service for accessing and updating journeys.
 */
@Service
public class CollectorJourneyService {

  private final EntityManager entityManager;
  private final JourneyRepository journeyRepository;
  private final JourneyEventRepository journeyEventRepository;

  @Autowired
  public CollectorJourneyService(
    @Nonnull EntityManager entityManager,
    @Nonnull JourneyRepository journeyRepository,
    @Nonnull JourneyEventRepository journeyEventRepository
  ) {
    this.entityManager = entityManager;
    this.journeyRepository = journeyRepository;
    this.journeyEventRepository = journeyEventRepository;
  }

  /**
   * Finds a single journey by the given id, either from cache or using the database.
   *
   * @param journeyId the id of the journey to get.
   * @return an optional holding the journey with the given id, if one exists.
   */
  @Nonnull
  @Cacheable(cacheNames = "journey", key = "'ji_' + #journeyId")
  public Optional<JourneyEntity> findJourneyById(@Nonnull UUID journeyId) {
    return this.journeyRepository.findById(journeyId);
  }

  /**
   * Finds a single journey by the given server code and foreign run id, either from cache or using the database.
   *
   * @param serverCode   the server code on which the journey is running.
   * @param foreignRunId the run id provided by the SimRail api of the journey.
   * @return an optional holding the journey on the given server with the given run id, if one exists.
   */
  @Nonnull
  @Cacheable(cacheNames = "journey", key = "'jfr_' + #serverCode + '_' + #foreignRunId")
  public Optional<JourneyEntity> findByServerCodeAndForeignRunId(
    @Nonnull String serverCode,
    @Nonnull UUID foreignRunId
  ) {
    return this.journeyRepository.findByServerCodeAndForeignRunId(serverCode, foreignRunId);
  }

  /**
   * Finds a single active journey by the given server code and foreign id, either from cache or using the database.
   *
   * @param serverCode the server code on which the journey is running.
   * @param foreignId  the foreign id of the journey provided by the SimRail api.
   * @return an optional holding the active journey on the given server with the given foreign id, if one exists.
   */
  @Nonnull
  @Cacheable(cacheNames = "journey", key = "'jf_' + #serverCode + '_' + #foreignId")
  public Optional<JourneyEntity> findActiveTrainByServerCodeAndForeignId(
    @Nonnull String serverCode,
    @Nonnull String foreignId
  ) {
    return this.journeyRepository.findLastActiveTrainByServerCodeAndForeignId(serverCode, foreignId)
      .map(journey -> {
        var firstSeenTime = journey.getFirstSeenTime();
        var elapsed = Duration.between(OffsetDateTime.now(), firstSeenTime).abs();
        return elapsed.toHours() >= 12 ? null : journey;
      });
  }

  /**
   * Finds all journeys that are happening on the server with the given id and whose foreign run id is in the given
   * collection of runs.
   *
   * @param serverId the server id to filter for the journeys.
   * @param runIds   the id of the runs to filter for.
   * @return all journeys on the server with the given id and whose run id is in the given run id list.
   */
  @Nonnull
  public List<JourneyEntity> findJourneysOnServerByRunIds(@Nonnull UUID serverId, @Nonnull List<UUID> runIds) {
    return this.journeyRepository.findAllByServerIdAndForeignRunIdIn(serverId, runIds);
  }

  /**
   * Persists a single journey into the database and local cache.
   *
   * @param journey the journey to persist.
   */
  @Caching(put = {
    @CachePut(cacheNames = "journey", key = "'ji_' + #journey.id"),
    @CachePut(cacheNames = "journey", key = "'jf_' + #journey.serverCode + '_' + #journey.foreignId"),
    @CachePut(cacheNames = "journey", key = "'jfr_' + #journey.serverCode + '_' + #journey.foreignRunId"),
  })
  public @Nonnull JourneyEntity persistJourney(@Nonnull JourneyEntity journey) {
    return this.journeyRepository.save(journey);
  }

  /**
   * Persists all given journey events in one batch, cleaning all previous known events of the journey.
   *
   * @param journey the journey to which the given events are related.
   * @param events  the events that are associated with the given journey that should be persisted.
   */
  @Transactional
  @Caching(evict = {
    @CacheEvict(cacheNames = "journey", key = "'ji_' + #journey.id"),
    @CacheEvict(cacheNames = "journey", key = "'jf_' + #journey.serverCode + '_' + #journey.foreignId"),
    @CacheEvict(cacheNames = "journey", key = "'jfr_' + #journey.serverCode + '_' + #journey.foreignRunId"),
  })
  public void updateJourneyEvents(@Nonnull JourneyEntity journey, @Nonnull List<JourneyEventEntity> events) {
    // pre-delete all entities that are associated with the journey
    this.journeyEventRepository.deleteAllByJourneyId(journey.getId());

    // persist all events (insert) and execute the persistence after, in a single bulk operation
    events.forEach(this.entityManager::persist);
    this.entityManager.flush();
    this.entityManager.clear();
  }
}
