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

package tools.simrail.backend.collector.vehicle;

import jakarta.annotation.Nonnull;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.simrail.backend.collector.server.SimRailServerDescriptor;
import tools.simrail.backend.collector.server.SimRailServerService;
import tools.simrail.backend.common.concurrent.TransactionalFailShutdownTaskScopeFactory;
import tools.simrail.backend.common.railcar.RailcarProvider;
import tools.simrail.backend.common.vehicle.JourneyVehicle;
import tools.simrail.backend.common.vehicle.JourneyVehicleLoad;
import tools.simrail.backend.common.vehicle.JourneyVehicleStatus;
import tools.simrail.backend.external.sraws.SimRailAwsApiClient;
import tools.simrail.backend.external.sraws.model.SimRailAwsTrainRun;
import tools.simrail.backend.external.srpanel.SimRailPanelApiClient;
import tools.simrail.backend.external.srpanel.model.SimRailPanelTrain;

@Component
class JourneyVehicleCollector {

  private static final Logger LOGGER = LoggerFactory.getLogger(JourneyVehicleCollector.class);

  private final SimRailAwsApiClient awsApiClient;
  private final SimRailPanelApiClient panelApiClient;

  private final RailcarProvider railcarProvider;
  private final SimRailServerService serverService;
  private final CollectorVehicleRepository vehicleRepository;
  private final TransactionalFailShutdownTaskScopeFactory transactionalTaskScopeFactory;

  @Autowired
  public JourneyVehicleCollector(
    @Nonnull SimRailAwsApiClient awsApiClient,
    @Nonnull SimRailPanelApiClient panelApiClient,
    @Nonnull RailcarProvider railcarProvider,
    @Nonnull SimRailServerService serverService,
    @Nonnull CollectorVehicleRepository vehicleRepository,
    @Nonnull TransactionalFailShutdownTaskScopeFactory transactionalTaskScopeFactory
  ) {
    this.awsApiClient = awsApiClient;
    this.panelApiClient = panelApiClient;
    this.railcarProvider = railcarProvider;
    this.serverService = serverService;
    this.vehicleRepository = vehicleRepository;
    this.transactionalTaskScopeFactory = transactionalTaskScopeFactory;
  }

