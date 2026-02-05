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

package tools.simrail.backend.api.dispatchpost;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import tools.simrail.backend.api.dispatchpost.dto.DispatchPostInfoDto;
import tools.simrail.backend.api.dispatchpost.dto.DispatchPostInfoDtoConverter;
import tools.simrail.backend.api.pagination.PaginatedResponseDto;

@Service
class DispatchPostService {

  private final ApiDispatchPostRepository dispatchPostRepository;
  private final DispatchPostInfoDtoConverter dispatchPostInfoConverter;

  @Autowired
  DispatchPostService(
    @NonNull ApiDispatchPostRepository dispatchPostRepository,
    @NonNull DispatchPostInfoDtoConverter dispatchPostInfoConverter
  ) {
    this.dispatchPostRepository = dispatchPostRepository;
    this.dispatchPostInfoConverter = dispatchPostInfoConverter;
  }

  /**
   * Finds the information about a single dispatch post, either by lookup or from cache.
   *
   * @param id the id of the dispatch post to get.
   * @return an optional holding the dispatch post to get, if one with the given id exists.
   */
  @Cacheable(cacheNames = "dispatch_post_cache", key = "'by_id_' + #id")
  public @NonNull Optional<DispatchPostInfoDto> findById(@NonNull UUID id) {
    return this.dispatchPostRepository.findById(id).map(this.dispatchPostInfoConverter);
  }

  /**
   * Finds dispatch posts that are matching the given filter parameters, either by lookup or from cache.
   *
   * @param serverId   the id of the server on which the dispatch posts to return should be located.
   * @param difficulty the difficulty of the dispatch posts to return.
   * @param pointId    the point where with which the dispatch post is associated.
   * @param deleted    if the dispatch post should be deleted (no longer registered).
   * @param limit      the maximum items to return in the response.
   * @param page       the page of items to return in the response.
   * @return a paginated response wrapper around the items that are matching the filter parameters.
   */
  @Cacheable(cacheNames = "dispatch_post_cache")
  public @NonNull PaginatedResponseDto<DispatchPostInfoDto> find(
    @Nullable UUID serverId,
    @Nullable Integer difficulty,
    @Nullable UUID pointId,
    @Nullable Boolean deleted,
    @Nullable Integer limit,
    @Nullable Integer page
  ) {
    // build the pagination parameter
    int indexedPage = Objects.requireNonNullElse(page, 1) - 1;
    int requestedLimit = Objects.requireNonNullElse(limit, 20);
    int offset = requestedLimit * indexedPage;

    // request one more item than requested for pagination and build the response based on the query result
    var items = this.dispatchPostRepository.find(
      serverId,
      difficulty,
      pointId,
      deleted,
      requestedLimit + 1,
      offset);
    var convertedItems = items.stream()
      .limit(requestedLimit)
      .map(this.dispatchPostInfoConverter)
      .toList();
    return new PaginatedResponseDto<>(convertedItems, items.size() > requestedLimit);
  }
}
