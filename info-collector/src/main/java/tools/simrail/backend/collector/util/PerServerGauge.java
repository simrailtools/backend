/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024-2026 Pasqual Koschmieder and contributors
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

package tools.simrail.backend.collector.util;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import tools.simrail.backend.collector.server.SimRailServerDescriptor;

/**
 * A wrapper for handling a gauge state per server.
 */
public final class PerServerGauge {

  private final MeterRegistry meterRegistry;
  private final Map<UUID, AtomicInteger> gaugeStateByServer;

  private final String metricName;
  private final String metricBaseUnit;
  private final String metricDescription;

  public PerServerGauge(
    @Nonnull MeterRegistry meterRegistry,
    @Nonnull String metricName,
    @Nullable String metricBaseUnit,
    @Nullable String metricDescription
  ) {
    this.meterRegistry = meterRegistry;
    this.metricName = metricName;
    this.metricBaseUnit = metricBaseUnit;
    this.metricDescription = metricDescription;
    this.gaugeStateByServer = new ConcurrentHashMap<>(24, 0.9f, 1);
  }

  /**
   * Sets the value of the gauge for the given server to the given value.
   *
   * @param server the server for which the state should be set.
   * @param value  the value to set for the gauge associated with the server.
   */
  public void setValue(@Nonnull SimRailServerDescriptor server, int value) {
    var serverGaugeState = this.gaugeStateByServer.computeIfAbsent(server.id(), _ -> {
      var state = new AtomicInteger();
      Gauge.builder(this.metricName, state::get)
        .baseUnit(this.metricBaseUnit)
        .description(this.metricDescription)
        .tag("server_code", server.code())
        .register(this.meterRegistry);
      return state;
    });
    serverGaugeState.set(value);
  }
}