  @Transactional
  @Scheduled(initialDelay = 1, fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
  public void collectJourneyVehicles() throws InterruptedException {
    // collect the scheduled/predicted vehicle compositions first
    var servers = this.serverService.getServers();
    try (var scope = this.transactionalTaskScopeFactory.get()) {
      for (var server : servers) {
        scope.fork(() -> {
          var startTime = Instant.now();
          var runs = this.awsApiClient.getTrainRuns(server.code());
          this.collectVehiclesFromTimetable(server, runs);
          var elapsedTime = Duration.between(startTime, Instant.now()).toSeconds();
          LOGGER.info("Collected predicted vehicle information for server {} in {}s", server.code(), elapsedTime);
          return null;
        });
      }

      // wait for all update tasks to complete, log the first exception if one occurred
      scope.join();
      scope.firstException().ifPresent(throwable -> LOGGER.warn("Exception collecting vehicle info", throwable));
    }

    // collect the real vehicle compositions
    for (var server : servers) {
      var startTime = Instant.now();
      var response = this.panelApiClient.getTrains(server.code());
      var activeTrains = response.getEntries();
      if (!response.isSuccess() || activeTrains == null || activeTrains.isEmpty()) {
        LOGGER.warn("SimRail api returned no active trains for server {}", server.code());
        return;
      }

      this.collectVehiclesFromLiveData(server, activeTrains);
      var elapsedTime = Duration.between(startTime, Instant.now()).toSeconds();
      LOGGER.info("Collected actual vehicle information for server {} in {}s", server.code(), elapsedTime);
    }
  }

  /**
   * Pre-inserts the vehicles for a journey based on the timetable and previously collected vehicle data.
   *
   * @param server    the server on which the vehicles should be pre-collected.
   * @param trainRuns the runs that are planned on the given server.
   */
  private void collectVehiclesFromTimetable(
    @Nonnull SimRailServerDescriptor server,
    @Nonnull List<SimRailAwsTrainRun> trainRuns
  ) {
    // resolve the journeys that already have a vehicle mapping
    var runIds = trainRuns.stream().map(SimRailAwsTrainRun::getRunId).toList();
    var journeysWithoutComposition = this.vehicleRepository.findJourneyRunsWithoutVehicleComposition(
      server.id(),
      runIds);
    var runIdToJourneyIdMapping = journeysWithoutComposition
      .stream()
      .collect(Collectors.toMap(entry -> (UUID) entry[1], entry -> (UUID) entry[0]));
    for (var run : trainRuns) {
      // resolve the journey id associated with the run, if the run id is not present in the
      // mapping it either means that the journey is not yet registered or a composition is already stored
      var journeyId = runIdToJourneyIdMapping.get(run.getRunId());
      if (journeyId == null) {
        continue;
      }

      // find the previous vehicles (previous day) and insert a predicted vehicle composition
      var firstEvent = run.getTimetable().getFirst();
      var firstEventTime = OffsetDateTime.of(firstEvent.getDepartureTime(), server.timezoneOffset());
      var previousEventDate = firstEventTime.minusDays(1).toLocalDate();
      var previousVehicles = this.vehicleRepository.findJourneyVehiclesByJourneyOnSpecificDate(
        server.id(),
        firstEvent.getTrainType(),
        firstEvent.getTrainNumber(),
        previousEventDate);
      if (previousVehicles.isEmpty()) {
        var unknownVehicle = new JourneyVehicle();
        unknownVehicle.setIndexInGroup(0);
        unknownVehicle.setJourneyId(journeyId);
        unknownVehicle.setStatus(JourneyVehicleStatus.UNKNOWN);
        this.saveJourneyVehicles(journeyId, List.of(unknownVehicle));
      } else {
        var currentEventVehicles = previousVehicles.stream()
          .map(vehicle -> {
            var newVehicle = new JourneyVehicle();
            newVehicle.setJourneyId(journeyId);
            newVehicle.setStatus(JourneyVehicleStatus.PREDICTION);
            newVehicle.setIndexInGroup(vehicle.getIndexInGroup());
            newVehicle.setRailcarId(vehicle.getRailcarId());
            newVehicle.setLoadWeight(vehicle.getLoadWeight());
            newVehicle.setLoad(vehicle.getLoad());
            return newVehicle;
          })
          .toList();
        this.saveJourneyVehicles(journeyId, currentEventVehicles);
      }
    }
  }

  /**
   * Saves the actual vehicle information for all active journeys of a server.
   *
   * @param server       the server information on which the journeys are running.
   * @param activeTrains the trains that are actively running on the given server.
   */
  private void collectVehiclesFromLiveData(
    @Nonnull SimRailServerDescriptor server,
    @Nonnull List<SimRailPanelTrain> activeTrains
  ) {
    var runIds = activeTrains.stream().map(SimRailPanelTrain::getRunId).toList();
    var runsWithoutConfirmedComposition = this.vehicleRepository.findJourneyRunsWithoutConfirmedVehicleComposition(
      server.id(),
      runIds);
    var runIdToJourneyIdMapping = runsWithoutConfirmedComposition
      .stream()
      .collect(Collectors.toMap(entry -> (UUID) entry[1], entry -> (UUID) entry[0]));
    for (var activeTrain : activeTrains) {
      // resolve the journey id associated with the run, if the run id is not present in the
      // mapping it either means that the journey is not yet registered or a composition is already stored
      var journeyId = runIdToJourneyIdMapping.get(activeTrain.getRunId());
      if (journeyId == null) {
        continue;
      }

      // parse the vehicle data, which is either a single string (no brake setting/load)
      // or multiple parts separated by a colon (':'), the last part might hold the weight and load
      // for example:
      // "Dragon2/ET25-002" - just the ET25-002 locomotive, no further data supplied
      // "412W/412W_33515356394-5" - just a wagon, no further data supplied
      // "408S/408S:P:50" - wagon, brake setting P, 50 tons loaded (unknown load)
      // "441V/441V_31516635283-3:P:43@Coal" - wagon, brake setting P, 43 tons of coal loaded
      var vehicles = activeTrain.getVehicles();
      var convertedVehicles = new ArrayList<JourneyVehicle>();
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
        vehicle.setIndexInGroup(index);
        vehicle.setJourneyId(journeyId);
        vehicle.setStatus(JourneyVehicleStatus.REAL);
        vehicle.setRailcarId(vehicleRailcar.getId());
        convertedVehicles.add(vehicle);

        if (vehicleDataParts.length == 3) {
          // fully featured vehicle data, parse load weight and type as well
          var vehicleLoadInfo = vehicleDataParts[2].split("@");
          var vehicleLoadWeight = Integer.parseInt(vehicleLoadInfo[0]);
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

      // register the vehicles for the journey
      this.saveJourneyVehicles(journeyId, convertedVehicles);
    }
  }

  /**
   * Saves the vehicles for the given journey, deleting the previously stored vehicle data.
   *
   * @param journeyId the id of the journey for which the vehicles should be stored.
   * @param vehicles  the new vehicle information for the journey.
   */
  private void saveJourneyVehicles(@Nonnull UUID journeyId, @Nonnull List<JourneyVehicle> vehicles) {
    this.vehicleRepository.deleteAllByJourneyId(journeyId);
    this.vehicleRepository.saveAll(vehicles);
  }
}
