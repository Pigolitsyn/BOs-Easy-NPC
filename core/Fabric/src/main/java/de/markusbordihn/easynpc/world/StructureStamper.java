/*
 * J3: bakes Rust-generated structures into chunk generation. Replaces the old RCON
 * "stamp geometry with setblock" step — structure/scene/road/tree blocks now ride into the
 * chunk during fillFromNoise, so they exist the moment a chunk loads.
 *
 * Coordinate contract (see WorldData / src/world_data.rs):
 *   - PlacedBlock x/z are world coords in the SAME frame as the heightmap (origin world 0,0).
 *   - PlacedBlock y is a REAL world Y.
 *   - blocksForChunk(data, pos) returns the subset of `structures` whose world coords fall
 *     inside `pos`, each translated to chunk-local coords (lx,lz in 0..15; y stays world Y).
 *
 * decor_slots are intentionally NOT baked here: decor *content* is produced by the Python LLM
 * pipeline, which fills the slots with setblock into the live dimension after generation. We
 * only carry the empty-slot rectangles in WorldData for that later pass. (TODO J4+: if decor
 * ever ships pre-resolved blocks from Rust, extend blocksForChunk to include them.)
 *
 * BlockState resolution: "minecraft:oak_stairs[facing=north,half=top]" -> Registries.BLOCK
 * lookup + per-property apply. The name/property string split is factored into the pure,
 * registry-free {@link #parseSpec} helper so it is unit-testable without a Minecraft runtime.
 */

package de.markusbordihn.easynpc.world;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkAccess;

public final class StructureStamper {

  private StructureStamper() {}

  /** A structure block resolved to chunk-local coords and a concrete BlockState. */
  public static final class LocalBlock {
    public final int localX; // 0..15
    public final int worldY; // real world Y
    public final int localZ; // 0..15
    public final BlockState state;

    LocalBlock(int localX, int worldY, int localZ, BlockState state) {
      this.localX = localX;
      this.worldY = worldY;
      this.localZ = localZ;
      this.state = state;
    }
  }

  /** A structure block selected for a chunk and translated to local x/z (no state resolved). */
  public static final class LocalSpec {
    public final int localX; // 0..15
    public final int worldY; // real world Y
    public final int localZ; // 0..15
    public final String name;
    public final Map<String, String> properties;

    LocalSpec(int localX, int worldY, int localZ, String name, Map<String, String> properties) {
      this.localX = localX;
      this.worldY = worldY;
      this.localZ = localZ;
      this.name = name;
      this.properties = properties;
    }
  }

  /**
   * Pure chunk selection + coordinate localization: the subset of {@code structures} whose
   * world coords fall inside {@code chunkX,chunkZ} (16x16), with x/z translated to chunk-local
   * (0..15) and y left as a real world Y. No registry access — unit-testable. {@link
   * #blocksForChunk} layers BlockState resolution on top of this.
   */
  public static List<LocalSpec> selectForChunk(WorldData data, int chunkX, int chunkZ) {
    List<LocalSpec> out = new ArrayList<>();
    if (data == null || data.structures == null) {
      return out;
    }
    int baseX = chunkX << 4;
    int baseZ = chunkZ << 4;
    for (WorldData.PlacedBlock b : data.structures) {
      if (b.x < baseX || b.x >= baseX + 16 || b.z < baseZ || b.z >= baseZ + 16) {
        continue;
      }
      out.add(new LocalSpec(b.x - baseX, b.y, b.z - baseZ, b.name, b.properties));
    }
    return out;
  }

  /**
   * Structures whose world coords fall inside {@code chunkX,chunkZ} (16x16 chunk), translated
   * to chunk-local x/z with their BlockState resolved. Returns an empty list when the plan has
   * no structures. Blocks with an unresolvable id are dropped. y is left as a real world Y; the
   * caller clamps it to the chunk's vertical range when placing.
   */
  public static List<LocalBlock> blocksForChunk(WorldData data, int chunkX, int chunkZ) {
    List<LocalBlock> out = new ArrayList<>();
    for (LocalSpec spec : selectForChunk(data, chunkX, chunkZ)) {
      BlockState state = resolveState(spec.name, spec.properties);
      if (state == null) {
        continue;
      }
      out.add(new LocalBlock(spec.localX, spec.worldY, spec.localZ, state));
    }
    return out;
  }

