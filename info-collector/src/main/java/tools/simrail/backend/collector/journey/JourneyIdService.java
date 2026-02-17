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

package tools.simrail.backend.collector.journey;

import java.time.LocalDateTime;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import tools.simrail.backend.common.journey.JourneyEntity;
import tools.simrail.backend.common.journey.JourneyEventEntity;
import tools.simrail.backend.common.journey.JourneyEventType;
import tools.simrail.backend.common.util.UuidV5Factory;

/**
 * Service to generate ids for journey-related entities.
 */
@Service
public final class JourneyIdService {

  private final UuidV5Factory journeyIdFactory;
  private final UuidV5Factory journeyEventIdFactory;

  public JourneyIdService() {
    this.journeyIdFactory = new UuidV5Factory(JourneyEntity.ID_NAMESPACE);
    this.journeyEventIdFactory = new UuidV5Factory(JourneyEventEntity.ID_NAMESPACE);
  }

  /**
   * Creates a new uuid for a journey based on the given server and run id.
   *
   * @param serverId the id of the server the train run will take place on.
   * @param runId    the id of the run to generate the journey id for.
   * @return the generated journey id based on the given server and run id.
   */
  public @NonNull UUID generateJourneyId(@NonNull UUID serverId, @NonNull UUID runId) {
    return this.journeyIdFactory.create(serverId.toString() + runId);
  }

  /**
   * Generates a journey event id.
   *
   * @param journeyId     the id of the journey that is associated with the event.
   * @param pointId       the id of the point where the event is happening.
   * @param scheduledTime the time when the journey is scheduled to arrive at the point.
   * @param eventType     the type of event to generate the id for.
   * @return an event id based on the given input parameters.
   */
  public @NonNull UUID generateJourneyEventId(
    @NonNull UUID journeyId,
    @NonNull UUID pointId,
    @NonNull LocalDateTime scheduledTime,
    @NonNull JourneyEventType eventType
  ) {
    return this.journeyEventIdFactory.create(journeyId.toString() + pointId + scheduledTime + eventType);
  }

  /**
   * Generates a jit journey event id.
   *
   * @param journeyId       the id of the journey that is associated with the event.
   * @param pointId         the id of the point where the event is happening.
   * @param previousEventId the id of the previous event.
   * @param eventType       the type of event to generate the id for.
   * @return a jit event id based on the given input parameters.
   */
  public @NonNull UUID generateJitJourneyEventId(
    @NonNull UUID journeyId,
    @NonNull UUID pointId,
    @NonNull UUID previousEventId,
    @NonNull JourneyEventType eventType
  ) {
    return this.journeyEventIdFactory.create("JIT_" + journeyId + pointId + previousEventId + eventType);
  }
}
