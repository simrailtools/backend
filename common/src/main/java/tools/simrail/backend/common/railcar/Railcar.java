/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024-present Pasqual Koschmieder and contributors
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

package tools.simrail.backend.common.railcar;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A railcar that can drive in SimRail.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public final class Railcar {

  /**
   * The id of the railcar, usually its unique type descriptor.
   */
  @JsonProperty("id")
  private UUID id;
  /**
   * The id of the railcar used by the SimRail api.
   */
  @JsonProperty("api_name")
  private String apiId;
  /**
   * The display name of the rail car.
   */
  @JsonProperty("display_name")
  private String displayName;
  /**
   * The baptismal name of the locomotive / wagon. Can be {@code null} if none.
   */
  @JsonProperty("name")
  private String name;
  /**
   * The Steam id of the DLC that is required to get access to this railcar in SimRail, null if no DLC is needed.
   */
  @JsonProperty("required_dlc_id")
  private String requiredDlcId;

  /**
   * The identifier of the group to which this railcar belongs.
   */
  @JsonProperty("type_id")
  private String typeGroupId;
  /**
   * The designation of the railcar, can for example be the layout.
   */
  @JsonProperty("designation")
  private String designation;
  /**
   * The producer of the railcar.
   */
  @JsonProperty("producer")
  private String producer;
  /**
   * The years in which this railcar was produced. This can either be a single year (in the form yyyy) or a range of
   * years, delimited by {@code -}. In case of a range the first part is the start year of production (in form yyyy) and
   * the second part is either the end year (in form yyyy) or {@code Present} if the railcar is still produced.
   */
  @JsonProperty("production_years")
  private String productionYears;
  /**
   * The type of this railcar.
   */
  @JsonProperty("type")
  private RailcarType railcarType;

  /**
   * The weight of this railcar, in tons.
   */
  @JsonProperty("weight")
  private double weight;
  /**
   * The length of this railcar, in meters.
   */
  @JsonProperty("length")
  private double length;
  /**
   * The width of this railcar, in meters.
   */
  @JsonProperty("width")
  private double width;
  /**
   * The maximum speed of this railcar, in kilometers per hour.
   */
  @JsonProperty("max_speed")
  private int maximumSpeed;
}
