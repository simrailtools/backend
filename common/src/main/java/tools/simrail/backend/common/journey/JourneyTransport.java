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

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Information about the transport used for a journey.
 */
@Data
@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public final class JourneyTransport {

  /**
   * Category of the transport used for the journey.
   */
  @Column(nullable = false)
  private String category;
  /**
   * Number of the transport used for the journey.
   */
  @Column(nullable = false)
  private String number;
  /**
   * A higher-level category of the transport.
   */
  @Column(nullable = false)
  private JourneyTransportType type;
  /**
   * Line information for repeating transports, null if no line is associated with the transport.
   */
  @Column
  private String line;
  /**
   * Marketing name or product name of the transport.
   */
  @Column
  private String label;
  /**
   * The maximum speed that this transport is allowed to drive at the associated point.
   */
  @Column
  private int maxSpeed;
}
