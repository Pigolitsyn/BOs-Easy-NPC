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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pure-logic tests for HUD done-flag transition detection — no Minecraft classes touched. */
class QuestHudTransitionsTest {

  @Test
  @DisplayName("newlyDone: first stage flips -> [0]")
  void newlyDoneFirstStage() {
    assertArrayEquals(
        new int[] {0},
        QuestHudTransitions.newlyDone(
            new boolean[] {false, false, false}, new boolean[] {true, false, false}));
  }

  @Test
  @DisplayName("newlyDone: second stage flips -> [1]")
  void newlyDoneSecondStage() {
    assertArrayEquals(
        new int[] {1},
        QuestHudTransitions.newlyDone(
            new boolean[] {true, false, false}, new boolean[] {true, true, false}));
  }

  @Test
  @DisplayName("newlyDone: null prev, [T,F] -> [0]")
  void newlyDoneNullPrev() {
    assertArrayEquals(
        new int[] {0}, QuestHudTransitions.newlyDone(null, new boolean[] {true, false}));
  }

  @Test
  @DisplayName("newlyDone: short prev treats missing as false")
  void newlyDoneShortPrev() {
    assertArrayEquals(
        new int[] {1, 2},
        QuestHudTransitions.newlyDone(
            new boolean[] {true}, new boolean[] {true, true, true}));
  }

  @Test
  @DisplayName("newlyDone: no change -> empty")
  void newlyDoneNoChange() {
    assertArrayEquals(
        new int[] {},
        QuestHudTransitions.newlyDone(
            new boolean[] {true, false}, new boolean[] {true, false}));
  }

  @Test
  @DisplayName("justCompleted: [T,T,F] -> [T,T,T] = true")
  void justCompletedTrue() {
    assertTrue(
        QuestHudTransitions.justCompleted(
            new boolean[] {true, true, false}, new boolean[] {true, true, true}));
  }

  @Test
  @DisplayName("justCompleted: already complete -> false")
  void justCompletedAlready() {
    assertFalse(
        QuestHudTransitions.justCompleted(
            new boolean[] {true, true, true}, new boolean[] {true, true, true}));
  }

  @Test
  @DisplayName("justCompleted: single not-done [F] -> [F] = false")
  void justCompletedNotComplete() {
    assertFalse(QuestHudTransitions.justCompleted(new boolean[] {false}, new boolean[] {false}));
  }

  @Test
  @DisplayName("justCompleted: null prev, all done now -> true")
  void justCompletedNullPrev() {
    assertTrue(QuestHudTransitions.justCompleted(null, new boolean[] {true, true}));
  }
}
