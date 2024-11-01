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

package tools.simrail.backend.external;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.ExceptionPropagationPolicy;
import feign.Feign;
import feign.Logger;
import feign.http2client.Http2Client;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.slf4j.Slf4jLogger;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public final class FeignClientProvider {

  private static final StackWalker CLASS_REF_RETAINING_STACK_WALKER =
    StackWalker.getInstance(Set.of(StackWalker.Option.RETAIN_CLASS_REFERENCE, StackWalker.Option.DROP_METHOD_INFO));

  private FeignClientProvider() {
    throw new UnsupportedOperationException();
  }

  public static @NotNull Feign.Builder prepareFeignInstance() {
    var callingClass = CLASS_REF_RETAINING_STACK_WALKER.getCallerClass();
    return Feign.builder()
      .client(new Http2Client())
      .logLevel(Logger.Level.FULL)
      .logger(new Slf4jLogger(callingClass))
      .exceptionPropagationPolicy(ExceptionPropagationPolicy.NONE);
  }

  public static @NotNull Feign.Builder prepareJsonFeignInstance() {
    // build json object mapper for encoding/decoding requests
    var bodyMapper = JsonMapper.builder()
      .addModule(new Jdk8Module())
      .addModule(new JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
      .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
      .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
      .enable(SerializationFeature.WRITE_DATES_WITH_ZONE_ID)
      .serializationInclusion(JsonInclude.Include.NON_NULL)
      .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
      .visibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
      .build();

    // create base feign instance
    var callingClass = CLASS_REF_RETAINING_STACK_WALKER.getCallerClass();
    return Feign.builder()
      .client(new Http2Client())
      .logLevel(Logger.Level.FULL)
      .logger(new Slf4jLogger(callingClass))
      .encoder(new JacksonEncoder(bodyMapper))
      .decoder(new JacksonDecoder(bodyMapper))
      .exceptionPropagationPolicy(ExceptionPropagationPolicy.NONE);
  }
}
