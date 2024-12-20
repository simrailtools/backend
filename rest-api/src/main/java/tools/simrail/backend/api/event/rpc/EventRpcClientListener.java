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

package tools.simrail.backend.api.event.rpc;

import com.google.protobuf.Empty;
import jakarta.annotation.Nonnull;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.grpc.client.GrpcChannelFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tools.simrail.backend.common.rpc.EventBusGrpc;

/**
 * Listener that connects to the backend to listen for update frames once the application reports as ready.
 */
@Component
class EventRpcClientListener {

  private final GrpcChannelFactory channelFactory;
  private final EventRpcStreamFrameHandler rpcHandler;

  @Autowired
  public EventRpcClientListener(
    @Nonnull GrpcChannelFactory channelFactory,
    @Nonnull EventRpcStreamFrameHandler rpcHandler
  ) {
    this.channelFactory = channelFactory;
    this.rpcHandler = rpcHandler;
  }

  @Async
  @EventListener
  public void onApplicationReady(@Nonnull ApplicationReadyEvent event) {
    var collectorChannel = this.channelFactory.createChannel("collector").build();
    var eventBusStub = EventBusGrpc.newStub(collectorChannel);

    // establish one stream per possible update frame
    var updateFrameObservers = List.of(
      new EventUpdateFrameObserver<>(
        this.rpcHandler::handleServerUpdate,
        eventBusStub,
        (stub, observer) -> stub.subscribeToServers(Empty.getDefaultInstance(), observer),
        event.getApplicationContext()),
      new EventUpdateFrameObserver<>(
        this.rpcHandler::handleJourneyUpdate,
        eventBusStub,
        (stub, observer) -> stub.subscribeToJourneys(Empty.getDefaultInstance(), observer),
        event.getApplicationContext()),
      new EventUpdateFrameObserver<>(
        this.rpcHandler::handleDispatchPostUpdate,
        eventBusStub,
        (stub, observer) -> stub.subscribeToDispatchPosts(Empty.getDefaultInstance(), observer),
        event.getApplicationContext()));
    for (var updateFrameObserver : updateFrameObservers) {
      updateFrameObserver.tryReconnect();
    }
  }
}
