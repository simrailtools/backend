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

package tools.simrail.backend.api.journey;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.simrail.backend.api.journey.converter.JourneyDtoConverter;
import tools.simrail.backend.api.journey.converter.JourneySummaryDtoConverter;
import tools.simrail.backend.api.journey.data.ApiJourneyEventRepository;
import tools.simrail.backend.api.journey.data.ApiJourneyRepository;
import tools.simrail.backend.api.journey.data.JourneyEventSummaryProjection;
import tools.simrail.backend.api.journey.data.JourneySummaryProjection;
import tools.simrail.backend.api.journey.dto.JourneyDto;
import tools.simrail.backend.api.journey.dto.JourneySummaryDto;
import tools.simrail.backend.api.journey.dto.JourneySummaryWithEventDto;
import tools.simrail.backend.api.journey.dto.JourneySummaryWithLiveDataDto;
import tools.simrail.backend.api.pagination.PaginatedResponseDto;
import tools.simrail.backend.common.cache.DataCache;
import tools.simrail.backend.common.journey.JourneyTransportType;
import tools.simrail.backend.common.proto.EventBusProto;

@Service
class JourneyService {

  private final ApiJourneyRepository journeyRepository;
  private final ApiJourneyEventRepository journeyEventRepository;
  private final DataCache<EventBusProto.JourneyUpdateFrame> journeyCache;

  private final JourneyDtoConverter journeyDtoConverter;
  private final JourneySummaryDtoConverter journeySummaryDtoConverter;

  @Autowired
  JourneyService(
    @NonNull ApiJourneyRepository journeyRepository,
    @NonNull ApiJourneyEventRepository journeyEventRepository,
    @NonNull JourneyDtoConverter journeyDtoConverter,
    @NonNull JourneySummaryDtoConverter journeySummaryDtoConverter,
    @NonNull @Qualifier("journey_realtime_cache") DataCache<EventBusProto.JourneyUpdateFrame> journeyDataCache
  ) {
    this.journeyRepository = journeyRepository;
    this.journeyEventRepository = journeyEventRepository;
    this.journeyCache = journeyDataCache;
    this.journeyDtoConverter = journeyDtoConverter;
    this.journeySummaryDtoConverter = journeySummaryDtoConverter;
  }

  /**
   * Get a journey by the given id. This method either returns from DB or from cache.
   *
   * @param journeyId the id of the journey to get.
   * @return an optional holding the journey with the given id, if one exists.
   */
  @Cacheable(cacheNames = "journey_cache", key = "'by_id_' + #journeyId")
  public @NonNull Optional<JourneyDto> findById(@NonNull UUID journeyId) {
    return this.journeyRepository.findWithEventsById(journeyId).map(journey -> {
      var liveData = this.journeyCache.findByPrimaryKey(journeyId.toString());
      return this.journeyDtoConverter.apply(journey, liveData);
    });
  }

  /**
   * Get all journeys by the given ids. This method either returns from DB or from cache.
   *
   * @param journeyIds the ids of the journeys to get.
   * @return a list of all journeys that were successfully resolved based on an id in the given id collection.
   */
  @Cacheable(cacheNames = "journey_cache", key = "'by_ids_' + #journeyIds")
  public @NonNull List<JourneyDto> findByIds(@NonNull Collection<UUID> journeyIds) {
    if (journeyIds.isEmpty()) {
      return List.of();
    } else {
      return this.journeyRepository.findWithEventsByIdIn(journeyIds).stream().map(journey -> {
        @SuppressWarnings("DataFlowIssue") // journey id cannot be null here
        var liveData = this.journeyCache.findByPrimaryKey(journey.getId().toString());
        return this.journeyDtoConverter.apply(journey, liveData);
      }).toList();
    }
  }

