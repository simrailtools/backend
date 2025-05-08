package tools.simrail.backend.collector.metric;

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
