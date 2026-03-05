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

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.LinkedBlockingQueue;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.simrail.backend.collector.server.SimRailServerDescriptor;
import tools.simrail.backend.common.journey.JourneyEventRepository;
import tools.simrail.backend.common.signal.PlatformSignalProvider;

/**
 * Factory for per-server event realtime updaters.
 */
@Component
final class JourneyEventRealtimeUpdaterFactory {

  private final MeterRegistry meterRegistry;
  private final JourneyIdService journeyIdService;
  private final PlatformSignalProvider signalProvider;
  private final TransactionTemplate transactionTemplate;
  private final JourneyEventRepository journeyEventRepository;

  @Autowired
  JourneyEventRealtimeUpdaterFactory(
    @NonNull MeterRegistry meterRegistry,
    @NonNull JourneyIdService journeyIdService,
    @NonNull PlatformSignalProvider signalProvider,
    @NonNull TransactionTemplate transactionTemplate,
    @NonNull JourneyEventRepository journeyEventRepository
  ) {
    this.meterRegistry = meterRegistry;
    this.journeyIdService = journeyIdService;
    this.signalProvider = signalProvider;
    this.transactionTemplate = transactionTemplate;
    this.journeyEventRepository = journeyEventRepository;
  }

  public @NonNull JourneyEventRealtimeUpdater create(@NonNull SimRailServerDescriptor server) {
    // create update queue and metrics for it
    var updateQueue = new LinkedBlockingQueue<JourneyEventUpdateRequest>();
    Gauge.builder("journey_event_update_queue_size", updateQueue::size)
      .tag("server_code", server.code())
      .description("The updates that are pending processing by the event realtime updater")
      .register(meterRegistry);

    // construct the realtime updater & start the event update thread
    var eventUpdateTimer = Timer.builder("journey_event_update_time_seconds")
      .tag("server_code", server.code())
      .description("Elapsed seconds while processing event updates for a single journey")
      .register(meterRegistry);
    var updater = new JourneyEventRealtimeUpdater(
      this.journeyIdService,
      this.signalProvider,
      this.transactionTemplate,
      this.journeyEventRepository,
      eventUpdateTimer,
      updateQueue);
    updater.startEventUpdates();
    return updater;
  }
}
