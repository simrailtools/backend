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
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.simrail.backend.common.journey.JourneyEntity;
import tools.simrail.backend.common.journey.JourneyEventEntity;
import tools.simrail.backend.common.journey.JourneyEventRepository;

/**
 * Service for accessing and updating journeys.
 */
@Service
class CollectorJourneyService {

  private static final Comparator<JourneyEventEntity> EVENT_BY_INDEX_COMPARATOR =
    Comparator.comparingInt(JourneyEventEntity::getEventIndex);

  private final EntityManager entityManager;
  private final CollectorJourneyRepository journeyRepository;
  private final JourneyEventRepository journeyEventRepository;
  private final CollectorJourneyVehicleRepository journeyVehicleRepository;

  private final Map<UUID, Map<UUID, JourneyEntity>> activeJourneysByServer;

  @Autowired
  public CollectorJourneyService(
    @Nonnull EntityManager entityManager,
    @Nonnull CollectorJourneyRepository journeyRepository,
    @Nonnull JourneyEventRepository journeyEventRepository,
    @Nonnull CollectorJourneyVehicleRepository journeyVehicleRepository
  ) {
    this.entityManager = entityManager;
    this.journeyRepository = journeyRepository;
    this.journeyEventRepository = journeyEventRepository;
    this.journeyVehicleRepository = journeyVehicleRepository;
    this.activeJourneysByServer = new ConcurrentHashMap<>();
  }

  /**
   * Populates the active journey cache when this service is constructed for the first time.
   */
  @PostConstruct
  public void populateActiveJourneyCache() {
    var storedActiveJourney = this.journeyRepository.findAllByFirstSeenTimeIsNotNullAndLastSeenTimeIsNull();
    for (var journey : storedActiveJourney) {
      var journeysOfServer = this.activeJourneysByServer.computeIfAbsent(journey.getServerId(), _ -> new HashMap<>());
      journeysOfServer.put(journey.getId(), journey);
    }
  }

  /**
   * Retrieves all journeys by the given run id from the database.
   *
   * @param runIds the ids of the runs to get the associated journey of.
   * @return the journeys that are associated with one of the given run ids.
   */
  @Nonnull
  public List<JourneyEntity> retrieveJourneysByRunIds(@Nonnull List<UUID> runIds) {
    return this.journeyRepository.findAllByForeignRunIdIn(runIds);
  }

  /**
   * Retrieves all journey events that are associated with a journey on the given server and with one of the run ids.
   *
   * @param serverId the id of the server where the journeys are running on.
   * @param runIds   the ids of the runs to get the associated journey events of.
   * @return the journey events associated with a journey on the given server and with one of the run ids.
   */
  @Nonnull
  public List<JourneyEventEntity> retrieveInactiveJourneyEventsOfServerByRunIds(
    @Nonnull UUID serverId,
    @Nonnull List<UUID> runIds
  ) {
    return this.journeyEventRepository.findAllInactiveByServerIdAndRunId(serverId, runIds);
  }

  /**
   * Resolves the active journeys for the given server by using the given server id. Note that changes to the returned
   * map will directly reflect into the cache and vice versa. If no data is cached for the server with the given id, the
   * data is loaded from the database.
   *
   * @param serverId the id of the server to get the active journeys of, either from cache or from database.
   * @return the active journeys of the server with the given id.
   */
  @Nonnull
  public Map<UUID, JourneyEntity> resolveCachedActiveJourneysOfServer(@Nonnull UUID serverId) {
    return this.activeJourneysByServer.computeIfAbsent(serverId, _ -> {
      // resolve the active journeys from the database
      var stored = this.journeyRepository.findAllByServerIdAndFirstSeenTimeIsNotNullAndLastSeenTimeIsNull(serverId);
      return stored.stream().collect(Collectors.toMap(JourneyEntity::getId, Function.identity()));
    });
  }

  /**
   * Resolves the active journeys for the given server by using the given server id, fetching and caching all missing
   * journeys which are not cached but requested by the given runs id collection. Note that changes to the returned
   * collection are reflected into the cache and vice vera. Additions to the returned collection are not possible.
   *
   * @param serverId the id of the server to get the active journeys of, either from cache or from database.
   * @param runIds   the ids of the runs that must be included in the cache.
   * @return the active journeys for the given server, at least containing the runs with the given ids.
   */
  @Nonnull
  public Collection<JourneyEntity> resolveCachedJourneysOfServer(@Nonnull UUID serverId, @Nonnull List<UUID> runIds) {
    var cachedJourneys = this.activeJourneysByServer.get(serverId);
    if (cachedJourneys == null) {
      // there are no journeys cached for the server currently, retrieve them from the database
      var journeysOfServer = this.journeyRepository.findAllByServerIdAndForeignRunIdIn(serverId, runIds)
        .stream()
        .collect(Collectors.toMap(JourneyEntity::getId, Function.identity()));
      this.activeJourneysByServer.put(serverId, journeysOfServer);
      return journeysOfServer.values();
    } else {
      // there are journeys cached already for the requested server, check if all requested
      // run ids are in the cache as well, else request them from the database
      var missingRunIds = new ArrayList<>(runIds);
      cachedJourneys.forEach((_, journey) -> missingRunIds.remove(journey.getForeignRunId()));
      if (!missingRunIds.isEmpty()) {
        var remainingRuns = this.journeyRepository.findAllByServerIdAndForeignRunIdIn(serverId, missingRunIds);
        remainingRuns.forEach(journey -> cachedJourneys.put(journey.getId(), journey));
      }

      return cachedJourneys.values();
    }
  }

