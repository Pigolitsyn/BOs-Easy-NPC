/*
 * J3 structure-baking tests. Two registry-free concerns:
 *   1. selectForChunk picks exactly the structures in a given chunk and localizes their x/z.
 *   2. parseSpec splits "id[k=v,...]" into id + ordered property map.
 * BlockState resolution itself (resolveState) needs the Minecraft block registry and is covered
 * by the live in-game check, not here.
 */

package de.markusbordihn.easynpc.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class StructureStamperTest {

  private static WorldData.PlacedBlock block(int x, int y, int z, String name) {
    WorldData.PlacedBlock b = new WorldData.PlacedBlock();
    b.x = x;
    b.y = y;
    b.z = z;
    b.name = name;
    b.properties = new java.util.LinkedHashMap<>();
    return b;
  }

  private static WorldData worldWith(WorldData.PlacedBlock... blocks) {
    WorldData d = new WorldData();
    d.structures = new ArrayList<>(List.of(blocks));
    return d;
  }

  @Test
  void selectsOnlyBlocksInChunkAndLocalizes() {
    // Chunk (1, -1): world x in [16,32), world z in [-16,0).
    WorldData d =
        worldWith(
            block(20, 68, -5, "minecraft:stone"), // in chunk -> local (4, , 11)
            block(16, 70, -16, "minecraft:dirt"), // chunk corner -> local (0, , 0)
            block(31, 64, -1, "minecraft:oak_log"), // far corner -> local (15, , 15)
            block(15, 70, -5, "minecraft:cobblestone"), // x too small -> excluded
            block(32, 70, -5, "minecraft:cobblestone"), // x too big -> excluded
            block(20, 70, 0, "minecraft:cobblestone")); // z too big -> excluded

    List<StructureStamper.LocalSpec> sel = StructureStamper.selectForChunk(d, 1, -1);
    assertEquals(3, sel.size());

    StructureStamper.LocalSpec a = sel.get(0);
    assertEquals(4, a.localX);
    assertEquals(11, a.localZ);
    assertEquals(68, a.worldY);
    assertEquals("minecraft:stone", a.name);

    assertEquals(0, sel.get(1).localX);
    assertEquals(0, sel.get(1).localZ);
    assertEquals(15, sel.get(2).localX);
    assertEquals(15, sel.get(2).localZ);
  }

  @Test
  void localCoordsAlwaysInRange() {
    WorldData d =
        worldWith(
            block(16, 64, 0, "minecraft:stone"),
            block(31, 64, 15, "minecraft:stone"),
            block(24, 64, 7, "minecraft:stone"));
    for (StructureStamper.LocalSpec s : StructureStamper.selectForChunk(d, 1, 0)) {
      assertTrue(s.localX >= 0 && s.localX < 16, "localX out of range: " + s.localX);
      assertTrue(s.localZ >= 0 && s.localZ < 16, "localZ out of range: " + s.localZ);
    }
  }

  @Test
  void emptyAndNullStructuresAreSafe() {
    assertEquals(0, StructureStamper.selectForChunk(null, 0, 0).size());
    assertEquals(0, StructureStamper.selectForChunk(new WorldData(), 0, 0).size());
  }

  @Test
  void negativeWorldCoordsLocalizeCorrectly() {
    // Chunk (-1, -1): world x,z in [-16, 0). World (-16,-1) -> local (0,15).
    WorldData d = worldWith(block(-16, 64, -1, "minecraft:stone"));
    List<StructureStamper.LocalSpec> sel = StructureStamper.selectForChunk(d, -1, -1);
    assertEquals(1, sel.size());
    assertEquals(0, sel.get(0).localX);
    assertEquals(15, sel.get(0).localZ);
  }

  @Test
  void parseSpecBareId() {
    StructureStamper.ParsedSpec p = StructureStamper.parseSpec("minecraft:stone");
    assertEquals("minecraft:stone", p.id);
    assertTrue(p.properties.isEmpty());
  }

  @Test
  void parseSpecSingleProperty() {
    StructureStamper.ParsedSpec p = StructureStamper.parseSpec("minecraft:oak_stairs[facing=north]");
    assertEquals("minecraft:oak_stairs", p.id);
    assertEquals(1, p.properties.size());
    assertEquals("north", p.properties.get("facing"));
  }

  @Test
  void parseSpecMultipleProperties() {
    StructureStamper.ParsedSpec p =
        StructureStamper.parseSpec("minecraft:oak_stairs[facing=north,half=top,waterlogged=false]");
    assertEquals("minecraft:oak_stairs", p.id);
    assertEquals(3, p.properties.size());
    assertEquals("north", p.properties.get("facing"));
    assertEquals("top", p.properties.get("half"));
    assertEquals("false", p.properties.get("waterlogged"));
  }

  @Test
  void parseSpecToleratesWhitespaceAndEmpties() {
    StructureStamper.ParsedSpec p =
        StructureStamper.parseSpec(" minecraft:stone[ facing = east , ] ");
    assertEquals("minecraft:stone", p.id);
    assertEquals("east", p.properties.get("facing"));
  }
}
