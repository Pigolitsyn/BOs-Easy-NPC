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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import de.markusbordihn.easynpc.quest.MapData.Pin;
import io.netty.buffer.Unpooled;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MapDataTest {

  private static final String CONTRACT_JSON =
      "{\"title\":\"Карта: Древний лес\","
          + "\"origin\":[100,-200],"
          + "\"span\":[512,512],"
          + "\"pins\":["
          + "{\"id\":1,\"label\":\"Старый дуб\",\"x\":300,\"z\":-50,\"kind\":\"objective\"},"
          + "{\"id\":2,\"label\":\"Картограф\",\"x\":120,\"z\":100,\"kind\":\"npc\"}"
          + "]}";

  @Test
  @DisplayName("fromJson parses the frozen map-json contract")
  void fromJsonParsesContract() {
    MapData data = MapData.fromJson(CONTRACT_JSON);
    assertEquals("Карта: Древний лес", data.title());
    assertEquals(100, data.originX());
    assertEquals(-200, data.originZ());
    assertEquals(512, data.spanX());
    assertEquals(512, data.spanZ());
    assertEquals(2, data.pins().size());

    Pin p1 = data.pins().get(0);
    assertEquals(1, p1.id());
    assertEquals("Старый дуб", p1.label());
    assertEquals(300, p1.x());
    assertEquals(-50, p1.z());
    assertEquals("objective", p1.kind());

    Pin p2 = data.pins().get(1);
    assertEquals(2, p2.id());
    assertEquals("npc", p2.kind());
  }

  @Test
  @DisplayName("fromJson tolerates missing optional fields with safe defaults")
  void fromJsonDefaults() {
    MapData data = MapData.fromJson("{}");
    assertEquals("", data.title());
    assertEquals(0, data.originX());
    assertEquals(0, data.originZ());
    assertEquals(1, data.spanX(), "missing span defaults to 1 (no divide-by-zero)");
    assertEquals(1, data.spanZ());
    assertTrue(data.pins().isEmpty());
  }

  @Test
  @DisplayName("fromJson clamps non-positive span to 1")
  void fromJsonClampsSpan() {
    MapData data = MapData.fromJson("{\"span\":[0,-5]}");
    assertEquals(1, data.spanX());
    assertEquals(1, data.spanZ());
  }

  @Test
  @DisplayName("pin without kind defaults to empty string (renders default style)")
  void fromJsonPinDefaultKind() {
    MapData data = MapData.fromJson("{\"pins\":[{\"id\":3,\"x\":1,\"z\":2}]}");
    assertEquals(1, data.pins().size());
    Pin pin = data.pins().get(0);
    assertEquals("", pin.kind());
    assertEquals("", pin.label());
  }

  @Test
  @DisplayName("write/read round-trips all fields including negative coords")
  void streamCodecRoundTrip() {
    MapData original =
        new MapData(
            "Map X",
            -100,
            50,
            256,
            300,
            List.of(
                new Pin(1, "A", -10, 20, "objective"),
                new Pin(2, "B", 30, -40, "")));

    FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
    original.write(buffer);
    MapData decoded = MapData.read(buffer);

    assertEquals(original, decoded, "encode->decode must be equal");
    assertEquals(-10, decoded.pins().get(0).x());
    assertEquals(-40, decoded.pins().get(1).z());
    assertEquals(0, buffer.readableBytes(), "buffer fully consumed");
  }

  @Test
  @DisplayName("MapRegistry put/get/remove")
  void registryPutGetRemove() {
    String key = "lorecraft:quest_maptest";
    assertNull(MapRegistry.get(key));

    MapData data = new MapData("M", 0, 0, 16, 16, List.of(new Pin(1, "A", 1, 1, "npc")));
    MapRegistry.put(key, data);
    assertEquals(data, MapRegistry.get(key));

    assertEquals(data, MapRegistry.remove(key));
    assertNull(MapRegistry.get(key));
  }
}
