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

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Persistable;

/**
 * A single event along the route of a journey.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "sit_journey_event")
public final class JourneyEventEntity implements Persistable<UUID> {

  /**
   * The namespace used to generate UUIDv5 ids for event entities.
   */
  public static final UUID ID_NAMESPACE = UUID.fromString("e869adba-bca7-485f-8c0c-edc61582b4f4");

  /**
   * The id of this event.
   */
  @Id
  @Column(name = "id")
  private UUID id;
  /**
   * The id of the journey which is related to this event.
   */
  @JsonIgnore // JSON is used to create a checksum, but JourneyEvent -> Journey -> Set<JourneyEvent> is a circular ref
  @JoinColumn(name = "journey_id")
  @ManyToOne(fetch = FetchType.LAZY)
  private JourneyEntity journey;
  /**
   * The index of this event along the journey route.
   */
  @Column(name = "event_index")
  private int eventIndex;

  /**
   * The time when this event was created.
   */
  @CreationTimestamp
  @Column(name = "created_at")
  private Instant createdAt;
  /**
   * The type of this event.
   */
  @Column(name = "event_type")
  @Enumerated(EnumType.STRING)
  private JourneyEventType eventType;

  /**
   * The id of the point where the event is scheduled to happen.
   */
  @Column(name = "point_id")
  private UUID pointId;
  /**
   * Indicates if the event is within the playable map bounds at the time of collection.
   */
  @Column(name = "in_playable_border")
  private boolean inPlayableBorder;

  /**
   * The time when this event was scheduled to happen.
   */
  @Column(name = "scheduled_time")
  private LocalDateTime scheduledTime;
  /**
   * The actual time when the journey reached this event.
   */
  @Column(name = "realtime_time")
  private LocalDateTime realtimeTime;
  /**
   * Information about the precision of the current realtime time.
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "realtime_time_type")
  private JourneyTimeType realtimeTimeType;

  /**
   * Get the transport that is used for the journey at this event.
   */
  @Column
  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "line", column = @Column(name = "transport_line")),
    @AttributeOverride(name = "type", column = @Column(name = "transport_type")),
    @AttributeOverride(name = "label", column = @Column(name = "transport_label")),
    @AttributeOverride(name = "number", column = @Column(name = "transport_number")),
    @AttributeOverride(name = "category", column = @Column(name = "transport_category")),
    @AttributeOverride(name = "maxSpeed", column = @Column(name = "transport_max_speed")),
  })
  private JourneyTransport transport;

  /**
   * The type of stop that is scheduled at this event.
   */
  @Column(name = "stop_type")
  @Enumerated(EnumType.STRING)
  private JourneyStopType stopType;
  /**
   * The information about the platform where the journey is scheduled to stop. Only present if the journey is scheduled
   * to stop with a passenger change at the stop.
   */
  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "track", column = @Column(name = "scheduled_track")),
    @AttributeOverride(name = "platform", column = @Column(name = "scheduled_platform")),
  })
  private JourneyPassengerStopInfo scheduledPassengerStopInfo;
  /**
   * The information about the platform where the journey actually stopped. Only present if the journey is scheduled to
   * stop with a passenger change at the stop and the journey already arrived at the station and stopped at a platform.
   */
  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "track", column = @Column(name = "realtime_track")),
    @AttributeOverride(name = "platform", column = @Column(name = "realtime_platform")),
  })
  private JourneyPassengerStopInfo realtimePassengerStopInfo;

  /**
   * Indicates if this event was canceled.
   */
  @Column(name = "cancelled")
  private boolean cancelled;
  /**
   * Indicates if this event was added to the schedule due to a route change.
   */
  @Column(name = "additional")
  private boolean additional;

  /**
   * Indicates if this journey event was newly created or already existed in the database.
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
    if (!(o instanceof JourneyEventEntity entity)) {
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
  public @NonNull String toString() {
    return "JourneyEvent{id=" + this.id + "}";
  }
}
