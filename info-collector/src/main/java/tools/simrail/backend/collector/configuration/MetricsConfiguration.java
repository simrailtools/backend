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

package tools.simrail.backend.collector.configuration;

import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.TimedAspect;
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
   * Aspect for intercepting methods annotated with timed.
   */
  @Bean
  public @Nonnull TimedAspect timedAspect(@Nonnull MeterRegistry registry) {
    return new TimedAspect(registry);
  }

  /**
   * Aspect for intercepting methods annotated with counted.
   */
  @Bean
  public @Nonnull CountedAspect countedAspect(@Nonnull MeterRegistry registry) {
    return new CountedAspect(registry);
  }

  /**
   * Metric displaying the number of rows that were deleted in the last database cleanup.
   */
  @Bean("db_cleanup_deletions_total")
  public @Nonnull Counter dbCleanupDeletionsTotalCounter(@Nonnull MeterRegistry registry) {
    return Counter.builder("db_cleanup_deletions_total")
      .description("Total number of entries deleted from the database")
      .register(registry);
  }

  /**
   * Timer for db cleanup runs.
   */
  @Bean("db_cleanup_duration_seconds")
  public @Nonnull Timer dbCleanupDurationTimer(@Nonnull MeterRegistry registry) {
    return Timer.builder("db_cleanup_duration_seconds")
      .description("Elapsed seconds while cleaning up old entries from the database")
      .register(registry);
  }

  /**
   * Gauge for the count of dispatch posts that were collected.
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
   * Timer for the dispatch posts collections.
   */
  @Bean("dispatch_post_collection_duration")
  public @Nonnull Meter.MeterProvider<Timer> dispatchPostCollectionDurationTimer(@Nonnull MeterRegistry registry) {
    return Timer.builder("dispatch_post_collection_seconds")
      .description("Elapsed seconds while collection dispatch posts")
      .withRegistry(registry);
  }

  /**
   * Gauge for the count of journeys that were marked as canceled.
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
   * Timer for the cancellation marking task.
   */
  @Bean("journey_cancellation_mark_duration")
  public @Nonnull Meter.MeterProvider<Timer> journeyCancellationMarkingTimer(@Nonnull MeterRegistry registry) {
    return Timer.builder("journey_cancellation_mark_seconds")
      .description("Elapsed seconds while marking non-spawned journeys as cancelled")
      .withRegistry(registry);
  }

  /**
   * Gauge for the number of runs that were collected in the last timetable collection.
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
   * Timer for the train run collection task.
   */
  @Bean("timetable_run_collect_duration")
  public @Nonnull Meter.MeterProvider<Timer> timetableRunCollectTimer(@Nonnull MeterRegistry registry) {
    return Timer.builder("timetable_run_collect_seconds")
      .description("Elapsed seconds while collecting train runs from timetable")
      .withRegistry(registry);
  }

  /**
   * Timer for the active train collection task.
   */
  @Bean("active_journey_collect_duration")
  public @Nonnull Meter.MeterProvider<Timer> activeJourneyCollectTimer(@Nonnull MeterRegistry registry) {
    return Timer.builder("active_journey_collect_seconds")
      .description("Elapsed seconds while collecting active train data on server")
      .withRegistry(registry);
  }

  /**
   * Gauge for the count of journeys that were updated during train data collection.
   */
  @Bean("active_journeys_updated_total")
  public @Nonnull PerServerGauge updatedActiveJourneysCounter(@Nonnull MeterRegistry registry) {
    return new PerServerGauge(
      registry,
      "active_journeys_updated_total",
      null,
      "Total number of active journeys updated");
  }

  /**
   * Counter for the number of active trains without an associated journey.
   */
  @Bean("active_trains_without_journey_total")
  public @Nonnull PerServerGauge activeTrainsWithoutJourneyCounter(@Nonnull MeterRegistry registry) {
    return new PerServerGauge(
      registry,
      "active_trains_without_journey_total",
      null,
      "Total number of active trains without an associated journey");
  }

  /**
   * Timer for the time taken to collect the predicted vehicle compositions.
   */
  @Bean("predicted_vc_collect_duration")
  public @Nonnull Meter.MeterProvider<Timer> predictedCompositionCollectTimer(@Nonnull MeterRegistry registry) {
    return Timer.builder("predicted_vehicle_composition_collect_seconds")
      .description("Elapsed seconds while collecting predicted vehicle compositions")
      .withRegistry(registry);
  }

  /**
   * Timer for the time taken to collect the actual vehicle compositions.
   */
  @Bean("actual_vc_collect_duration")
  public @Nonnull Meter.MeterProvider<Timer> actualCompositionCollectTimer(@Nonnull MeterRegistry registry) {
    return Timer.builder("actual_vehicle_composition_collect_seconds")
      .description("Elapsed seconds while collecting actual vehicle compositions")
      .withRegistry(registry);
  }
}
