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

package tools.simrail.backend.common.railcar;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * The provider for all railcars that are available in SimRail.
 */
@Service
public final class RailcarProvider {

  // visible for testing only
  final List<Railcar> railcars;

  @Autowired
  public RailcarProvider(
    @NonNull ObjectMapper objectMapper,
    @NonNull @Value("classpath:data/railcars.json") Resource railcarsResource
  ) throws IOException {
    // deserialize the railcars from the railcars json bundled in the application jar file
    var railcarListType = objectMapper.getTypeFactory().constructCollectionType(List.class, Railcar.class);
    try (var inputStream = railcarsResource.getInputStream()) {
      this.railcars = objectMapper.readValue(inputStream, railcarListType);
    }
  }

  /**
   * Finds a single railcar by the given internal railcar id.
   *
   * @param id the internal id of the railcar.
   * @return an optional holding the railcar with the given internal id, if one exists.
   */
  public @NonNull Optional<Railcar> findRailcarById(@NonNull UUID id) {
    return this.railcars.stream().filter(railcar -> railcar.getId().equals(id)).findFirst();
  }

  /**
   * Finds a single railcar by the id used by the SimRail api.
   *
   * @param id the id of the railcar used by the SimRail api.
   * @return an optional holding the railcar with the given SimRail api id, if one exists.
   */
  public @NonNull Optional<Railcar> findRailcarByApiId(@NonNull String id) {
    return this.railcars.stream().filter(railcar -> railcar.getApiId().equals(id)).findFirst();
  }

  /**
   * Finds all railcars with the given railcar type.
   *
   * @param type the type of the railcars to get.
   * @return all railcars with the given railcar type.
   */
  public @NonNull List<Railcar> findRailcarsByType(@NonNull RailcarType type) {
    return this.railcars.stream().filter(railcar -> railcar.getRailcarType() == type).toList();
  }
}
