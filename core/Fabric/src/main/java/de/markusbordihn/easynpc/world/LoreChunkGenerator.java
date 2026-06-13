/*
 * J2: LoreChunkGenerator — a real ChunkGenerator (MC 1.21.11, Mojang mappings) that
 * reconstructs terrain + surface + biome + water from a Rust-generated WorldData plan.
 * Evolution of SpikeGenerator (flat stone). Caves and structures are NOT handled here —
 * that is J3. Lives in the Fabric module (Fantasy + runtime dim are Fabric-only).
 *
 * Coordinate contract (see WorldData / src/world_data.rs):
 *   - heightmap[z*width+x] = REAL world Y of the surface for world column (x, z).
 *   - grid origin = world (0,0); grid spans world [0,width) x [0,depth).
 *   - columns outside the grid -> flat void at sea level, biome = default plains.
 * Column fill (top-down):
 *   y == h            -> surface_palette.surface
 *   minY <= y < h     -> surface_palette.subsurface
 *   h < y <= waterSea -> minecraft:water  (only when h < waterSea)
 *   y > max(h,waterSea)-> air (left untouched)
 */

package de.markusbordihn.easynpc.world;

import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.resources.Identifier;

public class LoreChunkGenerator extends ChunkGenerator {

  // Overworld extents (matches BuiltinDimensionTypes.OVERWORLD: -64..319 => 384).
  private static final int MIN_Y = -64;
  private static final int GEN_DEPTH = 384;

  // J3: do not carve the top of the relief away — keep a solid cap below the surface so caves
  // don't punch open the ground. Carve from (surface - this) downward.
  private static final int CAVE_SURFACE_MARGIN = 4;

  private final WorldData worldData;
  private final BlockState surface;
  private final BlockState subsurface;
  private final BlockState water;
  private final BlockState air;
  private final int seaLevel;
  private final boolean cavesEnabled;
  private final double caveDensity;
  private final long caveSeed;

  public LoreChunkGenerator(WorldData worldData, HolderGetter<Biome> biomeLookup) {
    super(new LoreBiomeSource(worldData, biomeLookup));
    this.worldData = worldData;
    this.surface = blockFromId(worldData.surfacePalette.surface, Blocks.GRASS_BLOCK);
    this.subsurface = blockFromId(worldData.surfacePalette.subsurface, Blocks.DIRT);
    this.water = Blocks.WATER.defaultBlockState();
    this.air = Blocks.AIR.defaultBlockState();
    this.seaLevel = worldData.waterSeaLevel();
    WorldData.CavePlan caves = worldData.caves;
    this.cavesEnabled = caves != null && caves.enabled && caves.density > 0.0f;
    this.caveDensity = caves != null ? caves.density : 0.0;
    this.caveSeed = caves != null ? caves.seed : worldData.seed;
  }

  private static BlockState blockFromId(String id, net.minecraft.world.level.block.Block fallback) {
    if (id == null) {
      return fallback.defaultBlockState();
    }
    Identifier rl = Identifier.parse(id);
    net.minecraft.world.level.block.Block block =
        net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(rl);
    // BuiltInRegistries returns AIR for unknown ids; fall back explicitly then.
    if (block == Blocks.AIR && !id.endsWith("air")) {
      return fallback.defaultBlockState();
    }
    return block.defaultBlockState();
  }

  @Override
  protected MapCodec<? extends ChunkGenerator> codec() {
    // Runtime-only generator (Fantasy holds the instance; never round-tripped to disk per
    // the 0.2 spike). A unit codec satisfies the abstract contract without real serialization.
    return MapCodec.unit(() -> this);
  }

  @Override
  public CompletableFuture<ChunkAccess> fillFromNoise(
      Blender blender,
      RandomState randomState,
      StructureManager structureManager,
      ChunkAccess chunk) {
    int chunkMinY = chunk.getMinY();
    int chunkMaxY = chunk.getMaxY();
    int baseX = chunk.getPos().getMinBlockX();
    int baseZ = chunk.getPos().getMinBlockZ();

    Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
    Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);

    for (int lx = 0; lx < 16; lx++) {
      for (int lz = 0; lz < 16; lz++) {
        int worldX = baseX + lx;
        int worldZ = baseZ + lz;
        int h = this.worldData.getHeight(worldX, worldZ);

        // Solid column: subsurface from bottom up to h-1, surface at h.
        int solidTop = Math.min(h, chunkMaxY);
        // J3: caves carve air below the surface cap; never above (carveTop keeps the cap solid).
        int carveTop = h - CAVE_SURFACE_MARGIN;
        for (int y = chunkMinY; y <= solidTop; y++) {
          BlockState state = (y == h) ? this.surface : this.subsurface;
          if (this.cavesEnabled
              && y <= carveTop
              && CaveNoise.isCarved(worldX, y, worldZ, this.caveSeed, this.caveDensity)) {
            state = this.air;
          }
          setBlock(chunk, lx, y, lz, state);
        }

        // Water from h+1 up to sea level, only where the column is below sea level.
        int topPrimed = h;
        if (h < this.seaLevel) {
          int waterTop = Math.min(this.seaLevel, chunkMaxY);
          for (int y = Math.max(h + 1, chunkMinY); y <= waterTop; y++) {
            setBlock(chunk, lx, y, lz, this.water);
          }
          topPrimed = Math.min(this.seaLevel, h); // OCEAN_FLOOR tracks the solid floor at h.
        }

        // Prime heightmaps: OCEAN_FLOOR_WG = solid surface (h); WORLD_SURFACE_WG = top of
        // water/land so light + spawn logic see the right level.
        int oceanY = Math.min(h, chunkMaxY);
        int surfaceY = Math.min(Math.max(h, h < this.seaLevel ? this.seaLevel : h), chunkMaxY);
        oceanFloor.update(lx, oceanY, lz, chunk.getBlockState(new BlockPos(worldX, oceanY, worldZ)));
        worldSurface.update(
            lx, surfaceY, lz, chunk.getBlockState(new BlockPos(worldX, surfaceY, worldZ)));
        // topPrimed referenced to keep intent clear; heightmap updates above are authoritative.
        if (topPrimed < chunkMinY) {
          topPrimed = chunkMinY;
        }
      }
    }

