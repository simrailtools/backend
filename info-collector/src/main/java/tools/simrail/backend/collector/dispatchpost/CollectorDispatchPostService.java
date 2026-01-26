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

package tools.simrail.backend.collector.dispatchpost;

import jakarta.transaction.Transactional;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tools.simrail.backend.collector.server.SimRailServerDescriptor;
import tools.simrail.backend.common.dispatchpost.SimRailDispatchPostEntity;
import tools.simrail.backend.common.dispatchpost.SimRailDispatchPostRepository;
import tools.simrail.backend.common.point.SimRailPointProvider;
import tools.simrail.backend.common.shared.GeoPositionEntity;
import tools.simrail.backend.common.util.MongoIdDecodeUtil;
import tools.simrail.backend.external.srpanel.model.SimRailPanelDispatchPost;

@Service
class CollectorDispatchPostService {

  private static final Map<String, GeoPositionEntity> OVERRIDDEN_STATION_POSITIONS_BY_FOREIGN_ID = Map.of(
    "675330d44337b38ac4027545", new GeoPositionEntity(50.354694, 20.011680) // Miechów
  );

  private final SimRailPointProvider pointProvider;
  private final SimRailDispatchPostRepository dispatchPostRepository;

  @Autowired
  public CollectorDispatchPostService(
    @NonNull SimRailPointProvider pointProvider,
    @NonNull SimRailDispatchPostRepository dispatchPostRepository
  ) {
    this.pointProvider = pointProvider;
    this.dispatchPostRepository = dispatchPostRepository;
  }

  @Transactional
  public void saveDispatchPost(
    @NonNull SimRailServerDescriptor server,
    @NonNull SimRailPanelDispatchPost dispatchPost,
    @NonNull DispatchPostUpdateHolder updateHolder
  ) {
    // ensure that a point exists for the dispatch post
    var point = this.pointProvider.findPointByName(dispatchPost.getStationName()).orElse(null);
    if (point == null) {
      return;
    }

    // get the position of the dispatch post, either an overridden one or the one reported by the api
    var position = OVERRIDDEN_STATION_POSITIONS_BY_FOREIGN_ID.get(dispatchPost.getId());
    if (position == null) {
      position = new GeoPositionEntity(dispatchPost.getPositionLatitude(), dispatchPost.getPositionLongitude());
    }

    var postEntity = new SimRailDispatchPostEntity();
    postEntity.setId(updateHolder.id);
    postEntity.setPointId(point.getId());
    postEntity.setForeignId(updateHolder.foreignId);
    postEntity.setServerId(server.id());
    postEntity.setName(dispatchPost.getStationName());
    postEntity.setDifficultyLevel(dispatchPost.getDifficulty());
    postEntity.setPosition(position);
    postEntity.setDeleted(false);

    var registeredSince = MongoIdDecodeUtil.parseMongoId(dispatchPost.getId());
    postEntity.setRegisteredSince(registeredSince);

    var imageUrls = Stream.of(dispatchPost.getImage1Url(), dispatchPost.getImage2Url(), dispatchPost.getImage3Url())
      .filter(Objects::nonNull)
      .collect(Collectors.toSet());
    postEntity.setImageUrls(imageUrls);

    this.dispatchPostRepository.save(postEntity);
  }

  @Transactional
  public void markUncontainedDispatchPostsAsDeleted(@NonNull UUID server, @NonNull Set<String> foreignPostIds) {
    var posts = this.dispatchPostRepository.findAllByServerIdAndForeignIdNotInAndDeletedIsFalse(server, foreignPostIds);
    posts.forEach(post -> post.setDeleted(true));
    this.dispatchPostRepository.saveAll(posts);
  }
}