  /**
   * Resolves the journey events that are associated with the given journey ids.
   *
   * @param journeyIds the ids of the journeys to get the events of.
   * @return the journey events for all requested journeys, in a journey id to events mapping.
   */
  @Nonnull
  public Map<UUID, List<JourneyEventEntity>> resolveJourneyEvents(@Nonnull Collection<UUID> journeyIds) {
    return this.journeyEventRepository.findAllByJourneyIdIn(journeyIds)
      .stream()
      .collect(Collectors.groupingBy(JourneyEventEntity::getJourneyId, Collectors.collectingAndThen(
        Collectors.toList(),
        events -> {
          events.sort(EVENT_BY_INDEX_COMPARATOR);
          return events;
        }
      )));
  }

  /**
   * Persists a single journey into the database and local cache.
   *
   * @param journey the journey to persist.
   */
  @Transactional
  public @Nonnull JourneyEntity persistJourney(@Nonnull JourneyEntity journey) {
    return this.journeyRepository.save(journey);
  }

  /**
   * Updates all the given journeys that are running on the server with the given id in one batch while also updating
   * the references to these entities in the active journey cache. If an entity is not already stored in the said
   * journey cache, it won't be added into the cache by this method.
   *
   * @param serverId the id of the server on which all the given journeys to update are happening.
   * @param journeys the journeys that should be updated in the database and potentially in the cache.
   */
  public void persistJourneysAndPopulateCache(@Nonnull UUID serverId, @Nonnull Collection<JourneyEntity> journeys) {
    var savedEntities = this.journeyRepository.saveAll(journeys);
    var cachedServerJourneys = this.activeJourneysByServer.get(serverId);
    if (cachedServerJourneys != null) {
      // update the cache based on the entities that were saved for the next save attempt
      // if a journey was removed from the cache, don't try to re-add it to the cache as it means
      // that the journey was removed on the server as well
      for (var savedEntity : savedEntities) {
        if (cachedServerJourneys.containsKey(savedEntity.getId())) {
          cachedServerJourneys.put(savedEntity.getId(), savedEntity);
        }
      }
    }
  }

  /**
   * Persists all new journey events created during journey data collection on a server. Events that are not new will
   * not be saved directly this is up to the persistence context to do once the current transaction closes.
   *
   * @param journeyEvents the updated journey events to persist.
   */
  public void persistJourneyEvents(@Nonnull Collection<JourneyEventEntity> journeyEvents) {
    for (JourneyEventEntity event : journeyEvents) {
      if (event.isNew()) {
        this.journeyEventRepository.save(event);
      }
    }
  }

  /**
   * Wipes the given journey completely from the database, removing all references to it.
   *
   * @param journeyId the id of the journey to wipe from the database.
   */
  @Transactional
  public void wipeJourney(@Nonnull UUID journeyId) {
    this.journeyEventRepository.deleteAllByJourneyId(journeyId);
    this.journeyRepository.deleteById(journeyId);
    this.journeyVehicleRepository.deleteAllByJourneyId(journeyId);
  }

  /**
   * Persists all given journey events in one batch, cleaning all previous known events of the journey.
   *
   * @param serverId  the id of the server where the associated journey events are happening.
   * @param journeyId the id of the journey to which the given events are related.
   * @param events    the events that are associated with the given journey that should be persisted.
   */
  @Transactional
  public void forcePersistJourneyEvents(
    @Nonnull UUID serverId,
    @Nonnull UUID journeyId,
    @Nonnull List<JourneyEventEntity> events
  ) {
    // check if the associated journey became active concurrent to the timetable collection operation
    // this check is not 100% perfect as there still might be a race, but it should be good enough
    var cachedJourneys = this.activeJourneysByServer.get(serverId);
    if (cachedJourneys == null || !cachedJourneys.containsKey(journeyId)) {
      // pre-delete all entities that are associated with the journey
      this.journeyEventRepository.deleteAllByJourneyId(journeyId);

      // persist all events (insert) and execute the persistence after, in a single bulk operation
      events.forEach(this.entityManager::persist);
      this.entityManager.flush();
    }
  }
}
