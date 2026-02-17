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

package tools.simrail.backend.api.cache;

import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import io.nats.client.Connection;
import java.util.function.Function;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import tools.simrail.backend.common.cache.DataCache;
import tools.simrail.backend.common.event.EventSubjectFactory;
import tools.simrail.backend.common.proto.EventBusProto;

/**
 * Handles the initial subscription to the nats topic for each cache.
 */
@Component
final class CacheSubscriptionHandler implements ApplicationListener<ApplicationReadyEvent> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CacheSubscriptionHandler.class);

  private final Connection connection;
  private final DataCache<EventBusProto.ServerUpdateFrame> serverDataCache;
  private final DataCache<EventBusProto.JourneyUpdateFrame> journeyDataCache;
  private final DataCache<EventBusProto.DispatchPostUpdateFrame> dispatchPostDataCache;

  @Autowired
  CacheSubscriptionHandler(
    @NonNull Connection connection,
    @NonNull @Qualifier("server_data_cache") DataCache<EventBusProto.ServerUpdateFrame> serverDataCache,
    @NonNull @Qualifier("journey_realtime_cache") DataCache<EventBusProto.JourneyUpdateFrame> journeyDataCache,
    @NonNull @Qualifier("dispatch_post_cache") DataCache<EventBusProto.DispatchPostUpdateFrame> dispatchPostDataCache
  ) {
    this.connection = connection;
    this.serverDataCache = serverDataCache;
    this.journeyDataCache = journeyDataCache;
    this.dispatchPostDataCache = dispatchPostDataCache;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
    var serverUpdateSubject = EventSubjectFactory.createServerUpdateSubjectV1("*");
    var serverRemoveSubject = EventSubjectFactory.createServerRemoveSubjectV1("*");
    this.subscribeToCacheUpdates(
      this.serverDataCache,
      serverUpdateSubject,
      serverRemoveSubject,
      EventBusProto.ServerRemoveFrame.parser(),
      EventBusProto.ServerRemoveFrame::getServerId);

    var journeyUpdateSubject = EventSubjectFactory.createJourneyUpdateSubjectV1("*", "*");
    var journeyRemoveSubject = EventSubjectFactory.createJourneyRemoveSubjectV1("*", "*");
    this.subscribeToCacheUpdates(
      this.journeyDataCache,
      journeyUpdateSubject,
      journeyRemoveSubject,
      EventBusProto.JourneyRemoveFrame.parser(),
      EventBusProto.JourneyRemoveFrame::getJourneyId);

    var postUpdateSubject = EventSubjectFactory.createDispatchPostUpdateSubjectV1("*", "*");
    var postRemoveSubject = EventSubjectFactory.createDispatchPostRemoveSubjectV1("*", "*");
    this.subscribeToCacheUpdates(
      this.dispatchPostDataCache,
      postUpdateSubject,
      postRemoveSubject,
      EventBusProto.DispatchPostRemoveFrame.parser(),
      EventBusProto.DispatchPostRemoveFrame::getPostId);
  }

  /**
   * Subscribes the given cache to updates using the nats connection. Before subscribing, the cache data will be pulled
   * from the remote storage to get the cache hydrated.
   *
   * @param dataCache                      the cache to subscribe to updates.
   * @param dataUpdateSubject              the subject to use for the update subscription.
   * @param dataRemoveSubject              the subject to use for the removal subscription.
   * @param removeFrameParser              the parser for the remove frames for data.
   * @param removeFramePrimaryKeyExtractor the extractor for the id to remove based on the remove frame.
   * @param <T>                            the type of data being cached.
   * @param <R>                            the type of the remove update frames.
   */
  private <T extends MessageLite, R extends MessageLite> void subscribeToCacheUpdates(
    @NonNull DataCache<T> dataCache,
    @NonNull String dataUpdateSubject,
    @NonNull String dataRemoveSubject,
    @NonNull Parser<R> removeFrameParser,
    @NonNull Function<R, String> removeFramePrimaryKeyExtractor
  ) {
    // hydrate the cache before subscribing
    dataCache.pullCacheFromStorage();

    // subscribe the dispatcher to the updates of data (primary cached data)
    var dispatcher = this.connection.createDispatcher();
    dispatcher.subscribe(dataUpdateSubject, msg -> {
      try {
        var updateFrame = dataCache.getMessageParser().parseFrom(msg.getData());
        dataCache.updateLocalValue(updateFrame);
      } catch (Exception exception) {
        LOGGER.warn("Failed to handle cache data update frame (topic: {})", msg.getSubject(), exception);
      }
    });

    // subscribe the dispatcher to the removals of data
    dispatcher.subscribe(dataRemoveSubject, msg -> {
      try {
        var removeFrame = removeFrameParser.parseFrom(msg.getData());
        var primaryCacheKey = removeFramePrimaryKeyExtractor.apply(removeFrame);
        dataCache.removeLocallyByPrimaryKey(primaryCacheKey);
      } catch (Exception exception) {
        LOGGER.warn("Failed to handle cache data remove frame (topic: {})", msg.getSubject(), exception);
      }
    });

    LOGGER.info("Hydrated and registered update listener for cache {}", dataCache.getName());
  }
}
