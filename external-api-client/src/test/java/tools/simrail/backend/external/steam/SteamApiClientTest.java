/*
 * This file is part of simrail-tools-backend, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024-present Pasqual Koschmieder and contributors
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

package tools.simrail.backend.external.steam;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@EnabledIfEnvironmentVariable(named = "TEST_STEAM_API_KEY", matches = ".+")
public final class SteamApiClientTest {

  @Test
  void testPlayerSummaries() {
    var apiKey = System.getenv("TEST_STEAM_API_KEY");
    var client = SteamApiClient.create(apiKey);
    var response = client.getPlayerSummaries(List.of("76561198342066455"));

    var players = response.response().players();
    Assertions.assertEquals(1, players.size());

    var playerSummary = players.getFirst();
    Assertions.assertEquals("76561198342066455", playerSummary.getId());
    Assertions.assertNotNull(playerSummary.getDisplayName());
  }

  @Test
  void testUserStats() {
    var apiKey = System.getenv("TEST_STEAM_API_KEY");
    var client = SteamApiClient.create(apiKey);
    var response = client.getUserStats("1422130", "76561198342066455");

    var stats = response.stats();
    Assertions.assertNotNull(stats);
    Assertions.assertEquals("76561198342066455", stats.getUserId());

    var statistics = stats.getStats();
    Assertions.assertFalse(statistics.isEmpty());
    for (var statistic : statistics) {
      Assertions.assertNotNull(statistic.getName());
    }

    var achievements = stats.getAchievements();
    Assertions.assertFalse(achievements.isEmpty());
    for (var achievement : achievements) {
      Assertions.assertNotNull(achievement.getName());
    }
  }

  @Test
  void testGlobalAchievementProgress() {
    var apiKey = System.getenv("TEST_STEAM_API_KEY");
    var client = SteamApiClient.create(apiKey);
    var response = client.getGlobalAchievementProgress("1422130");

    var achievements = response.response().achievements();
    Assertions.assertFalse(achievements.isEmpty());
    for (var achievement : achievements) {
      Assertions.assertNotNull(achievement.getAchievementName());
      Assertions.assertTrue(achievement.getPercentage() > 0.0 && achievement.getPercentage() <= 100.0);
    }
  }
}
