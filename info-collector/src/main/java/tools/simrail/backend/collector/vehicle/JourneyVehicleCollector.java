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

package tools.simrail.backend.collector.vehicle;

import feign.FeignException;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Timer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.simrail.backend.collector.journey.JourneyIdService;
import tools.simrail.backend.collector.server.SimRailServerDescriptor;
import tools.simrail.backend.collector.server.SimRailServerService;
import tools.simrail.backend.common.railcar.RailcarProvider;
import tools.simrail.backend.common.util.StringUtils;
import tools.simrail.backend.common.vehicle.JourneyVehicle;
import tools.simrail.backend.common.vehicle.JourneyVehicleLoad;
import tools.simrail.backend.common.vehicle.JourneyVehicleSequenceEntity;
import tools.simrail.backend.common.vehicle.JourneyVehicleStatus;
import tools.simrail.backend.external.sraws.SimRailAwsApiClient;
import tools.simrail.backend.external.sraws.model.SimRailAwsTrainRun;
import tools.simrail.backend.external.srpanel.SimRailPanelApiClient;
import tools.simrail.backend.external.srpanel.model.SimRailPanelTrain;

@Component
class JourneyVehicleCollector {

  private static final int BATCH_SIZE = 100;
  private static final Logger LOGGER = LoggerFactory.getLogger(JourneyVehicleCollector.class);

  private final SimRailAwsApiClient awsApiClient;
  private final SimRailPanelApiClient panelApiClient;

  private final RailcarProvider railcarProvider;
  private final JourneyIdService journeyIdService;
  private final SimRailServerService serverService;
  private final TransactionTemplate transactionTemplate;
  private final CollectorVehicleRepository vehicleRepository;

  private final Meter.MeterProvider<Timer> actualCompositionCollectTimer;
  private final Meter.MeterProvider<Timer> predictedCompositionCollectTimer;

  @Autowired
  JourneyVehicleCollector(
    @NonNull SimRailAwsApiClient awsApiClient,
    @NonNull SimRailPanelApiClient panelApiClient,
    @NonNull RailcarProvider railcarProvider,
    @NonNull JourneyIdService journeyIdService,
    @NonNull SimRailServerService serverService,
    @NonNull TransactionTemplate transactionTemplate,
    @NonNull CollectorVehicleRepository vehicleRepository,
    @Qualifier("actual_vc_collect_duration") Meter.@NonNull MeterProvider<Timer> actualCompositionCollectTimer,
    @Qualifier("predicted_vc_collect_duration") Meter.@NonNull MeterProvider<Timer> predictedCompositionCollectTimer
  ) {
    this.awsApiClient = awsApiClient;
    this.panelApiClient = panelApiClient;
    this.railcarProvider = railcarProvider;
    this.journeyIdService = journeyIdService;
    this.serverService = serverService;
    this.transactionTemplate = transactionTemplate;
    this.vehicleRepository = vehicleRepository;
    this.actualCompositionCollectTimer = actualCompositionCollectTimer;
    this.predictedCompositionCollectTimer = predictedCompositionCollectTimer;
  }

  /**
   * Generates the resolve key for a vehicle sequence.
   *
   * @param serverId       the id of the server where the journey is on.
   * @param trainNum       the number of the train that is associated with the sequence.
   * @param firstEventTime the time of the first event of the journey.
   * @return a resolve key for a journey based on the given parameters.
   */
  private static @NonNull String generateResolveKey(
    @NonNull UUID serverId,
    @NonNull String trainNum,
    @NonNull LocalDateTime firstEventTime
  ) {
    return serverId + trainNum + firstEventTime;
  }

  @Scheduled(initialDelay = 1, fixedDelay = 5, timeUnit = TimeUnit.MINUTES, scheduler = "vehicle_collect_scheduler")
  public void collectJourneyVehicles() {
    // collect the scheduled/predicted vehicle compositions first
    var servers = this.serverService.getServers();
    for (var server : servers) {
      var span = Timer.start();
      try {
        var runs = this.awsApiClient.getTrainRuns(server.code());
        if (runs != null && !runs.isEmpty()) {
          this.transactionTemplate.executeWithoutResult(_ -> this.collectVehiclesFromTimetable(server, runs));
        }
      } catch (FeignException _) {
        // ignore upstream api issues
      } finally {
        var timer = this.predictedCompositionCollectTimer.withTag("server_code", server.code());
        span.stop(timer);
      }
    }

    // collect the real vehicle compositions
    for (var server : servers) {
      var span = Timer.start();
      try {
        var response = this.panelApiClient.getTrains(server.code(), null).body();
        var trains = response != null ? response.getEntries() : null;
        if (trains != null && !trains.isEmpty()) {
          this.transactionTemplate.executeWithoutResult(_ -> this.collectVehiclesFromLiveData(server, trains));
        }
      } catch (FeignException _) {
        // ignore upstream api issues
      } finally {
        var timer = this.actualCompositionCollectTimer.withTag("server_code", server.code());
        span.stop(timer);
      }
    }
  }

