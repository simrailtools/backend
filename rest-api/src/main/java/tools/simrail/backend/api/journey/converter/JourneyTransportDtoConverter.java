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

package tools.simrail.backend.api.journey.converter;

import java.util.function.Function;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import tools.simrail.backend.api.journey.dto.JourneyTransportDto;
import tools.simrail.backend.common.journey.JourneyTransport;

/**
 * Converter for journey transports to a DTO.
 */
@Component
public final class JourneyTransportDtoConverter implements Function<JourneyTransport, JourneyTransportDto> {

  @Override
  public @NonNull JourneyTransportDto apply(@NonNull JourneyTransport transport) {
    return new JourneyTransportDto(
      transport.getCategory(),
      transport.getExternalCategory(),
      transport.getNumber(),
      transport.getLine(),
      transport.getLabel(),
      transport.getType(),
      transport.getMaxSpeed());
  }
}
