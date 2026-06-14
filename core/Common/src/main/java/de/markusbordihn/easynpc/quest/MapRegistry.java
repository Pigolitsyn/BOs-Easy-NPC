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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side registry of mod-rendered map definitions (MOD-6), keyed by full dimension id (e.g.
 * {@code lorecraft:quest_<hex>}, the same key scheme as {@link QuestHudRegistry}). One map per quest
 * dimension. Map data is static per dimension — unlike the quest HUD there is no per-player state to
 * resolve, so this is a plain store.
 */
public final class MapRegistry {

  /** dimKey (lorecraft:quest_<hex>) -> map data. */
  private static final Map<String, MapData> REGISTRY = new ConcurrentHashMap<>();

  private MapRegistry() {}

  public static void put(String dimKey, MapData data) {
    REGISTRY.put(dimKey, data);
  }

  public static MapData get(String dimKey) {
    return REGISTRY.get(dimKey);
  }

  public static MapData remove(String dimKey) {
    return REGISTRY.remove(dimKey);
  }
}
