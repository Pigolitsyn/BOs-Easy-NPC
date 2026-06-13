/*
 * Copyright 2023 Markus Bordihn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.markusbordihn.easynpc.quest;

import de.markusbordihn.easynpc.Constants;
import de.markusbordihn.easynpc.quest.QuestHudData.StageInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.Scoreboard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Server-side registry of quest HUD definitions, keyed by full dimension id (e.g.
 * {@code lorecraft:quest_<hex>}). Holds the base (done-less) definition; per-player completion is
 * resolved on demand from the scoreboard via {@link #withStatus}.
 */
public final class QuestHudRegistry {

  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  /** dimKey (lorecraft:quest_<hex>) -> base data (all stages done=false). */
  private static final Map<String, QuestHudData> REGISTRY = new ConcurrentHashMap<>();

  private QuestHudRegistry() {}

  public static void put(String dimKey, QuestHudData data) {
    REGISTRY.put(dimKey, data);
  }

  public static QuestHudData get(String dimKey) {
    return REGISTRY.get(dimKey);
  }

  public static QuestHudData remove(String dimKey) {
    return REGISTRY.remove(dimKey);
  }

  /**
   * Resolve the live per-player completion state for a registered quest dimension.
   *
   * <p>For each stage, looks up the scoreboard objective named by {@code stage.objective()}
   * ({@code q<code>_sN}) and reads the player's score: {@code done = score >= 1}. If the objective
   * does not exist yet (never awarded), {@code done = false}. Returns a copy of the base data with
   * done flags filled, or {@code null} if the dimension is not registered.
   */
  public static QuestHudData withStatus(
      MinecraftServer server, ServerPlayer player, String dimKey) {
    QuestHudData base = REGISTRY.get(dimKey);
    if (base == null || server == null || player == null) {
      return base;
    }
    Scoreboard scoreboard = server.getScoreboard();
    List<StageInfo> stages = new ArrayList<>(base.stages().size());
    for (StageInfo stage : base.stages()) {
      boolean done = false;
      try {
        Objective objective = scoreboard.getObjective(stage.objective());
        if (objective != null) {
          ScoreAccess scoreAccess = scoreboard.getOrCreatePlayerScore(player, objective);
          done = scoreAccess.get() >= 1;
        }
      } catch (Exception e) {
        log.warn(
            "[QuestHud] failed reading objective '{}' for {}: {}",
            stage.objective(),
            player.getName().getString(),
            e.toString());
      }
      stages.add(stage.withDone(done));
    }
    return base.withStages(stages);
  }

  /** Stable hash of the done-flags for change detection in the poll loop. */
  public static long doneHash(QuestHudData data) {
    if (data == null) {
      return 0L;
    }
    long hash = 1L;
    for (StageInfo stage : data.stages()) {
      hash = hash * 31L + (stage.done() ? 1L : 0L);
    }
    return hash;
  }
}
