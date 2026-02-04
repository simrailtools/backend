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

package tools.simrail.backend.collector.dispatchpost;

import java.util.UUID;
import org.jspecify.annotations.NonNull;
import tools.simrail.backend.common.proto.EventBusProto;
import tools.simrail.backend.common.update.UpdatableField;
import tools.simrail.backend.common.update.UpdatableFieldGroup;

/**
 * Holder for the updates to a single dispatch post.
 */
final class DispatchPostUpdateHolder {

  final UUID id;
  final String foreignId;
  final String secondaryCacheKey;

  // the updatable fields
  final UpdatableFieldGroup fieldGroup;
  final UpdatableField<EventBusProto.User> dispatcher;

  DispatchPostUpdateHolder(@NonNull UUID id, @NonNull String foreignId, @NonNull String secondaryCacheKey) {
    this.id = id;
    this.foreignId = foreignId;
    this.secondaryCacheKey = secondaryCacheKey;

    this.fieldGroup = new UpdatableFieldGroup();
    this.dispatcher = this.fieldGroup.createNullableField();
  }

  public static @NonNull String createSecondaryCacheKey(@NonNull String serverId, @NonNull String foreignId) {
    return serverId + '_' + foreignId;
  }
}
