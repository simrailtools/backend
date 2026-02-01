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

package tools.simrail.backend.api.point;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import me.xdrop.fuzzywuzzy.algorithms.BasicAlgorithm;
import me.xdrop.fuzzywuzzy.algorithms.WeightedRatio;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import tools.simrail.backend.api.pagination.PaginatedResponseDto;
import tools.simrail.backend.api.point.dto.PointInfoDto;
import tools.simrail.backend.api.point.dto.PointInfoDtoConverter;
import tools.simrail.backend.common.point.SimRailPoint;
import tools.simrail.backend.common.point.SimRailPointProvider;
import tools.simrail.backend.common.signal.PlatformSignalProvider;
import tools.simrail.backend.common.util.GeoUtil;

@Service
class SimRailPointService {

  // ratio for comparing names with search queries
  private static final BasicAlgorithm WEIGHTED_RATIO = new WeightedRatio().noProcessor();
  private static final Comparator<SimRailPoint> POINT_BY_ID_COMPARATOR =
    Comparator.comparing(SimRailPoint::getId);
  private static final Comparator<Map.Entry<SimRailPoint, Integer>> SEARCH_RESULT_COMPARATOR_ASC =
    Comparator.comparingInt(Map.Entry::getValue);
  private static final Comparator<Map.Entry<SimRailPoint, Integer>> SEARCH_RESULT_COMPARATOR_DESC =
    Collections.reverseOrder(SEARCH_RESULT_COMPARATOR_ASC);

  private final SimRailPointProvider pointProvider;
  private final PointInfoDtoConverter pointInfoConverter;
  private final PlatformSignalProvider platformSignalProvider;

  @Autowired
  public SimRailPointService(
    @NonNull SimRailPointProvider pointProvider,
    @NonNull PointInfoDtoConverter pointInfoConverter,
    @NonNull PlatformSignalProvider platformSignalProvider
  ) {
    this.pointProvider = pointProvider;
    this.pointInfoConverter = pointInfoConverter;
    this.platformSignalProvider = platformSignalProvider;
  }

  /**
   * Normalizes the given input string for search processing.
   *
   * @param input the string to normalize.
   * @return the normalized version of the given input string.
   */
  private static @NonNull String normalizeInputForSearch(@NonNull String input) {
    return input.replace(" ", "").toLowerCase(Locale.ROOT);
  }

  /**
   * Finds the point with the given id, either by looking it up or from cache.
   *
   * @param id the id of the point to get.
   * @return an optional holding the point with the given id, if one exists.
   */
  @Cacheable(cacheNames = "point_cache", key = "'by_id_' + #id")
  public @NonNull Optional<PointInfoDto> findPointById(@NonNull UUID id) {
    return this.pointProvider.findPointByIntId(id).map(point -> {
      var platformSignals = this.platformSignalProvider.findSignalsByPoint(point.getId());
      return this.pointInfoConverter.apply(point, platformSignals);
    });
  }

  /**
   * Finds the point with the given SimRail id, either by looking it up or from cache.
   *
   * @param id the SimRail id of the point to get.
   * @return an optional holding the point with the given id, if one exists.
   */
  @Cacheable(cacheNames = "point_cache", key = "'by_point_id' + #id")
  public @NonNull Optional<PointInfoDto> findPointByPointId(@NonNull String id) {
    return this.pointProvider.findPointByPointId(id).map(point -> {
      var platformSignals = this.platformSignalProvider.findSignalsByPoint(point.getId());
      return this.pointInfoConverter.apply(point, platformSignals);
    });
  }

