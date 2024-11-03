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
  private final SimRailServerRepository serverRepository;

  private long collectionRuns = 0;
  private volatile List<SimRailServerDescriptor> collectedServers = List.of();

  @Autowired
  public SimRailServerCollector(SimRailServerRepository serverRepository) {
    this.serverRepository = serverRepository;
    this.awsApiClient = SimRailAwsApiClient.create();
    this.panelApiClient = SimRailPanelApiClient.create();
    this.serverIdFactory = new UuidV5Factory(SimRailServerEntity.ID_NAMESPACE);
  }

  /**
   * Collects the information of all SimRail servers every 30 seconds.
   */
  @Scheduled(initialDelay = 0, fixedRate = 30, timeUnit = TimeUnit.SECONDS)
  public void collectServerInformation() {
    // collect further information about the server, such as the timezone
    // on every second collection run (every 60 seconds)
    var fullCollection = this.collectionRuns++ % 2 == 0;

    var response = this.panelApiClient.getServers();
    if (!response.isSuccess()) {
      LOGGER.warn("API did not return a successful result while getting server list");
      return;
    }

    var servers = response.getEntries();
    var foundServers = new ArrayList<SimRailServerDescriptor>();
    for (var server : servers) {
      // get or create the server entity & update the base information
      var serverEntity = this.serverRepository.findByForeignId(server.getId()).orElseGet(() -> {
        var newServer = new SimRailServerEntity();
        newServer.setForeignId(server.getId());
        newServer.setTimezone(ZoneOffset.UTC.getId());
        newServer.setId(this.serverIdFactory.create(server.getCode() + server.getId()));
        return newServer;
      });
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

      if (fullCollection) {
        // collect the server timezone identifier
        var serverUtcOffset = this.awsApiClient.getServerTimeOffset(server.getCode());
        var serverTimezoneId = ZoneOffset.ofHours(serverUtcOffset).getId();
        serverEntity.setTimezone(serverTimezoneId);
      }

      // save the entity and register it as discovered during the run
      var savedEntity = this.serverRepository.save(serverEntity);
      var serverDescriptor = new SimRailServerDescriptor(savedEntity.getId(), server.getId(), server.getCode());
      foundServers.add(serverDescriptor);
    }

    // update the found servers during the run to make them available to readers
    this.collectedServers = foundServers;
  }

  @Override
  public @Nonnull List<SimRailServerDescriptor> getServers() {
    return this.collectedServers;
  }
}
