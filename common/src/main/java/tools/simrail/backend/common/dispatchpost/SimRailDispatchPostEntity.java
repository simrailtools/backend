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
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import tools.simrail.backend.common.shared.GeoPositionEntity;

/**
 * The entity that holds all information about a single dispatch post registered in the SimRail api.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity(name = "sr_dispatch_post")
public final class SimRailDispatchPostEntity {

  /**
   * The namespace used to generate UUIDv5 ids for dispatch post entities.
   */
  public static final UUID ID_NAMESPACE = UUID.fromString("07b68676-9816-48ef-bd8a-cf15e3f38f4e");

  /**
   * The unique identifier of the dispatch post.
   */
  @Id
  @Column(name = "id")
  private UUID id;
  /**
   * The id of the point that is associated with the station.
   */
  @Column(name = "point_id")
  private UUID pointId;
  /**
   * The foreign (mongo identifier) of the dispatch post provided by the SimRail api.
   */
  @Column(name = "foreign_id")
  private String foreignId;

  /**
   * The timestamp when this dispatch post was last updated.
   */
  @UpdateTimestamp
  @Column(name = "update_time")
  private Instant updateTime;
  /**
   * The time when the dispatch post was initially registered in the SimRail backend.
   */
  @Column(name = "registered_since")
  private Instant registeredSince;

  /**
   * The name of the dispatch post.
   */
  @Column(name = "name")
  private String name;
  /**
   * The server code on which this dispatch post is located.
   */
  @Column(name = "server_id")
  private UUID serverId;
  /**
   * If the dispatch post is no longer registered in the SimRail backend.
   */
  @Column(name = "deleted")
  private boolean deleted;
  /**
   * The difficulty level of the station.
   */
  @Column(name = "difficulty_level")
  private int difficultyLevel;
  /**
   * The geo position where the dispatch post building is located.
   */
  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "latitude", column = @Column(name = "pos_latitude")),
    @AttributeOverride(name = "longitude", column = @Column(name = "pos_longitude"))
  })
  private GeoPositionEntity position;
  /**
   * The urls to images related to the dispatch post.
   */
  @Column(name = "image_urls")
  @JdbcTypeCode(SqlTypes.JSON)
  private Set<String> imageUrls;

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
