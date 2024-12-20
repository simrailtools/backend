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

package tools.simrail.backend.api.event;

import jakarta.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Configuration for the websocket communication used for event transmission.
 */
@Configuration
@EnableWebSocket
class EventWebsocketConfiguration implements WebSocketConfigurer {

  private final EventWebsocketHandler eventWebsocketHandler;
  private final EventWebsocketHandshakeInterceptor eventWebsocketInterceptor;

  @Autowired
  public EventWebsocketConfiguration(
    @Nonnull EventWebsocketHandler eventWebsocketHandler,
    @Nonnull EventWebsocketHandshakeInterceptor eventWebsocketInterceptor
  ) {
    this.eventWebsocketHandler = eventWebsocketHandler;
    this.eventWebsocketInterceptor = eventWebsocketInterceptor;
  }

  @Override
  public void registerWebSocketHandlers(@Nonnull WebSocketHandlerRegistry registry) {
    registry
      .addHandler(this.eventWebsocketHandler, "/sit-events/v1")
      .addInterceptors(this.eventWebsocketInterceptor)
      .setAllowedOrigins("*");
  }
}
