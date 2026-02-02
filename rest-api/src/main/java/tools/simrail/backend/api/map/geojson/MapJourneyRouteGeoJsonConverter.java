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

package tools.simrail.backend.api.map.geojson;

import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.simrail.backend.api.journey.dto.JourneyStopPlaceDto;
import tools.simrail.backend.api.map.dto.MapJourneyRouteDto;
import tools.simrail.backend.api.shared.GeoPositionDto;

/**
 * Converter for a MapJourneyRouteDto to a geojson feature collection.
 */
@Component
public final class MapJourneyRouteGeoJsonConverter {

  private final ObjectMapper objectMapper;

  @Autowired
  public MapJourneyRouteGeoJsonConverter(@NonNull ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public @NonNull ObjectNode convertToGeojson(@NonNull MapJourneyRouteDto journeyRoute) {
    // create the top-level feature collection object, add a metadata object to it containing the
    // id of the journey that was requested. this should be valid geojson syntax as per rfc7946 section 7.1
    var featureCollection = this.objectMapper.createObjectNode();
    featureCollection.put("type", "FeatureCollection");
    featureCollection.set("metadata", this.createMetadataNode(journeyRoute));

    // final features of the feature collection
    var features = this.objectMapper.createArrayNode();
    featureCollection.set("features", features);

    // add points for each stop into the feature collection
    for (var stop : journeyRoute.stops()) {
      var stopFeature = this.createStopPlaceFeature(stop);
      features.add(stopFeature);
    }

    // add the polyline as a line string into the features
    var lineString = this.createPolylineFeature(journeyRoute.polyline());
    features.add(lineString);

    return featureCollection;
  }

  /**
   * Creates the top-level metadata object for the feature collection.
   */
  private @NonNull ObjectNode createMetadataNode(@NonNull MapJourneyRouteDto journeyRoute) {
    var metadata = this.objectMapper.createObjectNode();
    metadata.put("journeyId", journeyRoute.journeyId().toString());
    return metadata;
  }

  /**
   * Creates a geojson point feature for the given stop place object.
   */
  private @NonNull ObjectNode createStopPlaceFeature(@NonNull JourneyStopPlaceDto stopPlace) {
    // point is a geojson feature defining a single point, see rfc7946 section 3.1.2
    var point = this.objectMapper.createObjectNode();
    point.put("type", "Feature");

    // geometry where the point is located
    var pos = stopPlace.position();
    var coordinates = this.objectMapper.createArrayNode();
    coordinates.add(pos.longitude());
    coordinates.add(pos.latitude());

    var geometry = this.objectMapper.createObjectNode();
    geometry.put("type", "Point");
    geometry.set("coordinates", coordinates);
    point.set("geometry", geometry);

    // properties of the stop place
    var properties = this.objectMapper.createObjectNode();
    properties.put("id", stopPlace.id().toString());
    properties.put("name", stopPlace.name());
    properties.put("stopPlace", stopPlace.stopPlace());
    properties.put("inPlayableBorder", stopPlace.inPlayableBorder());
    point.set("properties", properties);

    return point;
  }

  /**
   * Converts the given coordinates of the polyline into a linestring feature.
   */
  private @NonNull ObjectNode createPolylineFeature(@NonNull List<GeoPositionDto> entries) {
    // line string is a geojson feature defining a polyline, see rfc7946 section 3.1.4
    var lineString = this.objectMapper.createObjectNode();
    lineString.put("type", "Feature");
    lineString.putNull("properties");

    // the line string geometry
    var geometry = this.objectMapper.createObjectNode();
    geometry.put("type", "LineString");

    // add the points along the route into the coordinates of the line string
    var coordinates = this.objectMapper.createArrayNode();
    for (var entry : entries) {
      var point = this.objectMapper.createArrayNode();
      point.add(entry.longitude());
      point.add(entry.latitude());
      coordinates.add(point);
    }

    // add coordinates to geometry and add the geometry object to the line string
    geometry.set("coordinates", coordinates);
    lineString.set("geometry", geometry);

    return lineString;
  }
}
