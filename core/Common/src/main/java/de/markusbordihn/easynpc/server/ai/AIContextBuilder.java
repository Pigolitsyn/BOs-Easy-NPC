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

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;

/** Builds a compact world-state context string the AI can consume on event-triggered messages. */
public final class AIContextBuilder {

  private AIContextBuilder() {}

  public static String build(LivingEntity npc, Player nearestPlayer) {
    if (npc == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    sb.append("[Context] ");
    sb.append(timeOfDay(npc)).append(", ");
    sb.append(weather(npc));
    String biome = biome(npc);
    if (!biome.isEmpty()) {
      sb.append(", biome=").append(biome);
    }
    if (nearestPlayer != null) {
      sb.append(", player=").append(nearestPlayer.getName().getString());
      sb.append(" hp=")
          .append((int) nearestPlayer.getHealth())
          .append('/')
          .append((int) nearestPlayer.getMaxHealth());
      sb.append(" dist=").append(distance(npc, nearestPlayer)).append('m');
    }
    return sb.toString();
  }

  static String timeOfDay(LivingEntity npc) {
    if (npc.level() == null) {
      return "time=?";
    }
    long t = npc.level().getDayTime() % 24000L;
    String phase;
    if (t < 6000) phase = "morning";
    else if (t < 12000) phase = "day";
    else if (t < 13000) phase = "dusk";
    else if (t < 18000) phase = "night";
    else phase = "late_night";
    return "time=" + phase;
  }

  static String weather(LivingEntity npc) {
    if (!(npc.level() instanceof ServerLevel level)) {
      return "weather=?";
    }
    if (level.isThundering()) return "weather=thunder";
    if (level.isRaining()) return "weather=rain";
    return "weather=clear";
  }

  static String biome(LivingEntity npc) {
    if (npc.level() == null) {
      return "";
    }
    BlockPos pos = npc.blockPosition();
    Holder<Biome> holder = npc.level().getBiome(pos);
    String raw = holder.unwrapKey().map(Object::toString).orElse("");
    int colon = raw.lastIndexOf(':');
    if (colon >= 0 && colon + 1 < raw.length()) {
      String tail = raw.substring(colon + 1);
      int closing = tail.indexOf(']');
      return closing > 0 ? tail.substring(0, closing) : tail;
    }
    return raw;
  }

  static int distance(LivingEntity a, Player b) {
    double dx = a.getX() - b.getX();
    double dy = a.getY() - b.getY();
    double dz = a.getZ() - b.getZ();
    return (int) Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz));
  }
}
