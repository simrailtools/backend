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

package tools.simrail.backend.common.vehicle;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

/**
 * Holds the predicted or real vehicle sequence for a journey.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity(name = "sit_journey_vehicle_sequence")
public final class JourneyVehicleSequenceEntity {

  /**
   * The id of this journey sequence.
   */
  @Id
  @Column(name = "id")
  @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
  private UUID id;
  /**
   * The id of the journey that this vehicle sequence is associated with.
   */
  @Column(name = "journey_id")
  private UUID journeyId;
  /**
   * Special internal key to resolve this sequence.
   */
  @Column(name = "sequence_resolve_key")
  private String sequenceResolveKey;

  /**
   * The time and date when this vehicle information was last updated.
   */
  @UpdateTimestamp
  @Column(name = "updated_at")
  private Instant updateTime;

  /**
   * The status of the vehicle sequence information.
   */
  @Column(name = "status")
  @Enumerated(EnumType.STRING)
  private JourneyVehicleStatus status;
  /**
   * The vehicles that are in the vehicle sequence.
   */
  @Column(name = "vehicles")
  @JdbcTypeCode(SqlTypes.JSON)
  private Set<JourneyVehicle> vehicles;
}
