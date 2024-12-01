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

package tools.simrail.backend.api.journey;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import tools.simrail.backend.api.journey.converter.JourneyDtoConverter;
import tools.simrail.backend.api.journey.converter.JourneySummaryDtoConverter;
import tools.simrail.backend.api.journey.data.ApiJourneyEventRepository;
import tools.simrail.backend.api.journey.data.ApiJourneyRepository;
import tools.simrail.backend.api.journey.data.JourneyEventSummaryProjection;
import tools.simrail.backend.api.journey.data.JourneySummaryProjection;
import tools.simrail.backend.api.journey.dto.JourneyDto;
import tools.simrail.backend.api.journey.dto.JourneySummaryDto;
import tools.simrail.backend.api.pagination.PaginatedResponseDto;

@Service
class JourneyService {

  private final ApiJourneyRepository journeyRepository;
  private final ApiJourneyEventRepository journeyEventRepository;

  private final JourneyDtoConverter journeyDtoConverter;
  private final JourneySummaryDtoConverter journeySummaryDtoConverter;

  @Autowired
  public JourneyService(
    @Nonnull ApiJourneyRepository journeyRepository,
    @Nonnull ApiJourneyEventRepository journeyEventRepository,
    @Nonnull JourneyDtoConverter journeyDtoConverter,
    @Nonnull JourneySummaryDtoConverter journeySummaryDtoConverter
  ) {
    this.journeyRepository = journeyRepository;
    this.journeyEventRepository = journeyEventRepository;
    this.journeyDtoConverter = journeyDtoConverter;
    this.journeySummaryDtoConverter = journeySummaryDtoConverter;
  }

//  @Cacheable(cacheNames = "journey", key = "'by_id_' + #journeyId")
  public @Nonnull Optional<JourneyDto> findById(@Nonnull UUID journeyId) {
    return this.journeyRepository.findWithEventsById(journeyId).map(this.journeyDtoConverter);
  }

  /**
   * Finds details about a journey based on the relation of it. This means that the first and last events are used to
   * determine if a journey would match.
   *
   * @param page
   * @param limit
   * @param serverId
   * @param startTime
   * @param startStationId
   * @param startJourneyNumber
   * @param startJourneyCategory
   * @param endTime
   * @param endStationId
   * @return
   */
  public @Nonnull PaginatedResponseDto<JourneySummaryDto> findByRelation(
    @Nullable Integer page,
    @Nullable Integer limit,
    @Nullable UUID serverId,
    @Nullable OffsetDateTime startTime,
    @Nullable UUID startStationId,
    @Nullable String startJourneyNumber,
    @Nullable String startJourneyCategory,
    @Nullable OffsetDateTime endTime,
    @Nullable UUID endStationId
  ) {
    //
    int indexedPage = Objects.requireNonNullElse(page, 1) - 1;
    int requestedLimit = Objects.requireNonNullElse(limit, 20);
    int offset = requestedLimit * indexedPage;

    //
    var queriedItems = this.journeyRepository.findMatchingJourneySummaries(
      serverId,
      startTime,
      startStationId,
      startJourneyNumber,
      startJourneyCategory,
      endTime,
      endStationId,
      requestedLimit + 1, // request one more to check if more elements are available
      offset);
    var morePagesAvailable = queriedItems.size() > requestedLimit;
    var returnedItemCount = morePagesAvailable ? requestedLimit : queriedItems.size();

    //
    var queriesJourneyIds = queriedItems.stream()
      .limit(returnedItemCount)
      .map(JourneySummaryProjection::getId)
      .toList();
    var eventsByJourneyId = this.journeyEventRepository.findFirstAndLastEventOfJourneys(queriesJourneyIds)
      .stream()
      .collect(Collectors.groupingBy(JourneyEventSummaryProjection::getJourneyId));

    //
    var returnedItems = queriedItems.stream()
      .limit(returnedItemCount)
      .map(journey -> {
        // events are in a random order, therefore we need to check which one is actually the first event
        var associatedEvents = eventsByJourneyId.get(journey.getId());
        var firstEvent = associatedEvents.getFirst();
        var lastEvent = associatedEvents.get(1);
        if (firstEvent.getEventIndex() == 0) {
          return this.journeySummaryDtoConverter.convert(journey, firstEvent, lastEvent);
        } else {
          return this.journeySummaryDtoConverter.convert(journey, lastEvent, firstEvent);
        }
      })
      .toList();
    return new PaginatedResponseDto<>(returnedItems, morePagesAvailable);
  }
}
