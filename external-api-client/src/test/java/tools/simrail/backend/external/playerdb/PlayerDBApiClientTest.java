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

package tools.simrail.backend.external.playerdb;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.simrail.backend.external.playerdb.model.PlayerDbResponseWrapper;

public final class PlayerDBApiClientTest {

  private static final String TEST_USER_AGENT = "SimRailInformationTools Backend (Test)";

  @Test
  void testSuccessfulSteamData() {
    var client = PlayerDbApiClient.create(TEST_USER_AGENT);
    var response = Assertions.assertDoesNotThrow(() -> client.getSteamPlayer("76561198342066455"));
    Assertions.assertEquals(PlayerDbResponseWrapper.RESPONSE_CODE_SUCCESS, response.getCode());
    Assertions.assertNotNull(response.getData());
    Assertions.assertNotNull(response.getData().player());

    var playerData = response.getData().player();
    Assertions.assertEquals("76561198342066455", playerData.getId());
    Assertions.assertEquals("derklaro", playerData.getUsername());
    Assertions.assertNotNull(playerData.getAvatarUrl());
    Assertions.assertTrue(playerData.getAvatarUrl().startsWith("https://avatars.steamstatic.com/"));
    Assertions.assertTrue(playerData.getAvatarUrl().endsWith("_full.jpg"));

    var meta = playerData.getMetadata();
    Assertions.assertNotNull(meta);
    Assertions.assertTrue(meta.path("loccountrycode").isString());
  }

  @Test
  void testSuccessfulXBoxData() {
    var client = PlayerDbApiClient.create(TEST_USER_AGENT);
    var response = Assertions.assertDoesNotThrow(() -> client.getXboxPlayer("2535457121968519"));
    Assertions.assertEquals(PlayerDbResponseWrapper.RESPONSE_CODE_SUCCESS, response.getCode());

    var playerData = response.getData().player();
    Assertions.assertEquals("2535457121968519", playerData.getId());
    Assertions.assertEquals("derklaro7460", playerData.getUsername());
    Assertions.assertNotNull(playerData.getAvatarUrl());
    Assertions.assertTrue(playerData.getAvatarUrl().startsWith("https://images-eds-ssl.xboxlive.com/image"));
    Assertions.assertTrue(playerData.getAvatarUrl().endsWith("&format=png&h=180&w=180"));

    var meta = playerData.getMetadata();
    Assertions.assertNotNull(meta);
    Assertions.assertTrue(meta.path("location").isString());
  }

  @Test
  void testInvalidSteamId() {
    var client = PlayerDbApiClient.create(TEST_USER_AGENT);
    var response = Assertions.assertDoesNotThrow(() -> client.getSteamPlayer("76561197960265728"));
    Assertions.assertEquals(PlayerDbResponseWrapper.RESPONSE_CODE_STEAM_ID_INVALID, response.getCode());
    Assertions.assertNotNull(response.getData());
    Assertions.assertNull(response.getData().player());
  }

  @Test
  void testInvalidXBoxId() {
    var client = PlayerDbApiClient.create(TEST_USER_AGENT);
    var response = Assertions.assertDoesNotThrow(() -> client.getXboxPlayer("2535467890123456"));
    Assertions.assertEquals(PlayerDbResponseWrapper.RESPONSE_CODE_XBOX_BAD_RESPONSE_CODE, response.getCode());
    Assertions.assertNotNull(response.getData());
    Assertions.assertNull(response.getData().player());
    Assertions.assertEquals(400, response.getData().status());
  }
}
