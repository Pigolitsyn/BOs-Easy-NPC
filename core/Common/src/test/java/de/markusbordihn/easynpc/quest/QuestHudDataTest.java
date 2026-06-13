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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.markusbordihn.easynpc.quest.QuestHudData.StageInfo;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QuestHudDataTest {

  private static final String CONTRACT_JSON =
      "{\"title\":\"The Lost Relic\","
          + "\"stages\":["
          + "{\"id\":1,\"title\":\"Find the elder\",\"task_type\":\"talk\","
          + "\"objective\":\"qABCD_s1\",\"npc\":[10,64,-20]},"
          + "{\"id\":2,\"title\":\"Recover the relic\",\"task_type\":\"fetch\","
          + "\"objective\":\"qABCD_s2\",\"npc\":[100,70,200]}"
          + "]}";

  @Test
  @DisplayName("fromJson parses the frozen HUD-json contract; done defaults to false")
  void fromJsonParsesContract() {
    QuestHudData data = QuestHudData.fromJson(CONTRACT_JSON);
    assertEquals("The Lost Relic", data.title());
    assertEquals(2, data.stages().size());

    StageInfo s1 = data.stages().get(0);
    assertEquals(1, s1.id());
    assertEquals("Find the elder", s1.title());
    assertEquals("talk", s1.taskType());
    assertEquals("qABCD_s1", s1.objective());
    assertEquals(10, s1.npcX());
    assertEquals(64, s1.npcY());
    assertEquals(-20, s1.npcZ());
    assertFalse(s1.done(), "done not in contract -> defaults false");

    StageInfo s2 = data.stages().get(1);
    assertEquals(2, s2.id());
    assertEquals("fetch", s2.taskType());
    assertEquals(100, s2.npcX());
    assertEquals(200, s2.npcZ());
    assertFalse(s2.done());
  }

  @Test
  @DisplayName("write/read round-trips all fields including done")
  void streamCodecRoundTrip() {
    QuestHudData original =
        new QuestHudData(
            "Quest X",
            List.of(
                new StageInfo(1, "A", "talk", "qX_s1", 1, 2, 3, true),
                new StageInfo(2, "B", "kill", "qX_s2", -4, 5, -6, false)));

    FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
    original.write(buffer);
    QuestHudData decoded = QuestHudData.read(buffer);

    assertEquals(original, decoded, "encode->decode must be equal incl done flags");
    assertTrue(decoded.stages().get(0).done());
    assertFalse(decoded.stages().get(1).done());
    assertEquals(-4, decoded.stages().get(1).npcX());
    assertEquals(0, buffer.readableBytes(), "buffer fully consumed");
  }

  @Test
  @DisplayName("QuestHudRegistry put/get/remove and doneHash")
  void registryPutGetRemove() {
    String key = "lorecraft:quest_unittest";
    assertNull(QuestHudRegistry.get(key));

    QuestHudData data =
        new QuestHudData("Q", List.of(new StageInfo(1, "A", "talk", "qZ_s1", 0, 0, 0, false)));
    QuestHudRegistry.put(key, data);
    assertEquals(data, QuestHudRegistry.get(key));

    QuestHudData withDone =
        new QuestHudData("Q", List.of(new StageInfo(1, "A", "talk", "qZ_s1", 0, 0, 0, true)));
    assertTrue(
        QuestHudRegistry.doneHash(data) != QuestHudRegistry.doneHash(withDone),
        "done-flag change must change hash");

    assertEquals(data, QuestHudRegistry.remove(key));
    assertNull(QuestHudRegistry.get(key));
  }
}
