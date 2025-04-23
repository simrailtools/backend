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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import org.locationtech.jts.geom.Coordinate;
import tools.simrail.backend.common.util.GeoUtil;

/**
 * Deserializes a polygon from a list of points (in format Double[][]).
 */
final class SimRailPointBBDeserializer extends JsonDeserializer<OptimizedBoundingBox> {

  @Override
  public @Nonnull OptimizedBoundingBox deserialize(
    @Nonnull JsonParser parser,
    @Nonnull DeserializationContext context
  ) throws IOException {
    var polyInput = (ArrayNode) parser.readValueAsTree();
    var polyInputCount = polyInput.size();
    var polyCoordinates = new Coordinate[polyInputCount];
    for (var index = 0; index < polyInputCount; index++) {
      var pointNode = (ArrayNode) polyInput.get(index);
      var longitudeNode = (NumericNode) pointNode.get(0);
      var latitudeNode = (NumericNode) pointNode.get(1);
      polyCoordinates[index] = new Coordinate(longitudeNode.doubleValue(), latitudeNode.doubleValue());
    }

    var linearRing = GeoUtil.GEOMETRY_FACTORY.createLinearRing(polyCoordinates);
    var polygon = GeoUtil.GEOMETRY_FACTORY.createPolygon(linearRing);
    return new OptimizedBoundingBox(polygon);
  }
}
