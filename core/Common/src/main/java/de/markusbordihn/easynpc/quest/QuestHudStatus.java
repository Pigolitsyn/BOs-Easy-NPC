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

/**
 * Pure (MC-free) quest-progress logic for the client HUD. Status is derived from the per-stage
 * {@code done} flags carried in the {@link QuestHudData} packet — the client never reads the server
 * scoreboard (see spike hud-0). Kept free of Minecraft classes so it is unit-testable with plain
 * JUnit.
 */
public final class QuestHudStatus {

  /** Per-stage display status. */
  public enum Status {
    DONE,
    CURRENT,
    LOCKED
  }

  private QuestHudStatus() {}

  /**
   * Map per-stage done flags to display statuses: every done stage -> DONE; the first not-done stage
   * -> CURRENT; all remaining not-done stages -> LOCKED.
   */
  public static Status[] stageStatuses(final boolean[] done) {
    Status[] result = new Status[done.length];
    boolean currentAssigned = false;
    for (int i = 0; i < done.length; i++) {
      if (done[i]) {
        result[i] = Status.DONE;
      } else if (!currentAssigned) {
        result[i] = Status.CURRENT;
        currentAssigned = true;
      } else {
        result[i] = Status.LOCKED;
      }
    }
    return result;
  }

  /** Completion percentage, rounded: round(100 * doneCount / total). Empty -> 0. */
  public static int percent(final boolean[] done) {
    if (done.length == 0) {
      return 0;
    }
    int doneCount = 0;
    for (boolean d : done) {
      if (d) {
        doneCount++;
      }
    }
    return Math.round(100.0f * doneCount / done.length);
  }

  /** True when every stage is done. Empty -> false. */
  public static boolean isComplete(final boolean[] done) {
    if (done.length == 0) {
      return false;
    }
    for (boolean d : done) {
      if (!d) {
        return false;
      }
    }
    return true;
  }
}
