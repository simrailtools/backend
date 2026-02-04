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

package tools.simrail.backend.collector.server;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.nats.client.Connection;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.simrail.backend.common.cache.DataCache;
import tools.simrail.backend.common.event.EventSubjectFactory;
import tools.simrail.backend.common.proto.EventBusProto;
import tools.simrail.backend.common.scenery.SimRailServerSceneryProvider;
import tools.simrail.backend.common.server.SimRailServerEntity;
import tools.simrail.backend.common.server.SimRailServerScenery;
import tools.simrail.backend.common.util.MonotonicInstantProvider;
import tools.simrail.backend.common.util.StringUtils;
import tools.simrail.backend.common.util.UuidV5Factory;
import tools.simrail.backend.external.sraws.SimRailAwsApiClient;
import tools.simrail.backend.external.srpanel.SimRailPanelApiClient;

@Service
public class SimRailServerCollector implements SimRailServerService {

  // https://regex101.com/r/8kPxyF/2
  private static final Pattern SERVER_NAME_REGEX = Pattern.compile("^.+ \\((?<lang>.+)\\) ?(\\[(?<tags>.+)])?$");

  private final Connection connection;
  private final UuidV5Factory serverIdFactory;
  private final SimRailAwsApiClient awsApiClient;
  private final SimRailPanelApiClient panelApiClient;
  private final CollectorServerService serverService;
  private final SimRailServerSceneryProvider sceneryProvider;

  private final Map<String, ServerUpdateHolder> updateHoldersByForeignId;
  private final DataCache<EventBusProto.ServerUpdateFrame> serverDataCache;

  private long collectionRuns = 0;
  private Instant lastDatabaseUpdate;
  private volatile List<SimRailServerDescriptor> collectedServers = List.of();

  @Autowired
  SimRailServerCollector(
    @NonNull Connection connection,
    @NonNull MeterRegistry meterRegistry,
    @NonNull SimRailAwsApiClient awsApiClient,
    @NonNull SimRailPanelApiClient panelApiClient,
    @NonNull CollectorServerService serverService,
    @NonNull SimRailServerSceneryProvider sceneryProvider,
    @NonNull @Qualifier("server_data_cache") DataCache<EventBusProto.ServerUpdateFrame> serverDataCache
  ) {
    this.connection = connection;
    this.awsApiClient = awsApiClient;
    this.panelApiClient = panelApiClient;
    this.serverService = serverService;
    this.sceneryProvider = sceneryProvider;
    this.serverDataCache = serverDataCache;

    this.serverIdFactory = new UuidV5Factory(SimRailServerEntity.ID_NAMESPACE);
    this.updateHoldersByForeignId = new ConcurrentHashMap<>(20, 0.9f, 1);

    Gauge.builder("cached_active_servers_total", () -> this.collectedServers.size())
      .description("Total number of cached active SimRail servers")
      .register(meterRegistry);
  }

  @PostConstruct
  public void reconstructUpdateHoldersFromCache() {
    this.serverDataCache.pullCacheFromStorage(); // re-init data cache from underlying storage
    var cachedValues = this.serverDataCache.cachedValuesSnapshot();
    for (var cachedValue : cachedValues) {
      var idHolder = cachedValue.getIds();
      var sid = UUID.fromString(idHolder.getServerId());
      var updateHolder = new ServerUpdateHolder(sid, idHolder.getForeignId());
      this.updateHoldersByForeignId.put(idHolder.getForeignId(), updateHolder);

      // reconstruct the update fields state from the journey data
      var data = cachedValue.getServerData();
      updateHolder.online.forceUpdateValue(data.getOnline());
      updateHolder.utcOffsetSeconds.forceUpdateValue(data.getUtcOffsetSeconds());
      updateHolder.tags.forceUpdateValue(new HashSet<>(data.getTagsList()));
      updateHolder.scenery.forceUpdateValue(StringUtils.decodeEnum(data.getScenery(), SimRailServerScenery.FULL));
      if (data.hasSpokenLanguage()) {
        updateHolder.spokenLanguage.forceUpdateValue(data.getSpokenLanguage());
      }
    }
  }

