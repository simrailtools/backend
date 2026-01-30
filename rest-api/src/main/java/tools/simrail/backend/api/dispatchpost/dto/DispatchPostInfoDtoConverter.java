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

package tools.simrail.backend.api.dispatchpost.dto;

import java.util.function.Function;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.simrail.backend.api.shared.GeoPositionDto;
import tools.simrail.backend.api.shared.UserDtoConverter;
import tools.simrail.backend.common.cache.DataCache;
import tools.simrail.backend.common.dispatchpost.SimRailDispatchPostEntity;
import tools.simrail.backend.common.proto.EventBusProto;

/**
 * Converter for dispatch post entities to DTOs.
 */
@Component
public final class DispatchPostInfoDtoConverter implements Function<SimRailDispatchPostEntity, DispatchPostInfoDto> {

  private final UserDtoConverter userDtoConverter;
  private final DataCache<EventBusProto.DispatchPostUpdateFrame> dispatchPostDataCache;

  @Autowired
  public DispatchPostInfoDtoConverter(
    @NonNull UserDtoConverter userDtoConverter,
    @NonNull DataCache<EventBusProto.DispatchPostUpdateFrame> dispatchPostDataCache
  ) {
    this.userDtoConverter = userDtoConverter;
    this.dispatchPostDataCache = dispatchPostDataCache;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DispatchPostInfoDto apply(@NonNull SimRailDispatchPostEntity entity) {
    var cachedRtData = this.dispatchPostDataCache.findByPrimaryKey(entity.getId().toString());
    var dispatchPostData = cachedRtData == null ? null : cachedRtData.getDispatchPostData();
    var dispatcher = dispatchPostData == null || !dispatchPostData.hasDispatcher()
      ? null
      : this.userDtoConverter.apply(dispatchPostData.getDispatcher());

    var position = entity.getPosition();
    return new DispatchPostInfoDto(
      entity.getId(),
      entity.getName(),
      entity.getPointId(),
      entity.getServerId(),
      entity.getUpdateTime(),
      entity.getRegisteredSince(),
      new GeoPositionDto(position.getLatitude(), position.getLongitude()),
      entity.getImageUrls(),
      entity.getDifficultyLevel(),
      new DispatchPostRealtimeDataDto(dispatcher),
      entity.isDeleted());
  }
}
