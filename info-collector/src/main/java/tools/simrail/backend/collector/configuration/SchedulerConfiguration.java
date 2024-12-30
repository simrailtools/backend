/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024 Pasqual Koschmieder and contributors
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

import jakarta.annotation.Nonnull;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

/**
 * Configures everything related to scheduling.
 */
@Configuration
@EnableScheduling
public class SchedulerConfiguration {

  @Bean(name = "train_collect_scheduler")
  public @Nonnull TaskScheduler trainCollectionTaskScheduler() {
    var threadFactory = new CustomizableThreadFactory("train-collector-thread-");
    var executor = Executors.newSingleThreadScheduledExecutor(threadFactory);
    return new ConcurrentTaskScheduler(executor);
  }

  @Bean(name = "timetable_collect_scheduler")
  public @Nonnull TaskScheduler timetableCollectionTaskScheduler() {
    var threadFactory = new CustomizableThreadFactory("timetable-collector-thread-");
    var executor = Executors.newSingleThreadScheduledExecutor(threadFactory);
    return new ConcurrentTaskScheduler(executor);
  }

  @Bean(name = "dispatch_post_collect_scheduler")
  public @Nonnull TaskScheduler dispatchPostCollectionTaskScheduler() {
    var threadFactory = new CustomizableThreadFactory("dispatch-post-collector-thread-");
    var executor = Executors.newSingleThreadScheduledExecutor(threadFactory);
    return new ConcurrentTaskScheduler(executor);
  }

  @Bean(name = "server_collect_scheduler")
  public @Nonnull TaskScheduler serverCollectionTaskScheduler() {
    var threadFactory = new CustomizableThreadFactory("server-collector-thread-");
    var executor = Executors.newSingleThreadScheduledExecutor(threadFactory);
    return new ConcurrentTaskScheduler(executor);
  }

  @Bean(name = "vehicle_collect_scheduler")
  public @Nonnull TaskScheduler vehicleCollectionTaskScheduler() {
    var threadFactory = new CustomizableThreadFactory("vehicle-collector-thread-");
    var executor = Executors.newSingleThreadScheduledExecutor(threadFactory);
    return new ConcurrentTaskScheduler(executor);
  }
}
