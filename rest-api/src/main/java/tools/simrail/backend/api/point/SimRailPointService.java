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

package tools.simrail.backend.api.point;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import me.xdrop.fuzzywuzzy.Applicable;
import me.xdrop.fuzzywuzzy.algorithms.WeightedRatio;
import org.springframework.beans.factory.annotation.Autowired;
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
  private static final Applicable WEIGHTED_RATIO = new WeightedRatio().noProcessor();
  private static final Comparator<SimRailPoint> POINT_BY_ID_COMPARATOR =
    Comparator.comparing(SimRailPoint::getId);
  private static final Comparator<Map.Entry<SimRailPoint, Integer>> SEARCH_RESULT_COMPARATOR =
    Comparator.comparingInt(Map.Entry::getValue);

  private final SimRailPointProvider pointProvider;
  private final PointInfoDtoConverter pointInfoConverter;
  private final PlatformSignalProvider platformSignalProvider;

  @Autowired
  public SimRailPointService(
    @Nonnull SimRailPointProvider pointProvider,
    @Nonnull PointInfoDtoConverter pointInfoConverter,
    @Nonnull PlatformSignalProvider platformSignalProvider
  ) {
    this.pointProvider = pointProvider;
    this.pointInfoConverter = pointInfoConverter;
    this.platformSignalProvider = platformSignalProvider;
  }

  /**
   * @param input
   * @return
   */
  private static @Nonnull String normalizeInputForSearch(@Nonnull String input) {
    return input.replace(" ", "").toLowerCase(Locale.ROOT);
  }

  /**
   * @param id
   * @return
   */
  // @Cacheable
  public @Nonnull Optional<PointInfoDto> findPointById(@Nonnull UUID id) {
    return this.pointProvider.findPointByIntId(id).map(point -> {
      var platformSignals = this.platformSignalProvider.findSignalsByPoint(point.getId());
      return this.pointInfoConverter.apply(point, platformSignals);
    });
  }

  /**
   * @param id
   * @return
   */
  public @Nonnull Optional<PointInfoDto> findPointByPointId(@Nonnull String id) {
    return this.pointProvider.findPointByPointId(id).map(point -> {
      var platformSignals = this.platformSignalProvider.findSignalsByPoint(point.getId());
      return this.pointInfoConverter.apply(point, platformSignals);
    });
  }

  /**
   * @param countries
   * @param page
   * @param limit
   * @return
   */
  public @Nonnull PaginatedResponseDto<PointInfoDto> findPointsByCountry(
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
   * @param searchQuery
   * @param countries
   * @param limit
   * @return
   */
  public @Nonnull List<PointInfoDto> findPointsByName(
    @Nonnull String searchQuery,
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
      .sorted(SEARCH_RESULT_COMPARATOR)
      .limit(limit)
      .map(Map.Entry::getKey)
      .map(point -> {
        var platformSignals = this.platformSignalProvider.findSignalsByPoint(point.getId());
        return this.pointInfoConverter.apply(point, platformSignals);
      })
      .toList();
  }

  /**
   * @param latitude
   * @param longitude
   * @param radiusInMeters
   * @param countries
   * @param limit
   * @return
   */
  public @Nonnull List<PointInfoDto> findPointsAroundPosition(
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
      .sorted(SEARCH_RESULT_COMPARATOR)
      .limit(limit)
      .map(Map.Entry::getKey)
      .map(point -> {
        var platformSignals = this.platformSignalProvider.findSignalsByPoint(point.getId());
        return this.pointInfoConverter.apply(point, platformSignals);
      })
      .toList();
  }
}
