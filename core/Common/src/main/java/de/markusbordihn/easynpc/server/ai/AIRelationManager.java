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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks an integer relation score per (NPC, player) pair, clamped to [-100, +100]. Stored
 * in-memory only — resets on server restart for now.
 */
public final class AIRelationManager {

  public static final int MIN = -100;
  public static final int MAX = 100;
  public static final int DEFAULT_VALUE = 0;

  private static final Map<String, Integer> scores = new ConcurrentHashMap<>();

  private AIRelationManager() {}

  public static int get(UUID npcId, UUID playerId) {
    return scores.getOrDefault(key(npcId, playerId), DEFAULT_VALUE);
  }

  public static int set(UUID npcId, UUID playerId, int value) {
    int clamped = clamp(value);
    scores.put(key(npcId, playerId), clamped);
    return clamped;
  }

  public static int adjust(UUID npcId, UUID playerId, int delta) {
    return set(npcId, playerId, get(npcId, playerId) + delta);
  }

  public static void clear(UUID npcId, UUID playerId) {
    scores.remove(key(npcId, playerId));
  }

  public static void clearAll() {
    scores.clear();
  }

  public static String describe(int value) {
    if (value >= 80) return "devoted";
    if (value >= 50) return "friendly";
    if (value >= 20) return "warm";
    if (value >= -19) return "neutral";
    if (value >= -49) return "wary";
    if (value >= -79) return "hostile";
    return "hateful";
  }

  private static int clamp(int v) {
    return Math.max(MIN, Math.min(MAX, v));
  }

  private static String key(UUID npcId, UUID playerId) {
    return npcId + ":" + playerId;
  }
}