  /**
   * Lists all points that are registered according to the given filter and paging parameters.
   *
   * @param countries the countries in which the points to return may be located.
   * @param page      the page of elements to return, defaults to 1.
   * @param limit     the maximum elements to return per page, defaults to 20.
   * @return a paginated response containing the points that are matching the given filter and paging parameters.
   */
  @Cacheable(cacheNames = "point_cache", key = "'list_' + #countries + #page + #limit")
  public @NonNull PaginatedResponseDto<PointInfoDto> findPointsByCountry(
    @Nullable List<String> countries,
    @Nullable Integer page,
    @Nullable Integer limit
  ) {
    // build the pagination parameter
    int indexedPage = Objects.requireNonNullElse(page, 1) - 1;
    int requestedLimit = Objects.requireNonNullElse(limit, 20);
    int itemsToSkip = requestedLimit * indexedPage;

    // filter out the items according the search parameter input
    var items = this.pointProvider.getPoints().stream()
      .sorted(POINT_BY_ID_COMPARATOR)
      .filter(point -> countries == null || countries.isEmpty() || countries.contains(point.getCountry()))
      .skip(itemsToSkip)
      .limit(requestedLimit + 1)
      .map(point -> {
        var platformSignals = this.platformSignalProvider.findSignalsByPoint(point.getId());
        return this.pointInfoConverter.apply(point, platformSignals);
      })
      .toList();
    var moreItems = items.size() > requestedLimit;
    if (moreItems) {
      // more items are available than requested that are matching the requested filter, therefore
      // the result list must be bigger than the requested limit so we have to trim it to the expected size
      var responseItems = items.subList(0, requestedLimit);
      return new PaginatedResponseDto<>(responseItems, true);
    } else {
      // there are not more items available that are matching the given filter
      return new PaginatedResponseDto<>(items, false);
    }
  }

  /**
   * Finds points by their name using a distance ratio. The result is ordered DESC (highest match first).
   *
   * @param searchQuery the search query to search for in the point name.
   * @param countries   the countries in which the points to return may be located.
   * @param limit       the maximum results to return.
   * @return the points whose name is matching the given input search query, in descending order.
   */
  @Cacheable(cacheNames = "point_cache", key = "'by_name' + #searchQuery + #countries + #limit")
  public @NonNull List<PointInfoDto> findPointsByName(
    @NonNull String searchQuery,
    @Nullable List<String> countries,
    int limit
  ) {
    var normalizedSearchTerm = normalizeInputForSearch(searchQuery);
    return this.pointProvider.getPoints().stream()
      .filter(point -> countries == null || countries.isEmpty() || countries.contains(point.getCountry()))
      .map(point -> {
        var normalizedPointName = normalizeInputForSearch(point.getName());
        var similarityScore = WEIGHTED_RATIO.apply(normalizedPointName, normalizedSearchTerm);
        return Map.entry(point, similarityScore);
      })
      .filter(entry -> entry.getValue() > 50)
      .sorted(SEARCH_RESULT_COMPARATOR_DESC)
      .limit(limit)
      .map(Map.Entry::getKey)
      .map(point -> {
        var platformSignals = this.platformSignalProvider.findSignalsByPoint(point.getId());
        return this.pointInfoConverter.apply(point, platformSignals);
      })
      .toList();
  }

  /**
   * Finds points that are located in the given radius around the given geographical position. The result is ordered ASC
   * (closest points first).
   *
   * @param latitude       the latitude from which to search in the given search radius.
   * @param longitude      the longitude from which to search in the given search radius.
   * @param radiusInMeters the radius around the given geo position to search.
   * @param countries      the countries in which the points to return may be located.
   * @param limit          the maximum results to return.
   * @return the points that are located in the given radius around the given geo position, in ascending order.
   */
  @Cacheable(cacheNames = "point_cache", key = "'by_pos_' + #latitude + #longitude + #radiusInMeters + #countries + #limit")
  public @NonNull List<PointInfoDto> findPointsAroundPosition(
    double latitude,
    double longitude,
    int radiusInMeters,
    @Nullable List<String> countries,
    int limit
  ) {
    return this.pointProvider.getPoints().stream()
      .filter(point -> countries == null || countries.isEmpty() || countries.contains(point.getCountry()))
      .map(point -> {
        var pointPos = point.getPosition();
        var distanceInMeters = GeoUtil.calculateHaversineDistance(
          latitude,
          longitude,
          pointPos.getLatitude(),
          pointPos.getLongitude());
        var distanceMaxed = Math.min(distanceInMeters, Integer.MAX_VALUE);
        return Map.entry(point, (int) Math.round(distanceMaxed));
      })
      .filter(entry -> entry.getValue() <= radiusInMeters)
      .sorted(SEARCH_RESULT_COMPARATOR_ASC)
      .limit(limit)
      .map(Map.Entry::getKey)
      .map(point -> {
        var platformSignals = this.platformSignalProvider.findSignalsByPoint(point.getId());
        return this.pointInfoConverter.apply(point, platformSignals);
      })
      .toList();
  }
}
