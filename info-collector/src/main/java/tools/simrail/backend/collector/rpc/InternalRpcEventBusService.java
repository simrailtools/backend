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

package tools.simrail.backend.collector.rpc;

import com.google.protobuf.Empty;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.grpc.server.service.GrpcService;
import tools.simrail.backend.common.rpc.DispatchPostUpdateFrame;
import tools.simrail.backend.common.rpc.EventBusGrpc;
import tools.simrail.backend.common.rpc.JourneyUpdateFrame;
import tools.simrail.backend.common.rpc.ServerUpdateFrame;

/**
 * Service that handles the internal event bus communications.
 */
@GrpcService
public final class InternalRpcEventBusService extends EventBusGrpc.EventBusImplBase {

  private final Map<UUID, ServerCallStreamObserver<ServerUpdateFrame>> serverUpdateListeners;
  private final Map<UUID, ServerCallStreamObserver<JourneyUpdateFrame>> journeyUpdateListeners;
  private final Map<UUID, ServerCallStreamObserver<DispatchPostUpdateFrame>> dispatchPostUpdateListeners;

  public InternalRpcEventBusService() {
    this.serverUpdateListeners = new ConcurrentHashMap<>();
    this.journeyUpdateListeners = new ConcurrentHashMap<>();
    this.dispatchPostUpdateListeners = new ConcurrentHashMap<>();
  }

  /**
   * Sends out the given journey update frame to all registered journey update listeners. Note that the call to this
   * method must be synchronized externally if this method is called concurrently.
   *
   * @param updateFrame the journey update frame to publish.
   */
  public void publishJourneyUpdate(@Nonnull JourneyUpdateFrame updateFrame) {
    this.journeyUpdateListeners.values().forEach(observer -> observer.onNext(updateFrame));
  }

  /**
   * Sends out the given server update frame to all registered server update listeners. Note that the call to this
   * method must be synchronized externally if this method is called concurrently.
   *
   * @param updateFrame the server update frame to publish.
   */
  public void publishServerUpdate(@Nonnull ServerUpdateFrame updateFrame) {
    this.serverUpdateListeners.values().forEach(observer -> observer.onNext(updateFrame));
  }

  /**
   * Sends out the given dispatch post update frame to all registered dispatch post update listeners. Note that the call
   * to this method must be synchronized externally if this method is called concurrently.
   *
   * @param updateFrame the dispatch post update frame to publish.
   */
  public void publishDispatchPostUpdate(@Nonnull DispatchPostUpdateFrame updateFrame) {
    this.dispatchPostUpdateListeners.values().forEach(observer -> observer.onNext(updateFrame));
  }

  /**
   * Called when this service is destroyed, signalling to all clients that the connection will be closed.
   */
  @PreDestroy
  public void handleDestroy() {
    this.serverUpdateListeners.values().forEach(StreamObserver::onCompleted);
    this.journeyUpdateListeners.values().forEach(StreamObserver::onCompleted);
    this.dispatchPostUpdateListeners.values().forEach(StreamObserver::onCompleted);
  }

  /**
   * Adds a listener registration for journeys.
   */
  @Override
  public void subscribeToJourneys(@Nonnull Empty req, @Nonnull StreamObserver<JourneyUpdateFrame> responseObserver) {
    var serverCallObserver = (ServerCallStreamObserver<JourneyUpdateFrame>) responseObserver;
    this.registerObserver(serverCallObserver, this.journeyUpdateListeners);
  }

  /**
   * Adds a listener registration for servers.
   */
  @Override
  public void subscribeToServers(@Nonnull Empty req, @Nonnull StreamObserver<ServerUpdateFrame> responseObserver) {
    var serverCallObserver = (ServerCallStreamObserver<ServerUpdateFrame>) responseObserver;
    this.registerObserver(serverCallObserver, this.serverUpdateListeners);
  }

  /**
   * Adds a listener registration for dispatch posts.
   */
  @Override
  public void subscribeToDispatchPosts(
    @Nonnull Empty req,
    @Nonnull StreamObserver<DispatchPostUpdateFrame> responseObserver
  ) {
    var serverCallObserver = (ServerCallStreamObserver<DispatchPostUpdateFrame>) responseObserver;
    this.registerObserver(serverCallObserver, this.dispatchPostUpdateListeners);
  }

  /**
   * Registers the given stream observer into the list of observers for a specific type using a random id. Also sets
   * listeners on the observer to unregister the listener in case of a client disconnect.
   *
   * @param streamObserver the observer to register.
   * @param listeners      the map of listeners to register the observers to.
   * @param <V>            the type of responses being handled by the listeners.
   */
  private <V> void registerObserver(
    @Nonnull ServerCallStreamObserver<V> streamObserver,
    @Nonnull Map<UUID, ServerCallStreamObserver<V>> listeners
  ) {
    // register the listener into the given map
    var listenerId = UUID.randomUUID();
    listeners.put(listenerId, streamObserver);

    // set the listener to call on close/cancel to unregister the listener again
    Runnable closeHandler = () -> listeners.remove(listenerId);
    streamObserver.setOnCloseHandler(closeHandler);
    streamObserver.setOnCancelHandler(closeHandler);
  }
}
