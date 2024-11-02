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

package tools.simrail.backend.common.server;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;

/**
 * The entity that holds all information about a single server registered in the SimRail api.
 */
@Data
@NoArgsConstructor
@Entity(name = "sr_server")
@Table(name = "servers", indexes = {
  @Index(columnList = "code"),
  @Index(columnList = "foreignId"),
})
public final class SimRailServerEntity {

  /**
   * The unique identifier of the server
   */
  @Id
  @Nonnull
  @GeneratedValue
  private UUID id;
  /**
   * The revision version of this entity.
   */
  @Version
  @Column(nullable = false)
  private long version;
  /**
   * The foreign (mongo identifier) of the server provided by the SimRail api.
   */
  @Nonnull
  @Column(nullable = false, unique = true, updatable = false, length = 24)
  private String foreignId;

  /**
   * The timestamp when this server entry was last updated.
   */
  @Nonnull
  @UpdateTimestamp
  @Column(nullable = false)
  private OffsetDateTime updateTime;
  /**
   * The time when the server was initially registered in the SimRail backend.
   */
  @Nonnull
  @Column(nullable = false)
  private OffsetDateTime registeredSince;

  /**
   * The code of the server (e.g. DE1).
   */
  @Nonnull
  @Column(nullable = false, unique = true)
  private String code;
  /**
   * The region in which the server is located (e.g. Europe).
   */
  @Nonnull
  @Column(nullable = false)
  private SimRailServerRegion region;
  /**
   * The timezone that is used on the server (e.g. UTC+1).
   */
  @Nonnull
  @Audited
  @Column(nullable = false)
  private String timezone;
  /**
   * The language used to communicate on the server, null for international servers.
   */
  @Column
  @Nullable
  private String spokenLanguage;

  /**
   * If this server is currently online.
   */
  @Audited
  @Column(nullable = false)
  private boolean online;

  /**
   * The tags that are applied to the server, can be an empty list if no tags are applied.
   */
  @Nonnull
  @Audited
  @Column(nullable = false)
  @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
  private List<String> tags;
}
