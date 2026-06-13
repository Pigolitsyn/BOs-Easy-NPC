/*
 * J2 data tests: WorldData/WorldDataLoader parse the Rust --world-data contract and expose
 * consistent height/biome lookups. Pure data — no Minecraft runtime; chunk fill is verified
 * live in-game (RCON), not here.
 */

package de.markusbordihn.easynpc.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WorldDataTest {

  // 2x2 world. heightmap row-major z*width+x: (0,0)=65 (1,0)=66 (0,1)=60 (1,1)=70.
  // biome_map row-major: (0,0)=japan (1,0)=wizard (0,1)=plains (1,1)=space.
  private static final String SAMPLE =
      "{"
          + "\"width\":2,\"depth\":2,\"sea_level\":62,"
          + "\"heightmap\":[65,66,60,70],"
          + "\"biome_map\":[\"lorecraft:japan_blossom\",\"lorecraft:wizard_dark\","
          + "\"minecraft:plains\",\"lorecraft:space_void\"],"
          + "\"surface_palette\":{\"surface\":\"minecraft:grass_block\","
          + "\"subsurface\":\"minecraft:dirt\"},"
          + "\"water\":{\"sea_level\":62},"
          + "\"caves\":{\"enabled\":true,\"density\":0.1,\"seed\":42},"
          + "\"structures\":[{\"x\":10,\"y\":68,\"z\":-1,\"name\":\"minecraft:polished_andesite\","
          + "\"properties\":{}}],"
          + "\"anchors\":[{\"name\":\"village\",\"x\":8,\"y\":69,\"z\":-8,"
          + "\"size_x\":24,\"size_y\":24,\"size_z\":24,\"rotation\":0}],"
          + "\"decor_slots\":[{\"x\":17,\"y\":70,\"z\":3,\"w\":3,\"h\":3,\"d\":3}],"
          + "\"spawn\":[1,71,1],\"seed\":42}";

  @Test
  void parsesScalarsAndArrays() {
    WorldData d = WorldDataLoader.fromJson(SAMPLE);
    assertEquals(2, d.width);
    assertEquals(2, d.depth);
    assertEquals(62, d.seaLevel);
    assertEquals(62, d.waterSeaLevel());
    assertEquals(42L, d.seed);
    assertEquals(4, d.heightmap.size());
    assertEquals(4, d.biomeMap.size());
    assertEquals("minecraft:grass_block", d.surfacePalette.surface);
    assertEquals("minecraft:dirt", d.surfacePalette.subsurface);
    assertTrue(d.caves.enabled);
    assertEquals(42L, d.caves.seed);
  }

  @Test
  void parsesStructuresAnchorsDecorSpawn() {
    WorldData d = WorldDataLoader.fromJson(SAMPLE);
    assertEquals(1, d.structures.size());
    assertEquals("minecraft:polished_andesite", d.structures.get(0).name);
    assertEquals(-1, d.structures.get(0).z);
    assertEquals(1, d.anchors.size());
    assertEquals("village", d.anchors.get(0).name);
    assertEquals(24, d.anchors.get(0).sizeX);
    assertEquals(0, d.anchors.get(0).rotation);
    assertEquals(1, d.decorSlots.size());
    assertEquals(3, d.decorSlots.get(0).w);
    assertEquals(1, d.spawn[0]);
    assertEquals(71, d.spawn[1]);
    assertEquals(1, d.spawn[2]);
  }

  @Test
  void heightLookupIsRowMajor() {
    WorldData d = WorldDataLoader.fromJson(SAMPLE);
    // index = z*width + x
    assertEquals(65, d.getHeight(0, 0));
    assertEquals(66, d.getHeight(1, 0));
    assertEquals(60, d.getHeight(0, 1));
    assertEquals(70, d.getHeight(1, 1));
  }

  @Test
  void biomeLookupIsRowMajor() {
    WorldData d = WorldDataLoader.fromJson(SAMPLE);
    assertEquals("lorecraft:japan_blossom", d.getBiome(0, 0));
    assertEquals("lorecraft:wizard_dark", d.getBiome(1, 0));
    assertEquals("minecraft:plains", d.getBiome(0, 1));
    assertEquals("lorecraft:space_void", d.getBiome(1, 1));
  }

  @Test
  void outOfBoundsIsVoidHeightAndNullBiome() {
    WorldData d = WorldDataLoader.fromJson(SAMPLE);
    // Outside the grid is VOID (no terrain), signalled by VOID_HEIGHT — not a sea-level floor.
    assertEquals(WorldData.VOID_HEIGHT, d.getHeight(-1, 0));
    assertEquals(WorldData.VOID_HEIGHT, d.getHeight(0, 99));
    assertEquals(WorldData.VOID_HEIGHT, d.getHeight(2, 2));
    assertNull(d.getBiome(-1, 0));
    assertNull(d.getBiome(2, 0));
  }

  @Test
  void inBoundsMatchesGrid() {
    WorldData d = WorldDataLoader.fromJson(SAMPLE);
    assertTrue(d.inBounds(0, 0));
    assertTrue(d.inBounds(1, 1));
    assertTrue(!d.inBounds(2, 0));
    assertTrue(!d.inBounds(-1, 1));
  }

  @Test
  void rejectsMismatchedHeightmapSize() {
    String bad = SAMPLE.replace("[65,66,60,70]", "[65,66,60]");
    assertThrows(IllegalArgumentException.class, () -> WorldDataLoader.fromJson(bad));
  }

  @Test
  void rejectsMismatchedBiomeMapSize() {
    String bad =
        SAMPLE.replace(
            "\"minecraft:plains\",\"lorecraft:space_void\"", "\"minecraft:plains\"");
    assertThrows(IllegalArgumentException.class, () -> WorldDataLoader.fromJson(bad));
  }
}
