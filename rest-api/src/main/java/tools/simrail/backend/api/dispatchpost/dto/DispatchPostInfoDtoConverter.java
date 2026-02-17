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

package tools.simrail.backend.api.dispatchpost.dto;

import java.util.function.Function;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import tools.simrail.backend.api.shared.GeoPositionDtoConverter;
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
  private final GeoPositionDtoConverter geoPositionDtoConverter;
  private final DataCache<EventBusProto.DispatchPostUpdateFrame> dispatchPostDataCache;

  @Autowired
  public DispatchPostInfoDtoConverter(
    @NonNull UserDtoConverter userDtoConverter,
    @NonNull GeoPositionDtoConverter geoPositionDtoConverter,
    @NonNull @Qualifier("dispatch_post_cache") DataCache<EventBusProto.DispatchPostUpdateFrame> dispatchPostDataCache
  ) {
    this.userDtoConverter = userDtoConverter;
    this.geoPositionDtoConverter = geoPositionDtoConverter;
    this.dispatchPostDataCache = dispatchPostDataCache;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @NonNull DispatchPostInfoDto apply(@NonNull SimRailDispatchPostEntity entity) {
    var position = this.geoPositionDtoConverter.convert(entity.getPosition());
    var cachedRtData = this.dispatchPostDataCache.findByPrimaryKey(entity.getId().toString());
    var realtimeDataDto = this.convertRtData(cachedRtData);
    return new DispatchPostInfoDto(
      entity.getId(),
      entity.getName(),
      entity.getPointId(),
      entity.getServerId(),
      entity.getUpdateTime(),
      entity.getRegisteredSince(),
      position,
      entity.getImageUrls(),
      entity.getDifficultyLevel(),
      realtimeDataDto,
      entity.isDeleted());
  }

  /**
   * Converts the cached realtime data of a journey into a DTO.
   */
  private @NonNull DispatchPostRealtimeDataDto convertRtData(EventBusProto.@Nullable DispatchPostUpdateFrame data) {
    if (data == null) {
      return new DispatchPostRealtimeDataDto(null);
    }

    var postData = data.getDispatchPostData();
    if (!postData.hasDispatcher()) {
      return new DispatchPostRealtimeDataDto(null);
    }

    var dispatcher = this.userDtoConverter.apply(postData.getDispatcher());
    return new DispatchPostRealtimeDataDto(dispatcher);
  }
}
