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
   * Real surface Y for a world column. Returns {@code sea_level} for columns outside the grid
   * (flat void floor at sea level) so the generator never indexes out of range.
   */
  public int getHeight(int worldX, int worldZ) {
    if (!inBounds(worldX, worldZ)) {
      return this.seaLevel;
    }
    return this.heightmap.get(index(worldX, worldZ));
  }

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
