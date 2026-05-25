/*
 * Copyright 2025 Markus Bordihn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.markusbordihn.easynpc.server.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AIRelationManagerTest {

  private final UUID npc = UUID.randomUUID();
  private final UUID player = UUID.randomUUID();

  @BeforeEach
  void reset() {
    AIRelationManager.clearAll();
  }

  @Test
  void get_defaultsToZero() {
    assertEquals(0, AIRelationManager.get(npc, player));
  }

  @Test
  void set_clampsToBounds() {
    assertEquals(100, AIRelationManager.set(npc, player, 999));
    assertEquals(-100, AIRelationManager.set(npc, player, -999));
  }

  @Test
  void adjust_accumulates() {
    AIRelationManager.adjust(npc, player, 10);
    AIRelationManager.adjust(npc, player, 5);
    assertEquals(15, AIRelationManager.get(npc, player));
  }

  @Test
  void adjust_clampsOnOverflow() {
    AIRelationManager.set(npc, player, 95);
    assertEquals(100, AIRelationManager.adjust(npc, player, 50));
  }

  @Test
  void describe_bandsCorrectly() {
    assertEquals("devoted", AIRelationManager.describe(100));
    assertEquals("friendly", AIRelationManager.describe(70));
    assertEquals("warm", AIRelationManager.describe(20));
    assertEquals("neutral", AIRelationManager.describe(0));
    assertEquals("wary", AIRelationManager.describe(-30));
    assertEquals("hostile", AIRelationManager.describe(-70));
    assertEquals("hateful", AIRelationManager.describe(-100));
  }

  @Test
  void clear_resetsToDefault() {
    AIRelationManager.set(npc, player, 50);
    AIRelationManager.clear(npc, player);
    assertEquals(0, AIRelationManager.get(npc, player));
  }
}
