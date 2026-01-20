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

import jakarta.annotation.Nonnull;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * A descriptor of a server that was retrieved on the last collection run.
 *
 * @param id                      our id of the server.
 * @param foreignId               the SimRail backend id of the server.
 * @param code                    the server code.
 * @param timezoneOffset          the timezone offset of the server.
 * @param serverTimeOffsetSeconds the offset seconds of the server time from UTC.
 */
public record SimRailServerDescriptor(
  @Nonnull UUID id,
  @Nonnull String foreignId,
  @Nonnull String code,
  @Nonnull ZoneOffset timezoneOffset,
  long serverTimeOffsetSeconds
) {

  /**
   * Get the current date and time on this server.
   *
   * @return the current date and time on this server.
   */
  public @Nonnull LocalDateTime currentTime() {
    return ZonedDateTime.now(ZoneOffset.UTC)
      .plusSeconds(this.serverTimeOffsetSeconds)
      .withZoneSameLocal(this.timezoneOffset)
      .toOffsetDateTime();
  }
}