  /**
   * Pre-inserts the vehicles for a journey based on the timetable and previously collected vehicle data.
   *
   * @param server    the server on which the vehicles should be pre-collected.
   * @param trainRuns the runs that are planned on the given server.
   */
  private void collectVehiclesFromTimetable(
    @NonNull SimRailServerDescriptor server,
    @NonNull List<SimRailAwsTrainRun> trainRuns
  ) {
    // resolve the journeys that already have a vehicle mapping
    var journeyEntries = trainRuns.stream()
      .filter(run -> !run.getTimetable().isEmpty())
      .map(run -> {
        var firstTimetableEntry = run.getTimetable().getFirst();
        var journeyId = this.journeyIdService.generateJourneyId(server.id(), run.getRunId());
        return Map.entry(journeyId, firstTimetableEntry);
      })
      .collect(Collectors.toMap(Map.Entry::getKey, Function.identity()));
    if (journeyEntries.isEmpty()) {
      return;
    }

    var newSequences = new ArrayList<JourneyVehicleSequenceEntity>();
    var journeyEntriesList = List.copyOf(journeyEntries.values()); // copy to list for simplicity
    for (var startIdx = 0; startIdx < journeyEntriesList.size(); startIdx += BATCH_SIZE) {
      var endIdx = Math.min(startIdx + BATCH_SIZE, journeyEntriesList.size());
      var journeyBatch = journeyEntriesList.subList(startIdx, endIdx);

      // find all the ids of the journeys that don't have an associated journey sequence yet
      var journeyIds = journeyBatch.stream().map(Map.Entry::getKey).toArray(UUID[]::new);
      var journeyIdsWithoutSequence = this.vehicleRepository.findMissingJourneyIds(journeyIds);
      if (journeyIdsWithoutSequence.isEmpty()) {
        continue;
      }

      // build a mapping for previous resolve key -> resolving journey id
      var journeyByPreviousResolveKey = new HashMap<String, Map.Entry<UUID, String>>();
      for (var journeyId : journeyIdsWithoutSequence) {
        var entry = journeyEntries.get(journeyId);
        var timetableEntry = entry.getValue();
        var timetableEntryTime = Objects.requireNonNullElse(
          timetableEntry.getArrivalTime(),
          timetableEntry.getDepartureTime());
        var previousDayTime = timetableEntryTime.minusDays(1);
        var prevResolveKey = generateResolveKey(server.id(), timetableEntry.getTrainNumber(), previousDayTime);
        var currResolveKey = generateResolveKey(server.id(), timetableEntry.getTrainNumber(), timetableEntryTime);
        journeyByPreviousResolveKey.put(prevResolveKey, Map.entry(journeyId, currResolveKey));
      }

      // try to resolve the previous vehicle sequences by the resolved resolve keys
      var previousSequences = this.vehicleRepository.findAllBySequenceResolveKeyIn(journeyByPreviousResolveKey.keySet())
        .stream()
        .collect(Collectors.toMap(JourneyVehicleSequenceEntity::getSequenceResolveKey, Function.identity(), (l, r) -> {
          var leftUpdateTime = l.getUpdateTime();
          var rightUpdateTime = r.getUpdateTime();
          var leftIsNewer = leftUpdateTime.compareTo(rightUpdateTime) >= 0;
          return leftIsNewer ? l : r;
        }));
      for (var entry : journeyByPreviousResolveKey.entrySet()) {
        var journeyId = entry.getValue().getKey();
        var resolveKey = entry.getValue().getValue();
        var previousSequence = previousSequences.get(entry.getKey());

        // build the base sequence data
        var sequence = new JourneyVehicleSequenceEntity();
        sequence.setJourneyId(journeyId);
        sequence.setSequenceResolveKey(resolveKey);

        if (previousSequence == null) {
          // if no previous sequence is known, just mark the sequence status as unknown
          sequence.setStatus(JourneyVehicleStatus.UNKNOWN);
          sequence.setVehicles(Set.of());
        } else {
          // previous sequence is known, copy over the vehicles
          sequence.setStatus(JourneyVehicleStatus.PREDICTION);
          sequence.setVehicles(previousSequence.getVehicles());
        }

        // add the sequence for storing
        newSequences.add(sequence);
      }
    }

    this.vehicleRepository.saveAll(newSequences);
  }

