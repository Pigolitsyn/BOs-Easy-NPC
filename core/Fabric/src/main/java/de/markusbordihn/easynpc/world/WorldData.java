/*
 * J2: world-data plan (mirror of the Rust generator's --world-data JSON, src/world_data.rs).
 * Field names are load-bearing — the Rust side (R1) writes exactly these. Pure data class,
 * parsed by gson (see WorldDataLoader). Coordinate contract:
 *   - heightmap / biomeMap: row-major index = z * width + x, grid origin = world (0,0),
 *     grid spans world [0,width) x [0,depth). Values in heightmap are REAL world Y.
 *   - structures / anchors / decorSlots: absolute world coords (may be negative). J3+.
 */

package de.markusbordihn.easynpc.world;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class WorldData {

  public int width;
  public int depth;

  @SerializedName("sea_level")
  public int seaLevel;

  /** Row-major (z*width+x), REAL world Y per column. */
  public List<Integer> heightmap;

  /** Row-major (z*width+x), biome id per column (e.g. "lorecraft:japan_blossom"). */
  @SerializedName("biome_map")
  public List<String> biomeMap;

  @SerializedName("surface_palette")
  public SurfacePalette surfacePalette;

  public WaterPlan water;
  public CavePlan caves;
  public List<PlacedBlock> structures;
  public List<Anchor> anchors;

  @SerializedName("decor_slots")
  public List<DecorSlot> decorSlots;

  /** [x, y, z] spawn point. */
  public int[] spawn;

  public long seed;

  public static class SurfacePalette {
    public String surface;
    public String subsurface;
  }

  public static class WaterPlan {
    @SerializedName("sea_level")
    public int seaLevel;
  }

  public static class CavePlan {
    public boolean enabled;
    public float density;
    public long seed;
  }

  public static class PlacedBlock {
    public int x;
    public int y;
    public int z;
    public String name;
    public java.util.Map<String, String> properties;
  }

  public static class Anchor {
    public String name;
    public int x;
    public int y;
    public int z;

    @SerializedName("size_x")
    public int sizeX;

    @SerializedName("size_y")
    public int sizeY;

    @SerializedName("size_z")
    public int sizeZ;

    public int rotation;
  }

  public static class DecorSlot {
    public int x;
    public int y;
    public int z;
    public int w;
    public int h;
    public int d;
  }

  // ---- Convenience accessors used by the generator and tests. ----

  /** Grid index for a (gridX, gridZ) cell, row-major z*width+x. No bounds check. */
  public int index(int gridX, int gridZ) {
    return gridZ * this.width + gridX;
  }

  /** True if world coords (worldX, worldZ) fall inside the [0,width)x[0,depth) grid. */
  public boolean inBounds(int worldX, int worldZ) {
    return worldX >= 0 && worldX < this.width && worldZ >= 0 && worldZ < this.depth;
  }

  /**
   * Real surface Y for a world column. Returns {@link #VOID_HEIGHT} for columns outside the grid:
   * the quest area is an island in the void, so anything beyond [0,width)x[0,depth) has NO terrain
   * (empty column). Callers MUST treat a return of {@code VOID_HEIGHT} as "no blocks in this column"
   * — see LoreChunkGenerator. The sentinel sits below any reachable Y so it never primes terrain.
   */
  public int getHeight(int worldX, int worldZ) {
    if (!inBounds(worldX, worldZ)) {
      return VOID_HEIGHT;
    }
    return this.heightmap.get(index(worldX, worldZ));
  }

  /**
   * Sentinel surface Y for columns outside the quest grid — below the overworld floor (-64) so it
   * reads as "void / no terrain". {@link #getHeight} returns this out of bounds; the generator
   * leaves such columns empty (air from minY up) instead of building a flat sea-level floor.
   */
  public static final int VOID_HEIGHT = Integer.MIN_VALUE;

  /** Biome id for a world column, or {@code null} when outside the grid. */
  public String getBiome(int worldX, int worldZ) {
    if (!inBounds(worldX, worldZ)) {
      return null;
    }
    return this.biomeMap.get(index(worldX, worldZ));
  }

  /** Effective sea level: the nested water plan wins, falling back to the top-level field. */
  public int waterSeaLevel() {
    return this.water != null ? this.water.seaLevel : this.seaLevel;
  }
}
