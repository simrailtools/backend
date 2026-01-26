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

import java.util.Comparator;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class JourneyVehicle {

  /**
   * Comparator to compare the index of the vehicle in the sequence, ascending.
   */
  public static final Comparator<JourneyVehicle> BY_SEQUENCE_INDEX_COMPARATOR
    = Comparator.comparingInt(JourneyVehicle::getIndexInSequence);

  /**
   * The index where this vehicle is located in the vehicle sequence.
   */
  private int indexInSequence;
  /**
   * The id of the railcar that is associated with this vehicle.
   */
  @NonNull
  private UUID railcarId;

  /**
   * The weight of the load, null if no load is provided for the vehicle.
   */
  @Nullable
  private Integer loadWeight;
  /**
   * The load of the vehicle, null if no load is provided for the vehicle.
   */
  @Nullable
  private JourneyVehicleLoad load;
}