  /**
   * Saves the actual vehicle information for all active journeys of a server.
   *
   * @param server       the server information on which the journeys are running.
   * @param activeTrains the trains that are actively running on the given server.
   */
  private void collectVehiclesFromLiveData(
    @NonNull SimRailServerDescriptor server,
    @NonNull List<SimRailPanelTrain> activeTrains
  ) {
    // map the trains to a journey id -> vehicles mapping, then resolve the unconfirmed vehicle sequences
    var vehiclesByJourneyId = activeTrains.stream()
      .map(train -> {
        var journeyId = this.journeyIdService.generateJourneyId(server.id(), train.getRunId());
        return Map.entry(journeyId, train.getVehicles());
      })
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    var unconfirmedSequences = this.vehicleRepository.findAllUnconfirmedByJourneyIdIn(vehiclesByJourneyId.keySet());
    if (unconfirmedSequences.isEmpty()) {
      return;
    }

    for (var sequence : unconfirmedSequences) {
      // parse the vehicle data, which is either a single string (no brake setting/load)
      // or multiple parts separated by a colon (':'), the last part might hold the weight and load
      // for example:
      // "Dragon2/ET25-002" - just the ET25-002 locomotive, no further data supplied
      // "412W/412W_33515356394-5" - just a wagon, no further data supplied
      // "408S/408S:P:50" - wagon, brake setting P, 50 tons loaded (unknown load)
      // "441V/441V_31516635283-3:P:43@Coal" - wagon, brake setting P, 43 tons of coal loaded
      var vehicles = vehiclesByJourneyId.get(sequence.getJourneyId());
      var convertedVehicles = new HashSet<JourneyVehicle>();
      for (var index = 0; index < vehicles.size(); index++) {
        // get the railcar associated with the vehicle, skip the vehicle if the railcar is unknown
        var vehicleData = vehicles.get(index);
        var vehicleDataParts = vehicleData.split(":");
        var vehicleRailcar = this.railcarProvider.findRailcarByApiId(vehicleDataParts[0]).orElse(null);
        if (vehicleRailcar == null) {
          LOGGER.warn("Found vehicle with no associated railcar: {}", vehicleDataParts[0]);
          continue;
        }

        // create base information about the vehicle & register it
        var vehicle = new JourneyVehicle();
        vehicle.setIndexInSequence(index);
        vehicle.setRailcarId(vehicleRailcar.getId());
        convertedVehicles.add(vehicle);

        if (vehicleDataParts.length == 2) {
          // workaround for an upstream api issue, where the brake regime and load weight are merged
          // for example: "629Z/434Z_31514553133-5:G39@RandomContainer2x20"
          // brake regime G and load weight 39 are in one part rather than being split up into two
          // this code will also trigger for e.g. locomotives "4E/EU07-193:G:", therefore we have to check
          // if the brake regime part is longer than one char and suffixed with numbers
          var brakeRegimePart = vehicleDataParts[1];
          if (brakeRegimePart.length() > 1) {
            var firstChar = brakeRegimePart.charAt(0); // brake regime, can be P, G & R
            var secondChar = brakeRegimePart.charAt(1); // might be a number following the brake regime
            if ((firstChar == 'P' || firstChar == 'G' || firstChar == 'R')
              && (secondChar >= '0' && secondChar <= '9')) {
              var newDataParts = new String[3];
              newDataParts[0] = vehicleDataParts[0]; // vehicle api identifier
              newDataParts[1] = Character.toString(firstChar); // vehicle brake regime
              newDataParts[2] = brakeRegimePart.substring(1); // load weight & load part, without brake regime
              vehicleDataParts = newDataParts;
            }
          }
        }

        if (vehicleDataParts.length == 3) {
          // fully featured vehicle data, parse load weight and type as well
          var vehicleLoadInfo = vehicleDataParts[2].split("@");
          var vehicleLoadWeight = StringUtils.parseIntSafe(vehicleLoadInfo[0]);
          if (vehicleLoadInfo.length == 2) {
            // load type is also included
            var loadType = JourneyVehicleLoad.mapApiLoadType(vehicleLoadInfo[1]);
            vehicle.setLoad(loadType);
            vehicle.setLoadWeight(vehicleLoadWeight);
          } else {
            // only the load weight is included
            vehicle.setLoadWeight(vehicleLoadWeight);
          }
        }
      }

      // set the vehicles of the sequence, mark the sequence status as REAL
      sequence.setVehicles(convertedVehicles);
      sequence.setStatus(JourneyVehicleStatus.REAL);
    }

    this.vehicleRepository.saveAll(unconfirmedSequences);
  }
}