    // J3: bake structures (scenes/roads/trees from Rust) on top of the finished terrain.
    // decor_slots are NOT baked here — the Python LLM pipeline fills them via setblock later.
    StructureStamper.stampChunk(this.worldData, chunk);

    return CompletableFuture.completedFuture(chunk);
  }

  private static void setBlock(ChunkAccess chunk, int lx, int y, int lz, BlockState state) {
    LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
    section.acquire();
    try {
      section.setBlockState(lx, y & 15, lz, state, false);
    } finally {
      section.release();
    }
  }

  @Override
  public void buildSurface(
      WorldGenRegion region,
      StructureManager structureManager,
      RandomState randomState,
      ChunkAccess chunk) {
    // no-op: surface + subsurface already placed in fillFromNoise from the palette.
  }

  @Override
  public void applyCarvers(
      WorldGenRegion region,
      long seed,
      RandomState randomState,
      BiomeManager biomeManager,
      StructureManager structureManager,
      ChunkAccess chunk) {
    // no-op: caves are J3.
  }

  @Override
  public void spawnOriginalMobs(WorldGenRegion region) {
    // no-op.
  }

  @Override
  public int getSeaLevel() {
    return this.seaLevel;
  }

  @Override
  public int getMinY() {
    return MIN_Y;
  }

  @Override
  public int getGenDepth() {
    return GEN_DEPTH;
  }

  @Override
  public int getBaseHeight(
      int x,
      int z,
      Heightmap.Types type,
      LevelHeightAccessor level,
      RandomState randomState) {
    int h = this.worldData.getHeight(x, z);
    // OCEAN_FLOOR family wants the solid floor; surface/motion-blocking want the water top.
    boolean wantsFluid =
        type == Heightmap.Types.WORLD_SURFACE
            || type == Heightmap.Types.WORLD_SURFACE_WG
            || type == Heightmap.Types.MOTION_BLOCKING;
    int top = (wantsFluid && h < this.seaLevel) ? this.seaLevel : h;
    return top + 1; // first free (air) y above the surface.
  }

  @Override
  public NoiseColumn getBaseColumn(
      int x, int z, LevelHeightAccessor level, RandomState randomState) {
    int height = level.getHeight();
    int minY = level.getMinY();
    BlockState[] column = new BlockState[height];
    BlockState air = Blocks.AIR.defaultBlockState();
    int h = this.worldData.getHeight(x, z);
    for (int i = 0; i < height; i++) {
      int y = minY + i;
      if (y < h) {
        column[i] = this.subsurface;
      } else if (y == h) {
        column[i] = this.surface;
      } else if (h < this.seaLevel && y <= this.seaLevel) {
        column[i] = this.water;
      } else {
        column[i] = air;
      }
    }
    return new NoiseColumn(minY, column);
  }

  @Override
  public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
    info.add(
        "LoreChunkGenerator "
            + this.worldData.width
            + "x"
            + this.worldData.depth
            + " sea="
            + this.seaLevel
            + " h="
            + this.worldData.getHeight(pos.getX(), pos.getZ()));
  }

  /**
   * Per-column biome source backed by the WorldData biome_map. Resolves the biome id for a
   * world column to a registry Holder; columns outside the grid fall back to plains.
   */
  static final class LoreBiomeSource extends BiomeSource {

    private final WorldData worldData;
    private final HolderGetter<Biome> biomeLookup;
    private final Holder<Biome> fallback;
    private final java.util.Map<String, Holder<Biome>> resolved = new java.util.HashMap<>();

    LoreBiomeSource(WorldData worldData, HolderGetter<Biome> biomeLookup) {
      this.worldData = worldData;
      this.biomeLookup = biomeLookup;
      this.fallback = biomeLookup.getOrThrow(Biomes.PLAINS);
    }

    private Holder<Biome> resolve(String id) {
      if (id == null) {
        return this.fallback;
      }
      return this.resolved.computeIfAbsent(
          id,
          key -> {
            try {
              Identifier rl = Identifier.parse(key);
              net.minecraft.resources.ResourceKey<Biome> rk =
                  net.minecraft.resources.ResourceKey.create(Registries.BIOME, rl);
              return this.biomeLookup.get(rk).map(h -> (Holder<Biome>) h).orElse(this.fallback);
            } catch (RuntimeException e) {
              return this.fallback;
            }
          });
    }

    @Override
    protected MapCodec<? extends BiomeSource> codec() {
      return MapCodec.unit(() -> this);
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
      // Distinct biomes referenced by the map, plus the fallback.
      java.util.LinkedHashSet<Holder<Biome>> set = new java.util.LinkedHashSet<>();
      set.add(this.fallback);
      if (this.worldData.biomeMap != null) {
        this.worldData.biomeMap.stream().distinct().forEach(id -> set.add(resolve(id)));
      }
      return set.stream();
    }

    @Override
    public Holder<Biome> getNoiseBiome(
        int quartX, int quartY, int quartZ, Climate.Sampler sampler) {
      int worldX = QuartPos.toBlock(quartX);
      int worldZ = QuartPos.toBlock(quartZ);
      String id = this.worldData.getBiome(worldX, worldZ);
      return resolve(id);
    }
  }
}
