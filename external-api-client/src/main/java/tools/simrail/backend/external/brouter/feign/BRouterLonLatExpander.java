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

package tools.simrail.backend.external.brouter.feign;

import feign.Param;
import java.util.List;
import java.util.StringJoiner;
import org.jetbrains.annotations.NotNull;
import tools.simrail.backend.external.brouter.request.BRouterRouteRequest;

/**
 * Param expander for a list of geo positions.
 */
public final class BRouterLonLatExpander implements Param.Expander {

  @Override
  @SuppressWarnings("unchecked")
  public @NotNull String expand(@NotNull Object value) {
    var positionsJoiner = new StringJoiner("|");
    var positionsList = (List<BRouterRouteRequest.GeoPosition>) value;
    for (var position : positionsList) {
      var formattedPosition = String.format("%s,%s", position.longitude(), position.latitude());
      positionsJoiner.add(formattedPosition);
    }

    return positionsJoiner.toString();
  }
}
