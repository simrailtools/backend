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

package tools.simrail.backend.common.journey;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A single event along the route of a journey.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "sit_journey_event")
@Table(indexes = {
  @Index(columnList = "journeyId"),
})
public final class JourneyEventEntity {

  /**
   * The namespace used to generate UUIDv5 ids for event entities.
   */
  public static final UUID ID_NAMESPACE = UUID.fromString("e869adba-bca7-485f-8c0c-edc61582b4f4");

  /**
   * The id of this event.
   */
  @Id
  private UUID id;
  /**
   * The id of the journey which is related to this event.
   */
  @Column(nullable = false)
  private UUID journeyId;
  /**
   * The time when this event was created.
   */
  @CreationTimestamp
  private OffsetDateTime createdAt;
  /**
   * The type of this event.
   */
  @Column(nullable = false)
  private JourneyEventType eventType;
  /**
   * The index of this event along the journey route.
   */
  @Column
  private int eventIndex;
  /**
   * The descriptor of the stop where the event is scheduled to happen.
   */
  @Embedded
  private JourneyStopDescriptor stopDescriptor;

  /**
   * The time when this event was scheduled to happen.
   */
  @Column(nullable = false)
  private OffsetDateTime scheduledTime;
  /**
   * The current best realtime time information providable for this event.
   */
  @Column(nullable = false)
  private OffsetDateTime realtimeTime;
  /**
   * Information about the precision of the current realtime time.
   */
  @Column(nullable = false)
  private JourneyTimeType realtimeTimeType;

  /**
   * The type of stop that is scheduled at this event.
   */
  @Column(nullable = false)
  private JourneyStopType stopType;
  /**
   * The information about the platform where the journey is scheduled to stop. Only present if the journey is scheduled
   * to stop with a passenger change at the stop.
   */
  @Column
  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "track", column = @Column(name = "scheduled_track")),
    @AttributeOverride(name = "platform", column = @Column(name = "scheduled_platform"))
  })
  private JourneyPassengerStopInfo scheduledPassengerStopInfo;
  /**
   * The information about the platform where the journey actually stopped. Only present if the journey is scheduled to
   * stop with a passenger change at the stop and the journey already arrived at the station and stopped at a platform.
   */
  @Column
  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "track", column = @Column(name = "realtime_track")),
    @AttributeOverride(name = "platform", column = @Column(name = "realtime_platform"))
  })
  private JourneyPassengerStopInfo realtimePassengerStopInfo;

  /**
   * Get the transport that is used for the journey at this event.
   */
  @Embedded
  private JourneyTransport transport;

  /**
   * Indicates if this event was cancelled.
   */
  @Column
  private boolean cancelled;
  /**
   * Indicates if this event was added to the schedule due to a route change.
   */
  @Column
  private boolean additional;

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
  public @Nonnull String toString() {
    return "JourneyEvent{id=" + this.id + "}";
  }
}
