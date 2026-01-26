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

package tools.simrail.backend.collector.journey;

import java.util.UUID;
import org.jspecify.annotations.NonNull;
import tools.simrail.backend.common.proto.EventBusProto;
import tools.simrail.backend.common.update.UpdatableField;
import tools.simrail.backend.common.update.UpdatableFieldGroup;

/**
 * Holder for the updates to a single journey.
 */
final class JourneyUpdateHolder {

  final UUID runId;
  final UUID journeyId;

  // the run id as a string, optimization to not call "toString()" on runId every time
  final String runIdString;

  // the updatable fields of a journey
  final UpdatableFieldGroup fieldGroup;
  final UpdatableField<Integer> speed;
  final UpdatableField<EventBusProto.User> driver;
  final UpdatableField<EventBusProto.GeoPosition> position;
  final UpdatableField<EventBusProto.SignalInfo> nextSignal;
  final UpdatableField<String> nextSignalId; // internally used only, not stored in cache (derived from next signal)

  public JourneyUpdateHolder(@NonNull UUID runId, @NonNull UUID journeyId) {
    this.runId = runId;
    this.journeyId = journeyId;
    this.runIdString = runId.toString();

    this.fieldGroup = new UpdatableFieldGroup();
    this.speed = this.fieldGroup.createField();
    this.position = this.fieldGroup.createField();
    this.driver = this.fieldGroup.createNullableField();
    this.nextSignal = this.fieldGroup.createNullableField();
    this.nextSignalId = this.fieldGroup.createNullableField();
  }
}
