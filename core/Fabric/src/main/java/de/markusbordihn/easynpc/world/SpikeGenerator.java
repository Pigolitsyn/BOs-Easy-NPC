/*
 * SPIKE 0.2 (throwaway): trivial flat-stone ChunkGenerator for MC 1.21.11.
 * Fills stone from world bottom up to y=64, biome minecraft:plains. Mojang mappings.
 */

package de.markusbordihn.easynpc.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.StructureManager;

public class SpikeGenerator extends ChunkGenerator {

  // Floor height: stone up to (and including) y=64, so y=65 is the first air block.
  private static final int FLOOR_TOP_Y = 65;

  // MapCodec: required so Fantasy/engine can (de)serialize the generator. Captures the
  // plains biome holder from the biome registry and rebuilds a FixedBiomeSource.
  public static final MapCodec<SpikeGenerator> CODEC =
      RecordCodecBuilder.mapCodec(
          instance ->
              instance
                  .group(
                      Biome.CODEC
                          .fieldOf("biome")
                          .forGetter(gen -> ((FixedBiomeSource) gen.biomeSource).getNoiseBiome(0, 0, 0)))
                  .apply(instance, SpikeGenerator::new));

  public SpikeGenerator(Holder<Biome> biome) {
    super(new FixedBiomeSource(biome));
  }

  /** Convenience factory resolving plains from the server biome registry. */
  public static SpikeGenerator ofPlains(HolderGetter<Biome> biomeGetter) {
    return new SpikeGenerator(biomeGetter.getOrThrow(Biomes.PLAINS));
  }

  @Override
  protected MapCodec<? extends ChunkGenerator> codec() {
    return CODEC;
  }

  @Override
  public CompletableFuture<ChunkAccess> fillFromNoise(
      Blender blender,
      RandomState randomState,
      StructureManager structureManager,
      ChunkAccess chunk) {
    int minY = chunk.getMinY();
    int floorTop = Math.min(FLOOR_TOP_Y - 1, chunk.getMaxY());
    BlockState stone = Blocks.STONE.defaultBlockState();
    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    int baseX = chunk.getPos().getMinBlockX();
    int baseZ = chunk.getPos().getMinBlockZ();

    for (int y = minY; y <= floorTop; y++) {
      LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(y));
      section.acquire();
      try {
        int localY = y & 15;
        for (int x = 0; x < 16; x++) {
          for (int z = 0; z < 16; z++) {
            section.setBlockState(x, localY, z, stone, false);
          }
        }
      } finally {
        section.release();
      }
    }

    // Prime heightmaps so light/spawn logic sees the floor.
    Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
    Heightmap worldSurface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
    for (int x = 0; x < 16; x++) {
      for (int z = 0; z < 16; z++) {
        pos.set(baseX + x, floorTop, baseZ + z);
        oceanFloor.update(x, floorTop, z, stone);
        worldSurface.update(x, floorTop, z, stone);
      }
    }

    return CompletableFuture.completedFuture(chunk);
  }

  @Override
  public void buildSurface(
      WorldGenRegion region,
      StructureManager structureManager,
      RandomState randomState,
      ChunkAccess chunk) {
    // no-op: floor is already stone from fillFromNoise.
  }

  @Override
  public void applyCarvers(
      WorldGenRegion region,
      long seed,
      RandomState randomState,
      BiomeManager biomeManager,
      StructureManager structureManager,
      ChunkAccess chunk) {
    // no-op: no caves in the spike dimension.
  }

  @Override
  public void spawnOriginalMobs(WorldGenRegion region) {
    // no-op: no natural mob spawning.
  }

  @Override
  public int getSeaLevel() {
    return -63;
  }

  @Override
  public int getMinY() {
    return -64;
  }

  @Override
  public int getGenDepth() {
    // Total world height (matches overworld dimension type: -64..319 => 384).
    return 384;
  }

  @Override
  public int getBaseHeight(
      int x,
      int z,
      Heightmap.Types type,
      LevelHeightAccessor level,
      RandomState randomState) {
    return FLOOR_TOP_Y;
  }

  @Override
  public NoiseColumn getBaseColumn(
      int x, int z, LevelHeightAccessor level, RandomState randomState) {
    int height = level.getHeight();
    BlockState[] column = new BlockState[height];
    BlockState stone = Blocks.STONE.defaultBlockState();
    BlockState air = Blocks.AIR.defaultBlockState();
    int minY = level.getMinY();
    for (int i = 0; i < height; i++) {
      int y = minY + i;
      column[i] = (y < FLOOR_TOP_Y) ? stone : air;
    }
    return new NoiseColumn(minY, column);
  }

  @Override
  public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
    info.add("SpikeGenerator (flat stone <= y=" + (FLOOR_TOP_Y - 1) + ")");
  }
}
