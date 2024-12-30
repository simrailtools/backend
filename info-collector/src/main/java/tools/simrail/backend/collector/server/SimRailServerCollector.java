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

package tools.simrail.backend.collector.server;

import jakarta.annotation.Nonnull;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.simrail.backend.common.server.SimRailServerEntity;
import tools.simrail.backend.common.server.SimRailServerRegion;
import tools.simrail.backend.common.server.SimRailServerRepository;
import tools.simrail.backend.common.util.MongoIdDecodeUtil;
import tools.simrail.backend.common.util.UuidV5Factory;
import tools.simrail.backend.external.sraws.SimRailAwsApiClient;
import tools.simrail.backend.external.srpanel.SimRailPanelApiClient;

@Service
public final class SimRailServerCollector implements SimRailServerService {

  private static final Logger LOGGER = LoggerFactory.getLogger(SimRailServerCollector.class);
  // https://regex101.com/r/8kPxyF/2
  private static final Pattern SERVER_NAME_REGEX = Pattern.compile("^.+ \\((?<lang>.+)\\) ?(\\[(?<tags>.+)])?$");

  private final UuidV5Factory serverIdFactory;
  private final SimRailAwsApiClient awsApiClient;
  private final SimRailPanelApiClient panelApiClient;
  private final ServerUpdateHandler serverUpdateHandler;
  private final SimRailServerRepository serverRepository;

  private long collectionRuns = 0;
  private volatile List<SimRailServerDescriptor> collectedServers = List.of();

  @Autowired
  SimRailServerCollector(
    @Nonnull SimRailAwsApiClient awsApiClient,
    @Nonnull SimRailPanelApiClient panelApiClient,
    @Nonnull SimRailServerRepository serverRepository,
    @Nonnull ServerUpdateHandler serverUpdateHandler
  ) {
    this.awsApiClient = awsApiClient;
    this.panelApiClient = panelApiClient;
    this.serverRepository = serverRepository;
    this.serverUpdateHandler = serverUpdateHandler;
    this.serverIdFactory = new UuidV5Factory(SimRailServerEntity.ID_NAMESPACE);
  }

  /**
   * Collects the information of all SimRail servers every 30 seconds.
   */
  @Scheduled(initialDelay = 0, fixedRate = 30, timeUnit = TimeUnit.SECONDS, scheduler = "server_collect_scheduler")
  public void collectServerInformation() {
    // collect further information about the server, such as the timezone
    // on every second collection run (every 60 seconds)
    var fullCollection = this.collectionRuns++ % 2 == 0;

    var response = this.panelApiClient.getServers();
    var servers = response.getEntries();
    if (!response.isSuccess() || servers == null || servers.isEmpty()) {
      LOGGER.warn("API did not return a successful result while getting server list");
      return;
    }

    var foundServers = new ArrayList<SimRailServerDescriptor>();
    for (var server : servers) {
      // get or create the server entity, store the original variable information
      var serverEntity = this.serverRepository.findByForeignId(server.getId()).orElseGet(() -> {
        var newServer = new SimRailServerEntity();
        newServer.setNew(true);
        newServer.setForeignId(server.getId());
        newServer.setTimezone(ZoneOffset.UTC.getId());
        newServer.setId(this.serverIdFactory.create(server.getCode() + server.getId()));
        return newServer;
      });
      var originalOnline = serverEntity.isOnline();
      var originalTimezoneId = serverEntity.getTimezone();

      // update the base information
      serverEntity.setCode(server.getCode());
      serverEntity.setOnline(server.isOnline());

      // update the time when the server was initially registered in the SimRail backend
      var registeredSince = MongoIdDecodeUtil.parseMongoId(server.getId());
      serverEntity.setRegisteredSince(registeredSince);

      // update the server region
      var region = switch (server.getRegion()) {
        case ASIA -> SimRailServerRegion.ASIA;
        case EUROPE -> SimRailServerRegion.EUROPE;
        case US_NORTH -> SimRailServerRegion.US_NORTH;
      };
      serverEntity.setRegion(region);

      // update spoken language and tags
      var matcher = SERVER_NAME_REGEX.matcher(server.getName());
      if (matcher.matches()) {
        // get the server language, set it to null if the server is international as no specific language is spoken
        var lang = matcher.group("lang").strip();
        var spokenLang = lang.equals("International") ? null : lang;
        serverEntity.setSpokenLanguage(spokenLang);

        // update the tags of the server
        var tags = Objects.requireNonNullElse(matcher.group("tags"), "");
        var tagList = Arrays.stream(tags.split(","))
          .map(String::strip)
          .filter(tag -> !tag.isEmpty())
          .toList();
        var currentTags = serverEntity.getTags();
        if (!tagList.equals(currentTags)) {
          serverEntity.setTags(tagList);
        }
      } else {
        LOGGER.warn("Cannot parse language and/or tags from server name {}", server.getName());
        continue;
      }

      ZoneOffset serverZoneOffset = null;
      if (fullCollection) {
        // collect the server timezone identifier
        var serverUtcOffset = this.awsApiClient.getServerTimeOffset(server.getCode());
        serverZoneOffset = ZoneOffset.ofHours(serverUtcOffset);
        serverEntity.setTimezone(serverZoneOffset.getId());
      }

      // save the entity and register it as discovered during the run if we did a full collection
      var savedEntity = this.serverRepository.save(serverEntity);
      if (serverZoneOffset != null) {
        var serverDescriptor = new SimRailServerDescriptor(
          savedEntity.getId(),
          server.getId(),
          server.getCode(),
          serverZoneOffset);
        foundServers.add(serverDescriptor);
      }

      // publish a possible change to all listeners
      if (serverEntity.isNew()) {
        this.serverUpdateHandler.handleServerAdd(savedEntity);
      } else {
        this.serverUpdateHandler.handleServerUpdate(originalOnline, originalTimezoneId, savedEntity);
      }
    }

    if (fullCollection) {
      // mark all servers which are not included in the response as deleted
      var updatedServerIds = foundServers.stream().map(SimRailServerDescriptor::id).toList();
      var missingServers = this.serverRepository.findAllByIdNotInAndNotDeleted(updatedServerIds);
      for (var missingServer : missingServers) {
        missingServer.setOnline(false);
        missingServer.setDeleted(true);
        this.serverRepository.save(missingServer);
        this.serverUpdateHandler.handleServerRemove(missingServer);
      }

      // update the found servers during the run to make them available to readers
      this.collectedServers = foundServers;
    }
  }

  @Override
  public @Nonnull List<SimRailServerDescriptor> getServers() {
    return this.collectedServers;
  }

  @Override
  public @Nonnull Optional<SimRailServerDescriptor> findServerByIntId(@Nonnull UUID id) {
    return this.collectedServers.stream().filter(desc -> desc.id().equals(id)).findFirst();
  }

  @Override
  public @Nonnull Optional<SimRailServerDescriptor> findServerBySimRailId(@Nonnull String id) {
    return this.collectedServers.stream().filter(desc -> desc.foreignId().equals(id)).findFirst();
  }

  @Override
  public @Nonnull Optional<SimRailServerDescriptor> findServerByCode(@Nonnull String code) {
    return this.collectedServers.stream().filter(desc -> desc.code().equals(code)).findFirst();
  }
}
