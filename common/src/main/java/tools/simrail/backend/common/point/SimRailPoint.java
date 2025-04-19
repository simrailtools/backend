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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single SimRail point information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public final class SimRailPoint {

  /**
   * The name of the point.
   */
  @Nonnull
  @JsonProperty("name")
  private String name;
  /**
   * The internal id of the point.
   */
  @Nonnull
  @JsonProperty("id")
  private UUID id;
  /**
   * The SimRail point ids that are associated with this point.
   */
  @Nonnull
  @JsonProperty("ids")
  private Set<String> simRailPointIds;

  /**
   * The prefix of the point, used for example as a prefix for signal names. Null if the prefix is unknown.
   */
  @Nullable
  @JsonProperty("prefix")
  private String prefix;
  /**
   * The maximum speed of trains at this point.
   */
  @JsonProperty("max_speed")
  private int maxSpeed;
  /**
   * Indicates if the point is a stop place (point to stop without switches) or a full station.
   */
  @JsonProperty("stop_place")
  private boolean stopPlace;
  /**
   * The ISO 3166-1 alpha-3 country code where the point is located.
   */
  @JsonProperty("country")
  private String country;
  /**
   * The OSM node id of the point.
   */
  @JsonProperty("osm_id")
  private long osmNodeId;
  /**
   * The UIC reference of the point, can be null if unknown or if the point has no such ref.
   */
  @Nullable
  @JsonProperty("uic_ref")
  private String uicRef;

  /**
   * The center position (or similar) of the point.
   */
  @JsonProperty("position")
  private SimRailPointPosition position;
  /**
   * The bounding box of the point. Each inner list element is a tuple of the longitude (first element) and the latitude
   * (second element) of one point of the bounding box.
   */
  @JsonProperty("bb")
  @JsonDeserialize(using = SimRailPointBBDeserializer.class)
  private OptimizedBoundingBox boundingBox;
}
