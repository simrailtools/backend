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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single scheduled train run.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public final class SimRailAwsTrainRun {

  /**
   * The unique identifier of this specific train run.
   */
  @JsonProperty("runId")
  private UUID runId;

  /**
   * The type identifier of the train.
   */
  @JsonProperty("trainName")
  private String trainDisplayName;
  /**
   * The number of the train within Poland.
   */
  @JsonProperty("trainNoLocal")
  private String trainNumber;
  /**
   * The internation train number, currently not in use.
   */
  @Deprecated
  @JsonProperty("trainNoInternational")
  private String trainNumberInternational;
  /**
   * The number of the train that this train will convert to at the final station, currently not in use.
   */
  @Deprecated
  @JsonProperty("continuesAs")
  private String continuationTrainNumber;

  /**
   * The length of the train (in meters), not very precise.
   */
  @JsonProperty("trainLength")
  private int length;
  /**
   * The weight of the train (in tons), not very precise.
   */
  @JsonProperty("trainWeight")
  private int weight;

  /**
   * The name of the traction unit, this is not the full api identifier of the unit.
   */
  @JsonProperty("locoType")
  private String tractionUnitName;

  /**
   * The timetable of this train run.
   */
  @JsonProperty("timetable")
  private List<SimRailAwsTimetableEntry> timetable;
}
