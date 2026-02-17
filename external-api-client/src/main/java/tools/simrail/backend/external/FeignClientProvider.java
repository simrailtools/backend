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

package tools.simrail.backend.external;

import com.fasterxml.jackson.annotation.JsonInclude;
import feign.ExceptionPropagationPolicy;
import feign.Feign;
import feign.Logger;
import feign.jackson3.Jackson3Decoder;
import feign.jackson3.Jackson3Encoder;
import feign.slf4j.Slf4jLogger;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.simrail.backend.external.feign.CustomFieldQueryMapEncoder;
import tools.simrail.backend.external.feign.ExceptionHandlingRetryer;
import tools.simrail.backend.external.feign.FeignJava11Client;
import tools.simrail.backend.external.feign.FeignJsonResponseTupleDecoder;
import tools.simrail.backend.external.feign.FeignResponseInterceptor;

public final class FeignClientProvider {

  private static final StackWalker CLASS_REF_RETAINING_STACK_WALKER =
    StackWalker.getInstance(Set.of(StackWalker.Option.RETAIN_CLASS_REFERENCE, StackWalker.Option.DROP_METHOD_INFO));

  private FeignClientProvider() {
    throw new UnsupportedOperationException();
  }

  public static Feign.@NonNull Builder prepareFeignInstance() {
    var callingClass = CLASS_REF_RETAINING_STACK_WALKER.getCallerClass();
    return Feign.builder()
      .logLevel(Logger.Level.FULL)
      .client(new FeignJava11Client())
      .logger(new Slf4jLogger(callingClass))
      .queryMapEncoder(new CustomFieldQueryMapEncoder())
      .exceptionPropagationPolicy(ExceptionPropagationPolicy.NONE);
  }

  public static Feign.@NonNull Builder prepareJsonFeignInstance() {
    // build JSON object mapper for encoding/decoding requests
    var bodyMapper = JsonMapper.builder()
      .enable(DateTimeFeature.WRITE_DATES_WITH_ZONE_ID)
      .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
      .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
      .disable(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
      .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
      .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
      .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
      .changeDefaultPropertyInclusion(value -> value.withValueInclusion(JsonInclude.Include.NON_NULL))
      .build();

    // create base feign instance
    var callingClass = CLASS_REF_RETAINING_STACK_WALKER.getCallerClass();
    return Feign.builder()
      .logLevel(Logger.Level.FULL)
      .client(new FeignJava11Client())
      .logger(new Slf4jLogger(callingClass))
      .encoder(new Jackson3Encoder(bodyMapper))
      .decoder(new FeignJsonResponseTupleDecoder(new Jackson3Decoder(bodyMapper)))
      .retryer(ExceptionHandlingRetryer.INSTANCE)
      .queryMapEncoder(new CustomFieldQueryMapEncoder())
      .responseInterceptor(new FeignResponseInterceptor())
      .exceptionPropagationPolicy(ExceptionPropagationPolicy.NONE);
  }
}
