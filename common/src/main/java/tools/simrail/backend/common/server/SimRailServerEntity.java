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

package tools.simrail.backend.common.server;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * The entity that holds all information about a single server registered in the SimRail api.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity(name = "sr_server")
public final class SimRailServerEntity {

  /**
   * The namespace used to generate UUIDv5 ids for server entities.
   */
  public static final UUID ID_NAMESPACE = UUID.fromString("8fb462f5-82ab-4096-8538-fff7a96a0094");

  /**
   * The unique identifier of the server.
   */
  @Id
  @Column(name = "id")
  private UUID id;
  /**
   * The foreign (mongo identifier) of the server provided by the SimRail api.
   */
  @Column(name = "foreign_id")
  private String foreignId;
  /**
   * The code of the server (e.g. DE1).
   */
  @Column(name = "code")
  private String code;

  /**
   * The timestamp when this server entry was last updated.
   */
  @UpdateTimestamp
  @Column(name = "update_time")
  private Instant updateTime;
  /**
   * The time when the server was initially registered in the SimRail backend.
   */
  @Column(name = "registered_since")
  private Instant registeredSince;

  /**
   * The region in which the server is located (e.g. Europe).
   */
  @Column(name = "region")
  @Enumerated(EnumType.STRING)
  private SimRailServerRegion region;
  /**
   * The time offset on the server (in hours) from the current utc time.
   */
  @Column(name = "utc_offset_hours")
  private int utcOffsetHours;
  /**
   * The language used to communicate on the server, null for international servers.
   */
  @Column(name = "language")
  private String spokenLanguage;
  /**
   * Scenery (map part) of the server.
   */
  @Column(name = "scenery")
  @Enumerated(EnumType.STRING)
  private SimRailServerScenery scenery;
  /**
   * The tags that are applied to the server, can be an empty list if no tags are applied.
   */
  @Column(name = "tags")
  @JdbcTypeCode(SqlTypes.JSON)
  private List<String> tags;

  /**
   * If this server is no longer registered in the SimRail backend.
   */
  @Column(name = "deleted")
  private boolean deleted;

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SimRailServerEntity entity)) {
      return false;
    }
    return Objects.equals(this.id, entity.getId());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(this.id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public @Nonnull String toString() {
    return "ServerEntity{id=" + this.id + ", code=" + this.code + "}";
  }
}
