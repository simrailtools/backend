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

package tools.simrail.backend.api.point;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.simrail.backend.api.point.dto.PointInfoDto;

@Validated
@CrossOrigin
@RestController
@RequestMapping("/sit-points/v1/")
@Tag(name = "points-v1", description = "SimRail Point Data APIs (Version 1)")
class SimRailPointV1Controller {

  private final SimRailPointService pointService;

  @Autowired
  public SimRailPointV1Controller(@Nonnull SimRailPointService pointService) {
    this.pointService = pointService;
  }

  // /?countries={}&page={}&limit={}
  // by-id/{id}
  // by-ids
  // by-point-id/{id}
  // by-name/{query}?countries={}&limit={}
  // by-position?latitude={}&longitude={}&radius={}&countries={}&limit={}

  @GetMapping("/by-id/{id}")
  public @Nonnull ResponseEntity<PointInfoDto> findById(
    @PathVariable("id") @UUID(version = 4, allowNil = false) String id
  ) {
    return this.pointService.findPointById(java.util.UUID.fromString(id))
      .map(ResponseEntity::ok)
      .orElseGet(() -> ResponseEntity.notFound().build());
  }

  @GetMapping("/by-point-id/{id}")
  public @Nonnull ResponseEntity<PointInfoDto> findByPointId(
    @PathVariable("id") @Pattern(regexp = "[0-9]{2,4}") String pointId
  ) {
    return this.pointService.findPointByPointId(pointId)
      .map(ResponseEntity::ok)
      .orElseGet(() -> ResponseEntity.notFound().build());
  }
}
