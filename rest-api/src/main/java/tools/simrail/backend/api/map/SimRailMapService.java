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

package tools.simrail.backend.api.map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import tools.simrail.backend.api.journey.dto.JourneyGeoPositionDto;
import tools.simrail.backend.api.journey.dto.JourneyStopPlaceDto;
import tools.simrail.backend.api.map.data.MapEventSummaryProjection;
import tools.simrail.backend.api.map.data.MapJourneyEventRepository;
import tools.simrail.backend.api.map.dto.MapJourneyRouteDto;
import tools.simrail.backend.api.map.dto.MapJourneyRouteDtoConverter;
import tools.simrail.backend.common.point.SimRailPointProvider;
import tools.simrail.backend.external.brouter.BRouterApiClient;
import tools.simrail.backend.external.brouter.request.BRouterRouteRequest;

@Service
class SimRailMapService {

  private static final Comparator<MapEventSummaryProjection> EVENT_SORTER
    = Comparator.comparingInt(MapEventSummaryProjection::getEventIndex);

  private final Cache polylineCache;
  private final ObjectMapper objectMapper;
  private final BRouterApiClient bRouterApiClient;
  private final SimRailPointProvider pointProvider;
  private final MapJourneyEventRepository journeyEventRepository;
  private final MapJourneyRouteDtoConverter journeyRouteDtoConverter;

  @Autowired
  public SimRailMapService(
    @Nonnull CacheManager cacheManager,
    @Nonnull ObjectMapper objectMapper,
    @Nonnull BRouterApiClient bRouterApiClient,
    @Nonnull SimRailPointProvider pointProvider,
    @Nonnull MapJourneyEventRepository repository,
    @Nonnull MapJourneyRouteDtoConverter journeyRouteDtoConverter
  ) {
    this.objectMapper = objectMapper;
    this.bRouterApiClient = bRouterApiClient;
    this.pointProvider = pointProvider;
    this.journeyEventRepository = repository;
    this.journeyRouteDtoConverter = journeyRouteDtoConverter;
    this.polylineCache = cacheManager.getCache("points_polyline_cache");
  }

  /**
   * Resolve the stops and polyline for a single journey.
   *
   * @param journeyId                the id of the journey to resolve the information for.
   * @param includeCancelled         if canceled events should be included in the polyline
   * @param includeAdditional        if additional events should be included in the polyline.
   * @param allowFallbackComputation if a fallback polyline should be computed if a nice one is unavailable.
   * @return an optional DTO for the journey polyline, empty if some info cannot be resolved.
   */
  @Cacheable(cacheNames = "journey_polyline_cache")
  public @Nonnull Optional<MapJourneyRouteDto> polylineByJourneyId(
    @Nonnull UUID journeyId,
    boolean includeCancelled,
    boolean includeAdditional,
    boolean allowFallbackComputation
  ) {
    // resolve the events along the journey route
    var eventsAlongRoute = this.journeyEventRepository
      .findMapEventDataByJourneyId(journeyId, includeCancelled, includeAdditional);
    if (eventsAlongRoute.isEmpty()) {
      return Optional.empty();
    }

    // map the stops that are along the journey route
    var stopsAlongRoute = eventsAlongRoute.stream()
      .sorted(EVENT_SORTER)
      .map(event -> this.pointProvider.findPointByIntId(event.getPointId())
        .map(point -> {
          var pos = new JourneyGeoPositionDto(point.getPosition().getLatitude(), point.getPosition().getLongitude());
          return new JourneyStopPlaceDto(
            event.getPointId(),
            point.getName(),
            pos,
            point.isStopPlace(),
            event.isPointPlayable());
        })
        .orElse(null))
      .filter(Objects::nonNull)
      .toList();

    // resolve the journey polyline
    var polyline = this.resolveJourneyPolyline(stopsAlongRoute);
    if (polyline == null) {
      if (allowFallbackComputation) {
        // fallback to a polyline that just plainly connects all stops along the route
        polyline = this.objectMapper.createArrayNode();
        var stopPositions = stopsAlongRoute.stream().map(stop -> {
          var node = this.objectMapper.createArrayNode();
          node.add(stop.position().longitude());
          node.add(stop.position().latitude());
          return node;
        }).toList();
        polyline.addAll(stopPositions);
      } else {
        return Optional.empty();
      }
    }

    // convert & combine the information into a DTO
    var convertedRoute = this.journeyRouteDtoConverter.convert(journeyId, stopsAlongRoute, polyline);
    return Optional.of(convertedRoute);
  }

  /**
   * Resolves the coordinates of the polyline for the given stops along a journey route, either from cache or from the
   * BRouter api (using the rail profile).
   *
   * @param stopsAlongRoute the stops long the journey route to convert to a polyline.
   * @return the coordinate array for the polyline of the journey.
   */
  private @Nullable ArrayNode resolveJourneyPolyline(@Nonnull List<JourneyStopPlaceDto> stopsAlongRoute) {
    try {
      var pointsHash = stopsAlongRoute.stream()
        .map(JourneyStopPlaceDto::id)
        .mapToInt(UUID::hashCode)
        .reduce(0, (left, right) -> 31 * left + right);
      var pointsCacheKey = "points_" + pointsHash;
      return this.polylineCache.get(pointsCacheKey, () -> {
        // request the geojson data for the stops along the route
        var positionsAlongRoute = stopsAlongRoute.stream()
          .filter(place -> !place.stopPlace())
          .map(JourneyStopPlaceDto::position)
          .map(pos -> new BRouterRouteRequest.GeoPosition(pos.latitude(), pos.longitude()))
          .toList();
        var routeRequest = BRouterRouteRequest.create()
          .withProfile("rail")
          .withPositions(positionsAlongRoute)
          .withOutputFormat(BRouterRouteRequest.OutputFormat.GEOJSON)
          .withAlternativeMode(BRouterRouteRequest.AlternativeMode.DEFAULT);
        var rawRouteGeoJson = this.bRouterApiClient.route(routeRequest);
        var routeGeoJson = this.objectMapper.readTree(rawRouteGeoJson);

        // extract the geo positions
        var lineCoordinates = routeGeoJson
          .path("features")
          .path(0)
          .path("geometry")
          .path("coordinates");
        return lineCoordinates instanceof ArrayNode an ? an : null;
      });
    } catch (Cache.ValueRetrievalException _) {
      // can happen if the value loader throws, most likely due to an upstream issue with the BRouter api
      return null;
    }
  }
}