  /**
   * Collects the information of all SimRail servers every 30 seconds.
   */
  @Timed(value = "server_collect_seconds", description = "Elapsed time while collecting active server")
  @Scheduled(initialDelay = 0, fixedRate = 30, timeUnit = TimeUnit.SECONDS, scheduler = "server_collect_scheduler")
  public void collectServerInformation() throws Exception {
    // collect further information about the server, such as the timezone
    // on every second collection run (every 60 seconds)
    var fullCollection = this.collectionRuns++ % 2 == 0;

    // check if we should update the stored db data
    var now = Instant.now();
    var lastDbUpdate = this.lastDatabaseUpdate;
    var shouldUpdateDatabase = lastDbUpdate == null || Duration.between(lastDbUpdate, now).abs().toMinutes() >= 5;
    if (shouldUpdateDatabase) {
      this.lastDatabaseUpdate = now;
    }

    var response = this.panelApiClient.getServers();
    var servers = response.getEntries();
    if (!response.isSuccess() || servers == null || servers.isEmpty()) {
      return;
    }

    var foundServers = new ArrayList<SimRailServerDescriptor>();
    for (var index = 0; index < servers.size(); index++) {
      var server = servers.get(index);
      var updateHolder = this.updateHoldersByForeignId.get(server.getId());
      var mightBeNewServer = updateHolder == null;
      if (updateHolder == null) {
        var sid = this.serverIdFactory.create(server.getCode() + server.getId());
        updateHolder = new ServerUpdateHolder(sid, server.getId());
        this.updateHoldersByForeignId.put(server.getId(), updateHolder);
      }

      // update the server online status
      updateHolder.online.updateValue(server.isOnline());

      // update the scenery used on the server
      var scenery = this.sceneryProvider
        .findSceneryByServerId(server.getId())
        .orElse(SimRailServerScenery.WARSAW_KATOWICE_KRAKOW);
      updateHolder.scenery.updateValue(scenery);

      // update spoken language and tags
      var serverCode = server.getCode();
      var serverName = server.getName();
      if (serverCode.startsWith("xbx")) {
        // Xbox servers are named differently
        var nameParts = serverName.split(" ");
        var lang = nameParts.length >= 2 ? nameParts[1] : null;
        var spokenLang = lang == null || lang.equals("International") ? null : lang;
        updateHolder.spokenLanguage.updateValue(spokenLang);
        updateHolder.tags.updateValue(Set.of()); // Xbox servers don't have tags
      } else {
        var matcher = SERVER_NAME_REGEX.matcher(serverName);
        if (matcher.matches()) {
          // usual server name format: DE1 (Deutsch) [KEINE EVENTS]
          var lang = matcher.group("lang").strip();
          var spokenLang = serverCode.startsWith("int") || lang.equals("International") ? null : lang;
          updateHolder.spokenLanguage.updateValue(spokenLang);

          // tags are optional, check if they are present
          var tagPart = matcher.group("tags");
          if (tagPart == null) {
            updateHolder.tags.updateValue(Set.of());
          } else {
            var rawTags = tagPart.split(",");
            var tags = Arrays.stream(rawTags)
              .map(String::strip)
              .filter(tag -> !tag.isEmpty())
              .collect(Collectors.toSet());
            updateHolder.tags.updateValue(tags);
          }
        } else {
          // unusual name format, just assume international and no tags
          updateHolder.spokenLanguage.updateValue(null);
          updateHolder.tags.updateValue(Set.of());
        }
      }

      if (fullCollection || mightBeNewServer) {
        // collect the actual server timezone offset seconds
        var serverTimeResponse = this.awsApiClient.getServerTimeMillis(server.getCode());
        var serverZoneOffsetSeconds = ServerTimeUtil.calculateTimezoneOffsetSeconds(serverTimeResponse);

        // convert the collected utc offset seconds to utc offset hours and update it in the server entity
        if (serverZoneOffsetSeconds != null) {
          updateHolder.utcOffsetSeconds.updateValue(serverZoneOffsetSeconds);
        }
      }

      if (updateHolder.fieldGroup.consumeAnyDirty()) {
        var prevUpdateFrame = this.serverDataCache.findBySecondaryKey(updateHolder.foreignId);
        var updateFrameBuilder = switch (prevUpdateFrame) {
          case EventBusProto.ServerUpdateFrame frame -> frame.toBuilder();
          case null -> {
            var sidString = updateHolder.id.toString();
            var ids = EventBusProto.IdHolder.newBuilder()
              .setDataId(sidString)
              .setServerId(sidString)
              .setForeignId(updateHolder.foreignId)
              .build();
            yield EventBusProto.ServerUpdateFrame.newBuilder().setIds(ids);
          }
        };

        // apply the changed fields to the server data builder
        var serverDataBuilder = updateFrameBuilder.getServerData().toBuilder();
        updateHolder.online.ifDirty(serverDataBuilder::setOnline);
        updateHolder.utcOffsetSeconds.ifDirty(serverDataBuilder::setUtcOffsetSeconds);
        updateHolder.tags.ifDirty(tags -> serverDataBuilder.clearTags().addAllTags(tags));
        updateHolder.spokenLanguage.ifDirty(language -> {
          if (language == null) {
            serverDataBuilder.clearSpokenLanguage();
          } else {
            serverDataBuilder.setSpokenLanguage(language);
          }
        });
        updateHolder.scenery.ifDirty(serverScenery -> serverDataBuilder.setScenery(serverScenery.name()));

        // insert the server data into the cache
        var serverData = serverDataBuilder.build();
        var baseFrameData = EventBusProto.BaseFrameData.newBuilder()
          .setTimestamp(MonotonicInstantProvider.monotonicTimeMillis())
          .build();
        var updateFrame = updateFrameBuilder
          .setBaseData(baseFrameData)
          .setServerData(serverData)
          .build();
        this.serverDataCache.setCachedValue(updateFrame);

        // send out server update frame
        var subject = EventSubjectFactory.createServerUpdateSubjectV1(updateFrame.getIds().getServerId());
        this.connection.publish(subject, updateFrame.toByteArray());
      }

      // update the server data in the database if it's either a new server or for a periodical update
      if (mightBeNewServer || shouldUpdateDatabase) {
        this.serverService.saveServer(server, updateHolder);
      }

      // register a server descriptor for the server during a full collection
      if (fullCollection) {
        var utcOffsetSeconds = updateHolder.utcOffsetSeconds.currentValue();
        var sc = new SimRailServerDescriptor(updateHolder.id, updateHolder.foreignId, serverCode, utcOffsetSeconds);
        foundServers.add(sc);
      }

      // add a small delay every 5 servers to prevent exceeding api quota limits
      if (index != 0 && index % 5 == 0) {
        //noinspection BusyWait
        Thread.sleep(1000);
      }
    }

    if (fullCollection) {
      // mark all servers which are not included in the response as deleted
      var updatedServerIds = foundServers.stream().map(SimRailServerDescriptor::id).collect(Collectors.toSet());
      this.serverService.markUncontainedServersAsDeleted(updatedServerIds);

      // remove the removed servers from the
      var updatedServerIdStrings = foundServers.stream()
        .map(SimRailServerDescriptor::foreignId)
        .collect(Collectors.toSet());
      var removedServers = this.serverDataCache.findBySecondaryKeyNotIn(updatedServerIdStrings);
      removedServers.forEach(cached -> {
        this.serverDataCache.removeByPrimaryKey(cached.getIds().getDataId());

        // send out journey removal frame
        var baseFrameData = EventBusProto.BaseFrameData.newBuilder()
          .setTimestamp(MonotonicInstantProvider.monotonicTimeMillis())
          .build();
        var serverRemoveFrame = EventBusProto.ServerRemoveFrame.newBuilder()
          .setBaseData(baseFrameData)
          .setServerId(cached.getIds().getServerId())
          .build();
        var subject = EventSubjectFactory.createServerRemoveSubjectV1(cached.getIds().getServerId());
        this.connection.publish(subject, serverRemoveFrame.toByteArray());
      });

      // remove the unnecessary update holders from the local cache
      for (var entry : this.updateHoldersByForeignId.entrySet()) {
        var foreignId = entry.getKey();
        var holder = entry.getValue();
        if (!updatedServerIds.contains(holder.id)) {
          this.updateHoldersByForeignId.remove(foreignId);
        }
      }

      // update the list of servers that are known
      this.collectedServers = foundServers;
    }
  }

  @Override
  public @NonNull List<SimRailServerDescriptor> getServers() {
    return this.collectedServers;
  }

  @Override
  public @NonNull Optional<SimRailServerDescriptor> findServerByIntId(@NonNull UUID id) {
    return this.collectedServers.stream().filter(desc -> desc.id().equals(id)).findFirst();
  }

  @Override
  public @NonNull Optional<SimRailServerDescriptor> findServerBySimRailId(@NonNull String id) {
    return this.collectedServers.stream().filter(desc -> desc.foreignId().equals(id)).findFirst();
  }

  @Override
  public @NonNull Optional<SimRailServerDescriptor> findServerByCode(@NonNull String code) {
    return this.collectedServers.stream().filter(desc -> desc.code().equals(code)).findFirst();
  }
}
