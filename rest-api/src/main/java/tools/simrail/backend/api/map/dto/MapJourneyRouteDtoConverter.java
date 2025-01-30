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

package tools.simrail.backend.api.map.dto;

import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.simrail.backend.api.journey.dto.JourneyGeoPositionDto;
import tools.simrail.backend.api.journey.dto.JourneyStopPlaceDto;
import tools.simrail.backend.api.map.data.MapEventSummaryProjection;
import tools.simrail.backend.common.point.SimRailPointProvider;

/**
 * Converter for journey info to route info DTO.
 */
@Component
public final class MapJourneyRouteDtoConverter
  implements BiFunction<List<MapEventSummaryProjection>, ArrayNode, MapJourneyRouteDto> {

  private final SimRailPointProvider pointProvider;

  @Autowired
  public MapJourneyRouteDtoConverter(@Nonnull SimRailPointProvider pointProvider) {
    this.pointProvider = pointProvider;
  }

  @Override
  public @Nonnull MapJourneyRouteDto apply(
    @Nonnull List<MapEventSummaryProjection> events,
    @Nonnull ArrayNode routingPolylineResponse
  ) {
    // construct the polyline from the LineString feature of the GeoJson response
    var polyline = new ArrayList<MapPolylineEntryDto>();
    for (var node : routingPolylineResponse) {
      var data = (ArrayNode) node;
      var lon = data.get(0).asDouble();
      var lat = data.get(1).asDouble();
      var elevation = data.get(2).asDouble();
      polyline.add(new MapPolylineEntryDto(lat, lon, elevation));
    }

    // convert the stops along the journey route
    var stops = new ArrayList<JourneyStopPlaceDto>();
    for (var event : events) {
      var stop = event.getStopDescriptor();
      var point = this.pointProvider.findPointByIntId(stop.getId())
        .orElseThrow(() -> new IllegalStateException("Missing point with id " + stop.getId()));
      var pos = new JourneyGeoPositionDto(point.getPosition().getLatitude(), point.getPosition().getLongitude());
      var stopPlace = new JourneyStopPlaceDto(stop.getId(), point.getName(), pos, stop.isPlayable());
      stops.add(stopPlace);
    }

    // get the journey id from the first event
    var firstEvent = events.getFirst();
    var journeyId = firstEvent.getJourneyId();

    // construct the full journey route dto
    return new MapJourneyRouteDto(journeyId, stops, polyline);
  }
}
