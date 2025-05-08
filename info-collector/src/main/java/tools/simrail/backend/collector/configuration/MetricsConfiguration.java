package tools.simrail.backend.collector.configuration;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Nonnull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.simrail.backend.collector.metric.PerServerGauge;

/**
 * Configurations for metrics.
 */
@Configuration
public class MetricsConfiguration {

  /**
   *
   */
  @Bean("db_cleanup_deletions_total")
  public @Nonnull Counter dbCleanupDeletionsTotalCounter(@Nonnull MeterRegistry registry) {
    return Counter.builder("db_cleanup_deletions_total")
      .description("Total number of entries deleted from the database")
      .register(registry);
  }

  /**
   *
   */
  @Bean("db_cleanup_duration_seconds")
  public @Nonnull Timer dbCleanupDurationTimer(@Nonnull MeterRegistry registry) {
    return Timer.builder("db_cleanup_duration_seconds")
      .description("Elapsed seconds while cleaning up old entries from the database")
      .register(registry);
  }

  /**
   *
   */
  @Bean("dispatch_post_collected_total")
  public @Nonnull PerServerGauge dispatchPostCollectedTotalCounter(@Nonnull MeterRegistry registry) {
    return new PerServerGauge(
      registry,
      "dispatch_post_collected_total",
      null,
      "Total number of dispatch posts that were collected");
  }

  /**
   *
   */
  @Bean("dispatch_post_collection_duration")
  public @Nonnull Meter.MeterProvider<Timer> dispatchPostCollectionDurationTimer(@Nonnull MeterRegistry registry) {
    return Timer.builder("dispatch_post_collection_seconds")
      .description("Elapsed seconds while collection dispatch posts")
      .withRegistry(registry);
  }

  /**
   *
   */
  @Bean("journey_cancelled_marked_total")
  public @Nonnull PerServerGauge journeysMarkedAsCancelledCounter(@Nonnull MeterRegistry registry) {
    return new PerServerGauge(
      registry,
      "journey_cancelled_marked_total",
      null,
      "Total number of journeys that were marked as cancelled");
  }

  /**
   *
   */
  @Bean("journey_cancellation_mark_duration")
  public @Nonnull Meter.MeterProvider<Timer> journeyCancellationMarkingTimer(@Nonnull MeterRegistry registry) {
    return Timer.builder("journey_cancellation_mark_seconds")
      .description("Elapsed seconds while marking non-spawned journeys as cancelled")
      .withRegistry(registry);
  }

  /**
   *
   */
  @Bean("timetable_collected_runs_total")
  public @Nonnull PerServerGauge timetableCollectedRunsCounter(@Nonnull MeterRegistry registry) {
    return new PerServerGauge(
      registry,
      "timetable_collected_runs_total",
      null,
      "Total number of train runs saved during timetable collection");
  }

  /**
   *
   */
  @Bean("timetable_run_collect_duration")
  public @Nonnull Meter.MeterProvider<Timer> timetableRunCollectTimer(@Nonnull MeterRegistry registry) {
    return Timer.builder("timetable_run_collect_seconds")
      .description("Elapsed seconds while collecting train runs from timetable")
      .withRegistry(registry);
  }

  /**
   *
   */
  @Bean("active_journey_collect_duration")
  public @Nonnull Meter.MeterProvider<Timer> activeJourneyCollectTimer(@Nonnull MeterRegistry registry) {
    return Timer.builder("active_journey_collect_seconds")
      .description("Elapsed seconds while collecting active train data on server")
      .withRegistry(registry);
  }

  /**
   *
   */
  @Bean("active_journeys_updated_total")
  public @Nonnull Meter.MeterProvider<Counter> updatedActiveJourneysCounter(@Nonnull MeterRegistry registry) {
    return Counter.builder("active_journeys_updated_total")
      .description("Total number of active journeys updated")
      .withRegistry(registry);
  }

  /**
   *
   */
  @Bean("active_trains_without_journey_total")
  public @Nonnull Meter.MeterProvider<Counter> activeTrainsWithoutJourneyCounter(@Nonnull MeterRegistry registry) {
    return Counter.builder("active_trains_without_journey_total")
      .description("Total number of active trains without an associated journey")
      .withRegistry(registry);
  }

  /**
   *
   */
  @Bean("predicted_vc_collect_duration")
  public @Nonnull Meter.MeterProvider<Timer> predictedCompositionCollectTimer(@Nonnull MeterRegistry registry) {
    return Timer.builder("predicted_vehicle_composition_collect_seconds")
      .description("Elapsed seconds while collecting predicted vehicle compositions")
      .withRegistry(registry);
  }

  /**
   *
   */
  @Bean("actual_vc_collect_duration")
  public @Nonnull Meter.MeterProvider<Timer> actualCompositionCollectTimer(@Nonnull MeterRegistry registry) {
    return Timer.builder("actual_vehicle_composition_collect_seconds")
      .description("Elapsed seconds while collecting actual vehicle compositions")
      .withRegistry(registry);
  }
}
