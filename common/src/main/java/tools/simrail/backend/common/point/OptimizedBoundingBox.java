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

package tools.simrail.backend.common.point;

import jakarta.annotation.Nonnull;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.predicate.RectangleContains;
import org.locationtech.jts.operation.relate.RelateOp;

/**
 * An optimized bounding box implementation which removes checks that are not necessary for our use case.
 */
final class OptimizedBoundingBox {

  private final boolean rectangle;
  private final Polygon boundingBox;
  private final Envelope boxEnvelope;

  /**
   * Creates a new optimized bounding box implementation based on the given polygon.
   *
   * @param boundingBox the polygon to base this bounding box on.
   */
  public OptimizedBoundingBox(@Nonnull Polygon boundingBox) {
    this.boundingBox = boundingBox;
    this.rectangle = boundingBox.isRectangle();
    this.boxEnvelope = boundingBox.getEnvelopeInternal();
  }

  /**
   * Checks if this bounding box contains the given point.
   *
   * @param other the point to check.
   * @return true if this bounding box contains the given point, false otherwise.
   */
  public boolean contains(@Nonnull Point other) {
    // optimization for rectangle
    if (this.rectangle) {
      return RectangleContains.contains(this.boundingBox, other);
    }

    // optimization - envelope test
    var envelopeContains = this.boxEnvelope.contains(other.getEnvelopeInternal());
    return envelopeContains && RelateOp.relate(this.boundingBox, other).isContains();
  }
}
