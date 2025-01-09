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

package tools.simrail.backend.api.converter;

import jakarta.annotation.Nonnull;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.stereotype.Component;

/**
 * Registers custom converters into the proxy projection factory of spring, necessary until <a
 * href="https://github.com/spring-projects/spring-data-commons/issues/2335">the GitHub issue</a> is solved with a
 * conclusive response.
 */
@Component
final class ConverterRegistrationListener {

  @EventListener
  public void onApplicationReady(@Nonnull ApplicationReadyEvent event) throws Exception {
    // get the conversion service used for proxy projection factories
    var proxyProjectionFactoryClass = Class.forName("org.springframework.data.projection.ProxyProjectionFactory");
    var projectionConversionServiceField = proxyProjectionFactoryClass.getDeclaredField("CONVERSION_SERVICE");
    projectionConversionServiceField.setAccessible(true);
    var projectionConversionService = (GenericConversionService) projectionConversionServiceField.get(null);

    // register the custom converters
    projectionConversionService.addConverter(new InstantToOffsetDateTimeConverter());
  }
}