  /**
   * Get a list of all active journey on the server with the given id.
   *
   * @param serverId the id of the server to get the active journeys on.
   * @return all journeys that are currently active on the given server.
   */
  @Transactional(readOnly = true)
  public @NonNull Pair<Instant, List<JourneySummaryWithLiveDataDto>> findActiveJourneys(@NonNull String serverId) {
    var activeJourneyData = this.journeyCache.cachedValuesSnapshot()
      .stream()
      .filter(snapshot -> snapshot.getIds().getServerId().equals(serverId))
      .collect(Collectors.toMap(frame -> UUID.fromString(frame.getIds().getDataId()), Function.identity()));

    var journeyIds = activeJourneyData.keySet().toArray(UUID[]::new);
    var journeySummaries = this.journeyRepository.findJourneySummariesByJourneyIds(journeyIds);
    var journeyEventTails = this.journeyEventRepository.findFirstAndLastEventOfJourneys(journeyIds)
      .stream()
      .collect(Collectors.groupingBy(JourneyEventSummaryProjection::getJourneyId));

    var convertedSummaries = journeySummaries.stream().map(summary -> {
      var journeyData = activeJourneyData.get(summary.getId()).getJourneyData(); // must be present
      var journeyTailEvents = journeyEventTails.get(summary.getId());
      if (journeyTailEvents == null || journeyTailEvents.size() != 2) {
        return null;
      }

      // get the first and last event, possibly need to re-order them
      // as they are not required to be in a particular order
      var firstEvent = journeyTailEvents.getFirst();
      var lastEvent = journeyTailEvents.getLast();
      if (firstEvent.getEventIndex() > lastEvent.getEventIndex()) {
        var prevLast = lastEvent;
        lastEvent = firstEvent;
        firstEvent = prevLast;
      }

      return this.journeySummaryDtoConverter.convert(summary, firstEvent, lastEvent, journeyData);
    }).filter(Objects::nonNull).toList();
    return Pair.of(Instant.now(), convertedSummaries);
  }

  /**
   * Finds a journey by a single matching event along its route. Search results are either returned from cache or from
   * querying the database.
   *
   * @param page            the page of results to return, defaults to 1.
   * @param limit           the maximum amount of journeys to return, defaults to 20.
   * @param serverId        the id of the server to return journeys on.
   * @param date            the date that one event is happening on, not null.
   * @param line            the line that must match at one event along the journey route.
   * @param journeyNumber   the number that must be used at one event along the journey route.
   * @param journeyCategory the category of the journey at one event along the journey route.
   * @param transportTypes  the accepted transport types that the journey must have along the route, not null or empty.
   * @return a pagination wrapper around the query results based on the given filter parameters.
   */
  @Cacheable(cacheNames = "journey_search_cache", sync = true)
  public @NonNull PaginatedResponseDto<JourneySummaryDto> findByEvent(
    @Nullable Integer page,
    @Nullable Integer limit,
    @NonNull UUID serverId,
    @NonNull LocalDate date,
    @Nullable String line,
    @Nullable String journeyNumber,
    @Nullable String journeyCategory,
    @NonNull Set<JourneyTransportType> transportTypes
  ) {
    // build the pagination parameter
    int indexedPage = Objects.requireNonNullElse(page, 1) - 1;
    int requestedLimit = Objects.requireNonNullElse(limit, 20);
    int offset = requestedLimit * indexedPage;

    // query and map the results
    var transportTypeArray = transportTypes.stream().map(Enum::name).toArray(String[]::new);
    var queriedItems = this.journeyRepository.findJourneySummariesByMatchingEvent(
      serverId,
      date,
      line,
      journeyNumber,
      journeyCategory,
      transportTypeArray,
      requestedLimit + 1, // request one more to check if more elements are available
      offset);
    return this.filterJourneys(requestedLimit, queriedItems, (journey, eventPair) -> {
      var firstEvent = eventPair.getFirst();
      var lastEvent = eventPair.getSecond();
      return this.journeySummaryDtoConverter.convert(journey, firstEvent, lastEvent);
    });
  }

  /**
   * Finds a journey by the first playable event along its route. Search results are either returned from cache or from
   * querying the database.
   *
   * @param page            the page of results to return, defaults to 1.
   * @param limit           the maximum amount of journeys to return, defaults to 20.
   * @param serverId        the id of the server to return journeys on.
   * @param timeStart       the time range start from which journeys are returned, not null.
   * @param timeEnd         the time range end from which journeys are returned, not null.
   * @param journeyCategory the category of the journey at one event along the journey route.
   * @param transportTypes  the accepted transport types that the journey must have along the route, not null or empty.
   * @return a pagination wrapper around the query results based on the given filter parameters.
   */
  @Cacheable(cacheNames = "journey_search_cache", sync = true)
  public @NonNull PaginatedResponseDto<JourneySummaryWithEventDto> findByPlayableDeparture(
    @Nullable Integer page,
    @Nullable Integer limit,
    @NonNull UUID serverId,
    @NonNull LocalDateTime timeStart,
    @NonNull LocalDateTime timeEnd,
    @Nullable String journeyCategory,
    @NonNull Set<JourneyTransportType> transportTypes
  ) {
    // build the pagination parameter
    int indexedPage = Objects.requireNonNullElse(page, 1) - 1;
    int requestedLimit = Objects.requireNonNullElse(limit, 20);
    int offset = requestedLimit * indexedPage;

    // query and map the results
    var transportTypeArray = transportTypes.stream().map(Enum::name).toArray(String[]::new);
    var queriedItems = this.journeyRepository.findJourneySummariesByTimeAtPlayableBorderEnter(
      serverId,
      journeyCategory,
      transportTypeArray,
      timeStart,
      timeEnd,
      requestedLimit + 1, // request one more to check if more elements are available
      offset);
    return this.filterJourneys(requestedLimit, queriedItems, (journey, eventPair) -> {
      var firstEvent = eventPair.getFirst();
      var lastEvent = eventPair.getSecond();
      return this.journeySummaryDtoConverter.convert(journey, firstEvent, lastEvent);
    });
  }

