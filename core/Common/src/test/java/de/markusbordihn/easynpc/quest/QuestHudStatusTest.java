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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.markusbordihn.easynpc.quest.QuestHudStatus.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Pure-logic tests for the client quest HUD — no Minecraft classes touched. */
class QuestHudStatusTest {

  @Test
  @DisplayName("[T,T,F,F,F] -> DONE,DONE,CURRENT,LOCKED,LOCKED; 40%; not complete")
  void partialProgress() {
    boolean[] done = {true, true, false, false, false};

    assertArrayEquals(
        new Status[] {
          Status.DONE, Status.DONE, Status.CURRENT, Status.LOCKED, Status.LOCKED
        },
        QuestHudStatus.stageStatuses(done));
    assertEquals(40, QuestHudStatus.percent(done));
    assertFalse(QuestHudStatus.isComplete(done));
  }

  @Test
  @DisplayName("[T,T,T,T,T] -> all DONE; 100%; complete")
  void allDone() {
    boolean[] done = {true, true, true, true, true};

    assertArrayEquals(
        new Status[] {Status.DONE, Status.DONE, Status.DONE, Status.DONE, Status.DONE},
        QuestHudStatus.stageStatuses(done));
    assertEquals(100, QuestHudStatus.percent(done));
    assertTrue(QuestHudStatus.isComplete(done));
  }

  @Test
  @DisplayName("empty -> empty statuses; 0%; not complete")
  void empty() {
    boolean[] done = {};

    assertArrayEquals(new Status[] {}, QuestHudStatus.stageStatuses(done));
    assertEquals(0, QuestHudStatus.percent(done));
    assertFalse(QuestHudStatus.isComplete(done));
  }

  @Test
  @DisplayName("first stage current when nothing done; rounding (1/3 -> 33%)")
  void firstCurrentAndRounding() {
    boolean[] done = {false, false, false};

    assertArrayEquals(
        new Status[] {Status.CURRENT, Status.LOCKED, Status.LOCKED},
        QuestHudStatus.stageStatuses(done));
    assertEquals(0, QuestHudStatus.percent(done));

    boolean[] oneOfThree = {true, false, false};
    assertEquals(33, QuestHudStatus.percent(oneOfThree));
    assertEquals(
        Status.CURRENT, QuestHudStatus.stageStatuses(oneOfThree)[1], "first not-done is current");
  }
}
