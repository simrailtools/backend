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

package tools.simrail.backend.common.dispatchpost;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import tools.simrail.backend.common.shared.GeoPositionEntity;

/**
 * The entity that holds all information about a single dispatch post registered in the SimRail api.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity(name = "sr_dispatch_post")
@Table(indexes = {
  @Index(columnList = "pointId"),
  @Index(columnList = "foreignId"),
  @Index(columnList = "serverId, difficultyLevel, pointId, deleted, registeredSince, id"),
})
public final class SimRailDispatchPostEntity {

  /**
   * The namespace used to generate UUIDv5 ids for dispatch post entities.
   */
  public static final UUID ID_NAMESPACE = UUID.fromString("07b68676-9816-48ef-bd8a-cf15e3f38f4e");

  /**
   * The unique identifier of the dispatch post.
   */
  @Id
  @Nonnull
  private UUID id;
  /**
   * The id of the point that is associated with the station.
   */
  @Nonnull
  @Column(nullable = false)
  private UUID pointId;
  /**
   * The foreign (mongo identifier) of the dispatch post provided by the SimRail api.
   */
  @Nonnull
  @Column(nullable = false, updatable = false, length = 24)
  private String foreignId;

  /**
   * The timestamp when this dispatch post was last updated.
   */
  @Nonnull
  @UpdateTimestamp
  @Column(nullable = false)
  private OffsetDateTime updateTime;
  /**
   * The time when the dispatch post was initially registered in the SimRail backend.
   */
  @Nonnull
  @Column(nullable = false)
  private OffsetDateTime registeredSince;

  /**
   * The name of the dispatch post.
   */
  @Nonnull
  @Column(nullable = false)
  private String name;
  /**
   * The server code on which this dispatch post is located.
   */
  @Nonnull
  @Column(nullable = false, updatable = false)
  private UUID serverId;
  /**
   * If the dispatch post is no longer registered in the SimRail backend.
   */
  @Column
  private boolean deleted;
  /**
   * The difficulty level of the station.
   */
  @Column
  private int difficultyLevel;
  /**
   * The geo position where the dispatch post building is located.
   */
  @Nonnull
  @Embedded
  private GeoPositionEntity position;
  /**
   * The urls to images related to the dispatch post.
   */
  @Nonnull
  @Column(nullable = false)
  @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
  private Set<String> imageUrls;
  /**
   * The steam ids of the player(s) that are currently dispatching the station. Empty if the station is dispatched
   * automatically.
   */
  // impl note: this is a set as there might be multiple dispatchers for a station coming in the future,
  //            confirmed in a recent blog post, plus the api returns an array the dispatchers anyway
  @Nonnull
  @OrderColumn
  @Column(nullable = false)
  @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
  private Set<String> dispatcherSteamIds;

  /**
   * Internal marker to indicate if the post entity was newly created when being collected.
   */
  @Transient
  private boolean isNew;

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SimRailDispatchPostEntity entity)) {
      return false;
    }
    return Objects.equals(this.id, entity.getId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(this.id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nonnull String toString() {
    return "DispatchPostEntity{id=" + this.id + ", name=" + this.name + "}";
  }
}
