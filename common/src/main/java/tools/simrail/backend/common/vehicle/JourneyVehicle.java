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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A vehicle entry (one wagon or locomotive for journey).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity(name = "sit_vehicle")
@Table(indexes = {
  @Index(columnList = "journeyId"),
  @Index(columnList = "railcarId"),
  @Index(columnList = "journeyId, status"),
  @Index(columnList = "journeyId, indexInGroup"),
})
public final class JourneyVehicle {

  /**
   * The id of the specific vehicle.
   */
  @Id
  @GeneratedValue
  private Long id;
  /**
   * The time and date when this vehicle information was last updated.
   */
  @UpdateTimestamp
  private OffsetDateTime updateTime;

  /**
   * The id of the journey to which this vehicle entry belongs.
   */
  @Column(nullable = false)
  private UUID journeyId;
  /**
   * The index where this vehicle is loaded in the vehicle group.
   */
  @Column
  private int indexInGroup;
  /**
   * The status of the vehicle information.
   */
  @Column(nullable = false)
  private JourneyVehicleStatus status;

  /**
   * The id of the used railcar in the group, can only be null if the status is unknown.
   */
  @Column
  private UUID railcarId;
  /**
   * The weight of the load, null if no load is provided for the vehicle.
   */
  @Column
  private Integer loadWeight;
  /**
   * The load of the vehicle, null if no load is provided for the vehicle.
   */
  @Column
  @Enumerated(EnumType.STRING)
  private JourneyVehicleLoad load;
}
