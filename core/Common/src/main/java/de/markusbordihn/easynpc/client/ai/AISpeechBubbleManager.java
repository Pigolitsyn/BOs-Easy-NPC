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

package de.markusbordihn.easynpc.client.ai;

import de.markusbordihn.easynpc.entity.LivingEntityManager;
import de.markusbordihn.easynpc.entity.easynpc.EasyNPC;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Client-side store of active AI speech bubbles. Server pushes content via {@code NPCSpeakMessage};
 * renderers query {@link #getActive(UUID, long)} per-frame.
 */
public final class AISpeechBubbleManager {

  public static final long DEFAULT_TTL_MS = 6_000L;

  private static final Map<UUID, Bubble> bubbles = new ConcurrentHashMap<>();
  private static long ttlMillis = DEFAULT_TTL_MS;
  private static TimeSource timeSource = System::currentTimeMillis;

  private AISpeechBubbleManager() {}

  public static void publish(UUID npcId, String content) {
    if (npcId == null || content == null || content.isEmpty()) {
      return;
    }
    bubbles.put(npcId, new Bubble(content, timeSource.now()));
    notifyChatOverlay(npcId, content);
  }

  private static void notifyChatOverlay(UUID npcId, String content) {
    Minecraft mc;
    try {
      mc = Minecraft.getInstance();
    } catch (Throwable t) {
      return;
    }
    if (mc == null || mc.player == null) {
      return;
    }
    String name = "NPC";
    EasyNPC<?> npc = LivingEntityManager.getEasyNPCEntityByUUID(npcId);
    if (npc != null && npc.getLivingEntity() != null) {
      name = npc.getLivingEntity().getName().getString();
    }
    mc.player.displayClientMessage(
        Component.literal("<" + name + "> " + content), false);
  }

  public static Bubble getActive(UUID npcId, long nowMillis) {
    Bubble b = bubbles.get(npcId);
    if (b == null) {
      return null;
    }
    if (nowMillis - b.createdAtMillis() > ttlMillis) {
      bubbles.remove(npcId, b);
      return null;
    }
    return b;
  }

  public static Bubble getActive(UUID npcId) {
    return getActive(npcId, timeSource.now());
  }

  public static void clear() {
    bubbles.clear();
  }

  public static int size() {
    return bubbles.size();
  }

  // Test hooks.
  public static void setTtlMillisForTest(long ms) {
    ttlMillis = ms;
  }

  public static void setTimeSourceForTest(TimeSource source) {
    timeSource = source == null ? System::currentTimeMillis : source;
  }

  public static long getTtlMillis() {
    return ttlMillis;
  }

  public record Bubble(String content, long createdAtMillis) {}

  @FunctionalInterface
  public interface TimeSource {
    long now();
  }
}
