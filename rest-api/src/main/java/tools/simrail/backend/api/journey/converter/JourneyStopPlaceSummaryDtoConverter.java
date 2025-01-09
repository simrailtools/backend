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

package tools.simrail.backend.api.journey.converter;

import jakarta.annotation.Nonnull;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import tools.simrail.backend.api.journey.dto.JourneyStopPlaceSummaryDto;
import tools.simrail.backend.common.journey.JourneyStopDescriptor;

/**
 * Converter for journey stop descriptors to a stop place DTO.
 */
@Component
public final class JourneyStopPlaceSummaryDtoConverter
  implements Function<JourneyStopDescriptor, JourneyStopPlaceSummaryDto> {

  @Override
  public @Nonnull JourneyStopPlaceSummaryDto apply(@Nonnull JourneyStopDescriptor stop) {
    return new JourneyStopPlaceSummaryDto(stop.getId(), stop.getName(), stop.isPlayable());
  }
}
