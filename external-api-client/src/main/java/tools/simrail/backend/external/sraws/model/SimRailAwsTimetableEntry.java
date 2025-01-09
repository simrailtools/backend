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

package tools.simrail.backend.external.sraws.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single entry in a timetable of a scheduled train run.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public final class SimRailAwsTimetableEntry {

  /**
   * The type of the associated train at this point.
   */
  @JsonProperty("trainType")
  private String trainType;
  /**
   * The train number of the train at this point.
   */
  @JsonProperty("displayedTrainNumber")
  private String trainNumber;

  /**
   * The id of the point where this timetable entry takes place.
   */
  @JsonProperty("pointId")
  private String pointId;
  /**
   * The name of the point where this timetable entry takes place.
   */
  @JsonProperty("nameOfPoint")
  private String pointName;
  /**
   * A pretty version of the point name to be displayed to a person.
   */
  @JsonProperty("nameForPerson")
  private String prettyPointName;

  /**
   * The name of the point that is dispatching the point where this timetable entry takes place. If the point is not
   * controlled by a dispatcher, this field is null.
   */
  @JsonProperty("supervisedBy")
  private String dispatchingPoint;
  /**
   * The radio channels that are available at the station. This can either be:
   * <ol>
   *   <li>empty or a space to indicate that the value is not set.
   *   <li>a single radio channel.
   *   <li>a comma separated list of radio channels.
   * </ol>
   */
  @JsonProperty("radioChanels")
  private String radioChannels;
  /**
   * The mileage along the line where the point of this entry is located.
   */
  @JsonProperty("mileage")
  private double pointMileage;

  /**
   * The time when the train is scheduled to arrive at the point, null for the first point. This time is given in the
   * time zone of the server.
   */
  @JsonProperty("arrivalTime")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime arrivalTime;
  /**
   * The time when the train is scheduled to depart at the point, null for the last point. This time is given in the
   * time zone of the server.
   */
  @JsonProperty("departureTime")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime departureTime;

  /**
   * The type of stop that the associated train has at this point.
   */
  @JsonProperty("stopType")
  private StopType stopType;
  /**
   * The number of the track where the platform at which the train should stop is located. Might be present if the train
   * has a stop with passenger change (PH) at this point.
   */
  @JsonProperty("track")
  private Integer stopTrack;
  /**
   * The platform number (roman notation) where the train is scheduled to stop. Might be present if the train has a stop
   * with passenger change (PH) at this point.
   */
  @JsonProperty("platform")
  private String stopPlatform;
  /**
   * The category of the station from A (very big station) to E (small stopping point). Only present if the train has a
   * stop with passenger change (PH) at this point.
   */
  @JsonProperty("stationCategory")
  private String stationCategory;

  /**
   * The line onto which the train is scheduled to depart.
   */
  @JsonProperty("line")
  private int departureLine;
  /**
   * The speed that the train is allowed to drive at the point where this event takes place. This number must be
   * dividable by ten and can be zero.
   */
  @JsonProperty("maxSpeed")
  private int trainMaxAllowedSpeed;

  /**
   * An enumeration of the possible stop types at a point.
   */
  public enum StopType {

    /**
     * The train has no scheduled stop at the point.
     */
    @JsonProperty("NoStopOver")
    NONE,
    /**
     * The train has a technical stop at the point.
     */
    @JsonProperty("NoncommercialStop")
    PT,
    /**
     * The train has a stop to let passengers enter/exit at the point.
     */
    @JsonProperty("CommercialStop")
    PH,
  }
}
