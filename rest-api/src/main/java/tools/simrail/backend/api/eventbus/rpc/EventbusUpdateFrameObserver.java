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

package tools.simrail.backend.api.eventbus.rpc;

import io.grpc.stub.StreamObserver;
import jakarta.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import tools.simrail.backend.common.rpc.EventBusGrpc;

/**
 * Observer for update frames to the backend which reconnects once the connection to the backend is lost.
 */
record EventbusUpdateFrameObserver<T>(
  @Nonnull Consumer<T> frameHandler,
  @Nonnull EventBusGrpc.EventBusStub eventBusStub,
  @Nonnull BiConsumer<EventBusGrpc.EventBusStub, StreamObserver<T>> connector,
  @Nonnull ConfigurableApplicationContext applicationContext
) implements StreamObserver<T> {

  private static final int RECONNECT_DELAY_MS = 5000; // 5 seconds
  private static final Logger LOGGER = LoggerFactory.getLogger(EventbusUpdateFrameObserver.class);

  @Override
  public void onNext(@Nonnull T value) {
    this.frameHandler.accept(value);
  }

  @Override
  public void onError(@Nonnull Throwable throwable) {
    try {
      LOGGER.warn("Frame update connection lost to backend: {}, trying to reconnect", throwable.getMessage());
      Thread.sleep(RECONNECT_DELAY_MS);
      this.tryReconnect();
    } catch (InterruptedException _) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void onCompleted() {
    LOGGER.warn("Server completed frame submission, trying to reconnect");
    this.tryReconnect();
  }

  /**
   * Tries to reconnect to the backend, failing after too many failed reconnect attempts.
   */
  void tryReconnect() {
    if (this.applicationContext.isActive()) {
      this.connector.accept(this.eventBusStub, this);
    }
  }
}