  /**
   * Bake the chunk's structure blocks on top of already-filled terrain. y values outside the
   * chunk's vertical range are skipped. Run AFTER terrain/water fill in fillFromNoise.
   */
  public static void stampChunk(WorldData data, ChunkAccess chunk) {
    int chunkMinY = chunk.getMinY();
    int chunkMaxY = chunk.getMaxY();
    for (LocalBlock lb : blocksForChunk(data, chunk.getPos().x, chunk.getPos().z)) {
      if (lb.worldY < chunkMinY || lb.worldY > chunkMaxY) {
        continue;
      }
      // Section-level write (same approach as LoreChunkGenerator.setBlock) — avoids depending
      // on the ChunkAccess.setBlockState overload, which differs across 1.21.x mappings.
      net.minecraft.world.level.chunk.LevelChunkSection section =
          chunk.getSection(chunk.getSectionIndex(lb.worldY));
      section.acquire();
      try {
        section.setBlockState(lb.localX, lb.worldY & 15, lb.localZ, lb.state, false);
      } finally {
        section.release();
      }
    }
  }

  // ---- BlockState resolution (registry-backed) ----

  /**
   * Resolve "id[prop=val,...]" or a bare "id" + a properties map to a concrete BlockState.
   * Properties from both the bracketed spec and the {@code extra} map are applied (the map
   * wins on conflict, mirroring the Rust PlacedBlock.properties field). Returns {@code null}
   * for an unknown / air-resolving block id so the caller can skip it.
   */
  public static BlockState resolveState(String name, Map<String, String> extra) {
    if (name == null) {
      return null;
    }
    ParsedSpec spec = parseSpec(name);
    Identifier id = Identifier.parse(spec.id);
    Block block = BuiltInRegistries.BLOCK.getValue(id);
    if (block == Blocks.AIR && !spec.id.endsWith("air")) {
      return null; // unknown id resolves to AIR in BuiltInRegistries.
    }
    BlockState state = block.defaultBlockState();

    Map<String, String> props = new LinkedHashMap<>(spec.properties);
    if (extra != null) {
      props.putAll(extra);
    }
    StateDefinition<Block, BlockState> def = block.getStateDefinition();
    for (Map.Entry<String, String> e : props.entrySet()) {
      Property<?> property = def.getProperty(e.getKey());
      if (property == null) {
        continue; // ignore properties the block doesn't have.
      }
      state = applyProperty(state, property, e.getValue());
    }
    return state;
  }

  private static <T extends Comparable<T>> BlockState applyProperty(
      BlockState state, Property<T> property, String value) {
    return property.getValue(value).map(v -> state.setValue(property, v)).orElse(state);
  }

  // ---- Pure, registry-free spec parsing (unit-testable) ----

  /** Result of splitting "id[k=v,...]" into the id and its property map. */
  public static final class ParsedSpec {
    public final String id;
    public final Map<String, String> properties;

    ParsedSpec(String id, Map<String, String> properties) {
      this.id = id;
      this.properties = properties;
    }
  }

  /**
   * Split a block spec like {@code "minecraft:oak_stairs[facing=north,half=top]"} into its id
   * and an ordered property map. A bare id (no brackets) yields an empty map. Pure function —
   * no registry access — so it can be tested without a Minecraft runtime.
   */
  public static ParsedSpec parseSpec(String spec) {
    Map<String, String> props = new LinkedHashMap<>();
    if (spec == null) {
      return new ParsedSpec("", props);
    }
    String trimmed = spec.trim();
    int lb = trimmed.indexOf('[');
    if (lb < 0) {
      return new ParsedSpec(trimmed, props);
    }
    String id = trimmed.substring(0, lb).trim();
    int rb = trimmed.lastIndexOf(']');
    String body = (rb > lb) ? trimmed.substring(lb + 1, rb) : trimmed.substring(lb + 1);
    for (String pair : body.split(",")) {
      if (pair.isEmpty()) {
        continue;
      }
      int eq = pair.indexOf('=');
      if (eq < 0) {
        continue;
      }
      props.put(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
    }
    return new ParsedSpec(id, props);
  }
}
