/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024-2026 Pasqual Koschmieder and contributors
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

CREATE TABLE sr_server
(
  id               UUID                        NOT NULL,
  foreign_id       VARCHAR(24)                 NOT NULL,
  update_time      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  registered_since TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  code             TEXT                        NOT NULL,
  region           TEXT                        NOT NULL,
  utc_offset_hours SMALLINT                    NOT NULL,
  language         TEXT,
  scenery          TEXT                        NOT NULL,
  tags             JSONB                       NOT NULL,
  deleted          BOOLEAN                     NOT NULL,
  CONSTRAINT pk_sr_server PRIMARY KEY (id),
  CONSTRAINT uk_sr_server_foreign_id UNIQUE (foreign_id)
);

CREATE TABLE sr_dispatch_post
(
  id               UUID                        NOT NULL,
  point_id         UUID                        NOT NULL,
  foreign_id       VARCHAR(24)                 NOT NULL,
  update_time      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  registered_since TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  name             TEXT                        NOT NULL,
  server_id        UUID                        NOT NULL,
  deleted          BOOLEAN                     NOT NULL,
  difficulty_level SMALLINT                    NOT NULL,
  pos_latitude     DOUBLE PRECISION            NOT NULL,
  pos_longitude    DOUBLE PRECISION            NOT NULL,
  image_urls       JSONB                       NOT NULL,
  CONSTRAINT pk_sr_dispatch_post PRIMARY KEY (id),
  CONSTRAINT uk_sr_dispatch_post_foreign_id UNIQUE (foreign_id)
);

CREATE TABLE sit_journey
(
  id                      UUID                        NOT NULL,
  foreign_run_id          UUID                        NOT NULL,
  server_id               UUID                        NOT NULL,
  update_time             TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  first_seen_time         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  last_seen_time          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  cancelled               BOOLEAN                     NOT NULL,
  continuation_journey_id UUID,
  CONSTRAINT pk_sit_journey PRIMARY KEY (id),
  CONSTRAINT uk_sit_journey_run_id UNIQUE (foreign_run_id)
);

CREATE TABLE sit_journey_event
(
  id                  UUID                        NOT NULL,
  journey_id          UUID                        NOT NULL,
  event_index         SMALLINT                    NOT NULL,
  created_at          TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  event_type          TEXT                        NOT NULL,

  point_id            UUID                        NOT NULL,
  in_playable_border  BOOLEAN                     NOT NULL,

  scheduled_time      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  realtime_time       TIMESTAMP WITHOUT TIME ZONE,
  realtime_time_type  TEXT                        NOT NULL,

  transport_line      TEXT,
  transport_type      TEXT                        NOT NULL,
  transport_label     TEXT,
  transport_number    TEXT                        NOT NULL,
  transport_category  TEXT                        NOT NULL,
  transport_max_speed SMALLINT                    NOT NULL,

  stop_type           TEXT                        NOT NULL,
  scheduled_track     TEXT,
  scheduled_platform  TEXT,
  realtime_track      TEXT,
  realtime_platform   TEXT,

  cancelled           BOOLEAN                     NOT NULL,
  additional          BOOLEAN                     NOT NULL,

  CONSTRAINT pk_sit_journey_event PRIMARY KEY (id)
);

CREATE TABLE sit_journey_checksum
(
  id         BIGSERIAL NOT NULL,
  journey_id UUID      NOT NULL,
  checksum   TEXT      NOT NULL,
  CONSTRAINT pk_sit_journey_checksum PRIMARY KEY (id),
  CONSTRAINT uk_sit_journey_checksum_journey UNIQUE (journey_id)
);

CREATE TABLE sit_journey_vehicle
(
  id             UUID                        NOT NULL,
  journey_id     UUID                        NOT NULL,
  updated_at     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  index_in_group SMALLINT                    NOT NULL,
  status         TEXT                        NOT NULL,
  railcar_id     UUID                        NOT NULL,
  load_weight    SMALLINT,
  load           TEXT,
  CONSTRAINT pk_sit_journey_vehicle PRIMARY KEY (id)
);

CREATE INDEX idx_sr_server_code ON sr_server (code);
CREATE INDEX idx_sr_server_foreign_id ON sr_server (foreign_id);

ALTER TABLE sr_dispatch_post
  ADD CONSTRAINT fk_sr_dispatch_post_server
  FOREIGN KEY (server_id)
  REFERENCES sr_server (id)
  ON DELETE CASCADE;
CREATE INDEX idx_sr_dispatch_post_foreign_id ON sr_dispatch_post (foreign_id);
CREATE INDEX idx_sr_dispatch_post_server_point ON sr_dispatch_post (server_id, point_id);

ALTER TABLE sit_journey
  ADD CONSTRAINT fk_sit_journey_server
  FOREIGN KEY (server_id)
  REFERENCES sr_server (id)
  ON DELETE CASCADE;
CREATE INDEX idx_sit_journey_foreign_run_id ON sit_journey (foreign_run_id);

ALTER TABLE sit_journey_event
  ADD CONSTRAINT fk_sit_journey_event_journey
  FOREIGN KEY (journey_id)
  REFERENCES sit_journey (id)
  ON DELETE CASCADE;
CREATE INDEX idx_sit_journey_event_journey ON sit_journey_event (journey_id);

ALTER TABLE sit_journey_checksum
  ADD CONSTRAINT fk_sit_journey_checksum_journey
  FOREIGN KEY (journey_id)
  REFERENCES sit_journey (id)
  ON DELETE CASCADE;
CREATE INDEX idx_sit_journey_checksum_journey ON sit_journey_checksum (journey_id);

ALTER TABLE sit_journey_vehicle
  ADD CONSTRAINT fk_sit_journey_vehicle_journey
  FOREIGN KEY (journey_id)
  REFERENCES sit_journey (id)
  ON DELETE CASCADE;
CREATE INDEX idx_sit_journey_vehicle_journey ON sit_journey_vehicle (journey_id);
