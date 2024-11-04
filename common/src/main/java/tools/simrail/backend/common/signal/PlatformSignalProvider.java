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

package tools.simrail.backend.common.signal;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * A service that provides platform information based on point ids and signal ids.
 */
@Service
public final class PlatformSignalProvider {

  // visible for testing only
  final Map<UUID, Map<String, PlatformSignal>> signalsByPointAndId;

  @Autowired
  public PlatformSignalProvider(
    @Nonnull ObjectMapper objectMapper,
    @Nonnull @Value("classpath:data/signals.json") Resource signalsResource
  ) throws IOException {
    // deserialize the signals from the signals json bundled in the application jar file
    // and map them in the format point id -> signal id -> signal
    var signalListType = objectMapper.getTypeFactory().constructCollectionType(List.class, PlatformSignal.class);
    try (var inputStream = signalsResource.getInputStream()) {
      this.signalsByPointAndId = new HashMap<>();
      List<PlatformSignal> signals = objectMapper.readValue(inputStream, signalListType);
      for (var signal : signals) {
        var signalMapping = this.signalsByPointAndId.computeIfAbsent(signal.getPointId(), _ -> new HashMap<>());
        signalMapping.put(signal.getSignalId(), signal);
      }
    }
  }

  /**
   * Finds the information about a single signal by the point where the signal should be located and the id of the
   * signal to get.
   *
   * @param pointId  the id of the point where the signal to get should be located.
   * @param signalId the id of the signal to get the information of.
   * @return an optional holding the signal info of the point and signal id, if one exists.
   */
  public @Nonnull Optional<PlatformSignal> findSignalInfo(@Nonnull UUID pointId, @Nonnull String signalId) {
    return Optional
      .ofNullable(this.signalsByPointAndId.get(pointId))
      .map(signalIdToSignalMapping -> signalIdToSignalMapping.get(signalId));
  }
}
