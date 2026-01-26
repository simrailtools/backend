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

package tools.simrail.backend.collector.vehicle;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tools.simrail.backend.common.vehicle.JourneyVehicleSequenceEntity;
import tools.simrail.backend.common.vehicle.JourneyVehicleSequenceRepository;

/**
 * Repository with specialized information for the collector.
 */
interface CollectorVehicleRepository extends JourneyVehicleSequenceRepository {

  /**
   * Finds the subset of the given journey ids that don't have an associated vehicle sequence.
   *
   * @param journeyIds the ids to find the subset of not stored ids of.
   * @return a subset of the given collection holding all ids that don't have an associated vehicle sequence.
   */
  // "Condition 'vs.journey_id IS NULL' is always 'false'" is actually wrong, don't listen to it ¯\_(ツ)_/¯
  @Query(value = """
    SELECT j.journey_id
    FROM unnest(cast(:journeyIds as uuid[])) AS j(journey_id)
    WHERE EXISTS (SELECT 1 FROM sit_journey sj WHERE sj.id = j.journey_id)
    AND NOT EXISTS (SELECT 1 FROM sit_journey_vehicle_sequence vs WHERE vs.journey_id = j.journey_id)
    """, nativeQuery = true)
  List<UUID> findMissingJourneyIds(@Param("journeyIds") UUID[] journeyIds);

  /**
   * Finds all unconfirmed vehicle sequences whose associated journey is in the given collection of journey ids.
   *
   * @param journeyIds the ids of the journeys to find the unconfirmed vehicle sequences of.
   * @return the unconfirmed vehicle sequences that are associated with one of the given journeys.
   */
  @Query(value = """
    SELECT *
    FROM sit_journey_vehicle_sequence
    WHERE journey_id IN (:journeyIds) AND status <> 'REAL'
    """, nativeQuery = true)
  List<JourneyVehicleSequenceEntity> findAllUnconfirmedByJourneyIdIn(@Param("journeyIds") Collection<UUID> journeyIds);

  /**
   * Finds all the vehicle sequences based on the given sequence resolve keys.
   *
   * @param sequenceResolveKeys the sequence keys to find the sequences of.
   * @return the sequences associated with one of the given sequence resolve keys.
   */
  @NonNull
  List<JourneyVehicleSequenceEntity> findAllBySequenceResolveKeyIn(@NonNull Collection<String> sequenceResolveKeys);
}