  /**
   * Finds all journeys that have the given railcar in their vehicle composition on the given date.
   *
   * @param page             the page of results to return, defaults to 1.
   * @param limit            the maximum amount of journeys to return, defaults to 20.
   * @param serverId         the id of the server to return journeys on.
   * @param date             the date to filter the journeys on.
   * @param requiredRailcars the railcars that must be included in the vehicle composition of the journey.
   * @param journeyCategory  the category of the journey at one event along the journey route.
   * @param transportTypes   the accepted transport types of the journey must have along the route, not null or empty.
   * @return a paginated response holding all journeys that use the given railcar on the given date.
   */
  @Cacheable(cacheNames = "journey_search_cache", sync = true)
  public @NonNull PaginatedResponseDto<JourneySummaryDto> findByRailcars(
    @Nullable Integer page,
    @Nullable Integer limit,
    @NonNull UUID serverId,
    @NonNull LocalDate date,
    @NonNull Set<UUID> requiredRailcars,
    @Nullable String journeyCategory,
    @NonNull Set<JourneyTransportType> transportTypes
  ) {
    // build the pagination parameter
    int indexedPage = Objects.requireNonNullElse(page, 1) - 1;
    int requestedLimit = Objects.requireNonNullElse(limit, 20);
    int offset = requestedLimit * indexedPage;

    // query and map the results
    var requiredRailcarArray = requiredRailcars.toArray(UUID[]::new);
    var transportTypesArray = transportTypes.stream().map(Enum::name).toArray(String[]::new);
    var queriedItems = this.journeyRepository.findJourneySummariesByRailcar(
      serverId,
      date,
      requiredRailcarArray,
      journeyCategory,
      transportTypesArray,
      requestedLimit + 1, // request one more to check if more elements are available
      offset);
    return this.filterJourneys(requestedLimit, queriedItems, (journey, eventPair) -> {
      var firstEvent = eventPair.getFirst();
      var lastEvent = eventPair.getSecond();
      return this.journeySummaryDtoConverter.convert(journey, firstEvent, lastEvent);
    });
  }

  /**
   * Filters the journey summary query results and maps them according to the request parameters into a paginated
   * response DTO.
   *
   * @param requestedLimit the requested maximum items to be returned.
   * @param queriedItems   the items that were actually queried from the database.
   * @return a paginated response wrapper for the queried items.
   */
  private @NonNull <P extends JourneySummaryProjection, R> PaginatedResponseDto<R> filterJourneys(
    int requestedLimit,
    @NonNull List<P> queriedItems,
    @NonNull BiFunction<P, Pair<JourneyEventSummaryProjection, JourneyEventSummaryProjection>, R> dtoConverter
  ) {
    // return an empty response if no items were queried
    if (queriedItems.isEmpty()) {
      return new PaginatedResponseDto<>(List.of(), false);
    }

    var morePagesAvailable = queriedItems.size() > requestedLimit;
    var returnedItemCount = morePagesAvailable ? requestedLimit : queriedItems.size();

    // get the first and last event per queried journey
    var queriesJourneyIds = queriedItems.stream()
      .limit(returnedItemCount)
      .map(JourneySummaryProjection::getId)
      .toArray(UUID[]::new);
    var eventsByJourneyId = this.journeyEventRepository.findFirstAndLastEventOfJourneys(queriesJourneyIds)
      .stream()
      .collect(Collectors.groupingBy(JourneyEventSummaryProjection::getJourneyId));

    // convert all queried items into a summary DTO, including the first and last event
    var returnedItems = queriedItems.stream()
      .limit(returnedItemCount)
      .filter(journey -> eventsByJourneyId.containsKey(journey.getId()))
      .map(journey -> {
        // events are in a random order, therefore we need to check which one is actually the first event
        var associatedEvents = eventsByJourneyId.get(journey.getId());
        var firstEvent = associatedEvents.getFirst();
        var lastEvent = associatedEvents.get(1);
        var eventPair = firstEvent.getEventIndex() == 0
          ? Pair.of(firstEvent, lastEvent)
          : Pair.of(lastEvent, firstEvent);
        return dtoConverter.apply(journey, eventPair);
      })
      .toList();
    return new PaginatedResponseDto<>(returnedItems, morePagesAvailable);
  }
}
