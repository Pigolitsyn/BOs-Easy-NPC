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
 * Pure (MC-free) detection of done-flag transitions between two consecutive HUD updates. Used by the
 * client store to fire stage toasts (US2) and the completion banner (US4) exactly once when progress
 * changes. Kept free of Minecraft classes so it is unit-testable with plain JUnit.
 *
 * <p>{@code prev} may be {@code null} or shorter than {@code cur} (the very first update, or a stage
 * count change). Missing previous entries are treated as {@code false} (not yet done).
 */
public final class QuestHudTransitions {

  private static final int[] EMPTY = new int[0];

  private QuestHudTransitions() {}

  /**
   * Indices of stages that became done in this update: {@code cur[i] && !prev[i]}. Missing previous
   * entries (null/short prev) count as not-done, so a fresh quest whose first update already has done
   * stages reports them all as newly done.
   */
  public static int[] newlyDone(final boolean[] prev, final boolean[] cur) {
    if (cur == null || cur.length == 0) {
      return EMPTY;
    }
    int count = 0;
    for (int i = 0; i < cur.length; i++) {
      if (cur[i] && !was(prev, i)) {
        count++;
      }
    }
    if (count == 0) {
      return EMPTY;
    }
    int[] result = new int[count];
    int n = 0;
    for (int i = 0; i < cur.length; i++) {
      if (cur[i] && !was(prev, i)) {
        result[n++] = i;
      }
    }
    return result;
  }

  /**
   * True when ALL stages are done now but were NOT all done before. Empty current -> false (nothing
   * to complete). A first update where everything is already done counts as just completed.
   */
  public static boolean justCompleted(final boolean[] prev, final boolean[] cur) {
    if (!QuestHudStatus.isComplete(cur)) {
      return false;
    }
    return !wasComplete(prev, cur.length);
  }

  /** Previous done flag for index {@code i}, treating null/short prev as false. */
  private static boolean was(final boolean[] prev, final int i) {
    return prev != null && i < prev.length && prev[i];
  }

  /** True only if every one of {@code len} stages was already done in {@code prev}. */
  private static boolean wasComplete(final boolean[] prev, final int len) {
    if (prev == null || prev.length < len) {
      return false;
    }
    for (int i = 0; i < len; i++) {
      if (!prev[i]) {
        return false;
      }
    }
    return true;
  }
}
