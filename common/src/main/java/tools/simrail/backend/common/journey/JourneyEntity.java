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

package tools.simrail.backend.common.journey;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;
import tools.simrail.backend.common.shared.GeoPositionEntity;

/**
 * Represents information about the complete journey of a train.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity(name = "sit_journey")
@Table(indexes = {
  @Index(columnList = "serverId"),
  @Index(columnList = "serverId, foreignRunId"),
  @Index(columnList = "firstSeenTime, lastSeenTime"),
  @Index(columnList = "serverId, firstSeenTime, lastSeenTime"),
})
public final class JourneyEntity implements Persistable<UUID> {

  /**
   * The namespace used to generate UUIDv5 ids for journey entities.
   */
  public static final UUID ID_NAMESPACE = UUID.fromString("36b63943-4f28-4f1e-a333-376b39f6022e");

  /**
   * The id of this journey.
   */
  @Id
  private UUID id;
  /**
   * The foreign identifier of the train provided by the SimRail api, this one repeats itself every day.
   */
  @Column
  private String foreignId;
  /**
   * The foreign run id provided by the SimRail api.
   */
  @Column(nullable = false, unique = true)
  private UUID foreignRunId;

  /**
   * The internal id of the server on which the associated journey is happening.
   */
  @Column(nullable = false)
  private UUID serverId;

  /**
   * The last time when this journey was last updated.
   */
  @UpdateTimestamp
  private OffsetDateTime updateTime;
  /**
   * The time when the journey was first seen as active on the associated server.
   */
  @Column
  private OffsetDateTime firstSeenTime;
  /**
   * The time when the journey was last seen as active on the associated server.
   */
  @Column
  private OffsetDateTime lastSeenTime;

  /**
   * Indicates if the journey is cancelled (did not spawn after a fixed period of time).
   */
  private boolean cancelled;
  /**
   * The id of the journey that this train transitions into at the final stop.
   */
  @Column
  private UUID continuationJourneyId;
  /**
   * The events that are along the route of this journey.
   */
  @BatchSize(size = 100)
  @OrderBy("eventIndex")
  @OneToMany(fetch = FetchType.LAZY)
  @JoinColumn(name = "journeyId", insertable = false, updatable = false)
  private List<JourneyEventEntity> events;

  /**
   * The current speed of the train, null if the train is currently not active.
   */
  @Column
  private Integer speed;
  /**
   * The current position of the train, null if the train is currently not active.
   */
  @Column
  @Embedded
  private GeoPositionEntity position;
  /**
   * The signal that is in front of this journey, null if the train is not active or the signal is too far away.
   */
  @Column
  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "name", column = @Column(name = "next_signal_name")),
    @AttributeOverride(name = "distance", column = @Column(name = "next_signal_distance")),
    @AttributeOverride(name = "maxAllowedSpeed", column = @Column(name = "next_signal_max_speed")),
  })
  private JourneySignalInfo nextSignal;
  /**
   * The steam id of the player that currently controls the train, null if the train is currently not active or not
   * driven by a player.
   */
  @Column(length = 20)
  private String driverSteamId;

  /**
   * Indicates if this entity is now or already persisted in the database.
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
    if (!(o instanceof JourneyEntity entity)) {
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
    return "Journey{id=" + this.id + "}";
  }
}
