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

package tools.simrail.backend.common.config;

import java.time.Duration;
import org.jspecify.annotations.NonNull;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.Kryo5Codec;
import org.redisson.config.Config;
import org.redisson.config.ConstantDelay;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.simrail.backend.common.cache.DataCache;
import tools.simrail.backend.common.proto.EventBusProto;

/**
 * Configures everything related to data caching.
 */
@Configuration
public class CommonDataCacheConfiguration {

  @Bean(destroyMethod = "shutdown")
  public @NonNull RedissonClient createRedissonClient(
    @Value("${sit.valkey.url}") String valkeyUrl,
    @Value("${sit.valkey.db-index:0}") int databaseIndex
  ) {
    var config = new Config()
      .setTcpNoDelay(true)
      .setCodec(new Kryo5Codec());
    config.useSingleServer()
      .setAddress(valkeyUrl)
      .setDatabase(databaseIndex)
      // pool options
      .setConnectionPoolSize(64)
      .setConnectionMinimumIdleSize(10)
      .setSubscriptionConnectionPoolSize(0)
      .setSubscriptionConnectionMinimumIdleSize(0)
      // timeouts & retries
      .setTimeout(5000)
      .setRetryAttempts(2)
      .setConnectTimeout(5000)
      .setIdleConnectionTimeout(30_000)
      .setPingConnectionInterval(10_000)
      .setRetryDelay(new ConstantDelay(Duration.ofSeconds(1)))
      .setReconnectionDelay(new ConstantDelay(Duration.ofSeconds(1)));
    return Redisson.create(config);
  }

  @Bean(name = "journey_realtime_cache")
  public @NonNull DataCache<EventBusProto.JourneyUpdateFrame> journeyDataCache(@NonNull RedissonClient redisson) {
    return new DataCache<>(
      "journey_realtime_cache",
      Duration.ofHours(6),
      redisson,
      EventBusProto.JourneyUpdateFrame.parser(),
      data -> data.getBaseData().getTimestamp(),
      data -> data.getIds().getDataId(),
      data -> data.getIds().getForeignId());
  }

  @Bean(name = "server_data_cache")
  public @NonNull DataCache<EventBusProto.ServerUpdateFrame> serverDataCache(@NonNull RedissonClient redisson) {
    return new DataCache<>(
      "server_data_cache",
      Duration.ofHours(12),
      redisson,
      EventBusProto.ServerUpdateFrame.parser(),
      data -> data.getBaseData().getTimestamp(),
      data -> data.getIds().getDataId(),
      data -> data.getIds().getForeignId());
  }

  @Bean(name = "dispatch_post_cache")
  public @NonNull DataCache<EventBusProto.DispatchPostUpdateFrame> postDataCache(@NonNull RedissonClient redisson) {
    return new DataCache<>(
      "dispatch_post_cache",
      Duration.ofHours(12),
      redisson,
      EventBusProto.DispatchPostUpdateFrame.parser(),
      data -> data.getBaseData().getTimestamp(),
      data -> data.getIds().getDataId(),
      data -> data.getIds().getServerId() + '_' + data.getIds().getForeignId());
  }
}
