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

package de.markusbordihn.easynpc.client.map;

import de.markusbordihn.easynpc.quest.MapData;

/**
 * Client-side store for the map of the player's current dimension (MOD-6). Populated by {@code
 * MapDefinitionMessage.handleClient} and emptied by {@code MapClearMessage.handleClient}. Simpler
 * than {@code QuestHudState} — the map is static per dimension, so there are no transition events.
 * MC-free so it is unit-testable.
 */
public final class MapState {

  private static volatile MapData current;

  private MapState() {}

  /** Set the active map definition (push on dimension entry / login). */
  public static void set(final MapData data) {
    current = data;
  }

  /** Clear the active map (player left a mapped dimension, or it was unloaded). */
  public static void clear() {
    current = null;
  }

  /** Active map definition, or {@code null} when none is available for the current dimension. */
  public static MapData get() {
    return current;
  }

  /** True when there is no map to show. */
  public static boolean isEmpty() {
    return current == null;
  }
}
