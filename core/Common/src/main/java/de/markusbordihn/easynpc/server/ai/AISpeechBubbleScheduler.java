/*
 * Copyright 2025 Markus Bordihn
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

package de.markusbordihn.easynpc.server.ai;

import de.markusbordihn.easynpc.Constants;
import de.markusbordihn.easynpc.data.dialog.DialogDataSet;
import de.markusbordihn.easynpc.data.dialog.DialogType;
import de.markusbordihn.easynpc.entity.LivingEntityManager;
import de.markusbordihn.easynpc.entity.easynpc.EasyNPC;
import de.markusbordihn.easynpc.entity.easynpc.data.DialogDataCapable;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Server tick scanner that triggers stateless AI calls for NPCs with proactive speech enabled.
 * Tracks last-fire game tick per NPC to enforce per-NPC interval.
 */
public final class AISpeechBubbleScheduler {

  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);
  private static final int SCAN_INTERVAL_TICKS = 20;

  private static final Map<UUID, Long> lastFireTick = new ConcurrentHashMap<>();
  private static long scanCounter = 0L;

  private AISpeechBubbleScheduler() {}

  public static void tick(MinecraftServer server) {
    if (server == null) {
      return;
    }
    scanCounter++;
    if (scanCounter % SCAN_INTERVAL_TICKS != 0L) {
      return;
    }
    for (ServerLevel level : server.getAllLevels()) {
      scanLevel(level);
    }
  }

  static void scanLevel(ServerLevel level) {
    long now = level.getGameTime();
    int candidates = 0;
    int aiBubble = 0;
    int fired = 0;
    for (EasyNPC<?> easyNPC : LivingEntityManager.getNpcEntityMap().values()) {
      if (easyNPC.getLivingEntity() == null
          || easyNPC.getLivingEntity().level() != level
          || !(easyNPC instanceof DialogDataCapable<?> dialogCap)) {
        continue;
      }
      candidates++;
      DialogDataSet ds = dialogCap.getDialogDataSet();
      if (ds == null
          || ds.getType() != DialogType.AI
          || !ds.isAISpeechBubbleEnabled()) {
        continue;
      }
      aiBubble++;
      UUID npcId = easyNPC.getEntityUUID();
      Long last = lastFireTick.get(npcId);
      long intervalTicks = Math.max(20, ds.getAISpeechBubbleIntervalTicks());
      if (last != null && (now - last) < intervalTicks) {
        continue;
      }
      ServerPlayer nearest = nearestPlayerInRadius(level, easyNPC.getLivingEntity(), ds.getAISpeechBubbleRadius());
      if (nearest == null) {
        log.info(
            "[AI][bubble] npc={} enabled but no player in radius={}",
            npcId,
            ds.getAISpeechBubbleRadius());
        continue;
      }
      lastFireTick.put(npcId, now);
      fired++;
      log.info(
          "[AI][bubble] fire npc={} player={} radius={} interval={}",
          npcId,
          nearest.getName().getString(),
          ds.getAISpeechBubbleRadius(),
          intervalTicks);
      fireEvent(easyNPC, ds, nearest, level);
    }
    if (candidates > 0) {
      log.debug(
          "[AI][bubble] scan level={} candidates={} aiBubbleEnabled={} fired={}",
          level.dimension(),
          candidates,
          aiBubble,
          fired);
    }
  }

  static ServerPlayer nearestPlayerInRadius(ServerLevel level, LivingEntity npc, int radius) {
    ServerPlayer nearest = null;
    double bestSq = (double) radius * radius;
    for (ServerPlayer p : level.players()) {
      double dx = p.getX() - npc.getX();
      double dy = p.getY() - npc.getY();
      double dz = p.getZ() - npc.getZ();
      double sq = dx * dx + dy * dy + dz * dz;
      if (sq <= bestSq) {
        bestSq = sq;
        nearest = p;
      }
    }
    return nearest;
  }

  static void fireEvent(
      EasyNPC<?> easyNPC, DialogDataSet ds, ServerPlayer nearest, ServerLevel level) {
    String context = AIContextBuilder.build(easyNPC.getLivingEntity(), nearest);
    String prompt =
        context
            + "\nA player just approached. Say one short, in-character line "
            + "(max 80 chars). Reply with only the line, no quotes.";
    log.debug(
        "[AI][event] firing event prompt for npc={} player={}",
        easyNPC.getEntityUUID(),
        nearest.getName().getString());
    AIDialogHandler.processEventMessage(
        easyNPC.getEntityUUID(),
        level,
        prompt,
        ds.getAIServerUrl(),
        ds.getAIModelName(),
        ds.compileEffectiveSystemPrompt(),
        ds.getAIApiKey(),
        ds.getAISpeechBubbleRadius(),
        null);
  }

  public static void reset() {
    lastFireTick.clear();
    scanCounter = 0L;
  }
}
