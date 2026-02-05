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

package tools.simrail.backend.external.brouter.request;

import feign.Param;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import tools.simrail.backend.external.brouter.feign.BRouterAlternativeModeExpander;
import tools.simrail.backend.external.brouter.feign.BRouterLonLatExpander;
import tools.simrail.backend.external.brouter.feign.BRouterOutputFormatExpander;

@Data
@With
@AllArgsConstructor
@NoArgsConstructor(staticName = "create")
public final class BRouterRouteRequest {

  /**
   * The positions of the waypoints on the route.
   */
  @Param(value = "lonlats", expander = BRouterLonLatExpander.class)
  private List<GeoPosition> positions;

  /**
   * The profile to use for routing.
   */
  @Param(value = "profile")
  private String profile;

  /**
   * The alternative routing engine to use for the request.
   */
  @Param(value = "alternativeidx", expander = BRouterAlternativeModeExpander.class)
  private AlternativeMode alternativeMode = AlternativeMode.DEFAULT;

  /**
   * The format to return the data in.
   */
  @Param(value = "format", expander = BRouterOutputFormatExpander.class)
  private OutputFormat outputFormat = OutputFormat.GEOJSON;

  /**
   * A geo position.
   *
   * @param latitude  the latitude of the position.
   * @param longitude the longitude of the position.
   */
  public record GeoPosition(double latitude, double longitude) {

  }

  /**
   * The output format of the routing data.
   */
  public enum OutputFormat {

    /**
     * Output the data in Keyhole Markup Language.
     */
    KML,
    /**
     * Output the data in GPS Exchange Format.
     */
    GPX,
    /**
     * Output the data in GeoJson format.
     */
    GEOJSON,
  }

  /**
   * Which alternative routing mode to use.
   */
  public enum AlternativeMode {

    /**
     * Default routing engine.
     */
    DEFAULT,
    /**
     * First alternative routing engine.
     */
    FIRST_ALTERNATIVE,
    /**
     * Second alternative routing engine.
     */
    SECOND_ALTERNATIVE,
    /**
     * Third alternative routing engine.
     */
    THIRD_ALTERNATIVE,
  }
}
