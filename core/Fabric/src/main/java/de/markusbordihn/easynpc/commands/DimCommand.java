/*
 * J4 production commands: `/lorecraft dim create|delete|tp`.
 *
 * Frozen contract (Python P2/P3 writes against these exact names/args — DO NOT rename):
 *   lorecraft dim create <quest_id:word> <data_path:string> <seed:long>
 *   lorecraft dim delete <quest_id:word>
 *   lorecraft dim tp <player:entity> <quest_id:word> [<x:int> <y:int> <z:int>]
 *
 * Backed by Fantasy persistent runtime worlds (lorecraft:quest_<id>) + LoreChunkGenerator.
 * Fabric-only (Fantasy is not on the Common/NeoForge classpath). Permission: gamemaster.
 *
 * dimTypeForData: v1 uses BuiltinDimensionTypes.OVERWORLD for every quest dim — the themed
 * biomes already supply ambient/sky atmosphere; ambient_light/has_ceiling/coordinate_scale are
 * left at overworld defaults. TODO(v2): per-theme custom dimension types.
 */

package de.markusbordihn.easynpc.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.markusbordihn.easynpc.world.LoreChunkGenerator;
import de.markusbordihn.easynpc.world.WorldData;
import de.markusbordihn.easynpc.world.WorldDataLoader;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

public final class DimCommand {

  public static final String NAMESPACE = "lorecraft";
  public static final String QUEST_PREFIX = "quest_";

  /**
   * quest_id -> live handle for the current server session. Lets delete() find the deletable
   * handle for a dim it opened. Empty after a restart (handles aren't persisted) — delete then
   * falls back to looking the world up on the live server and re-wrapping it.
   */
  private static final Map<String, RuntimeWorldHandle> HANDLES = new ConcurrentHashMap<>();

  private DimCommand() {}

  /** Full dimension Identifier for a quest id: lorecraft:quest_<id>. */
  public static Identifier dimId(String questId) {
    return Identifier.fromNamespaceAndPath(NAMESPACE, QUEST_PREFIX + questId);
  }

  /** Level ResourceKey for a quest id. */
  public static ResourceKey<Level> dimKey(String questId) {
    return ResourceKey.create(Registries.DIMENSION, dimId(questId));
  }

  /** v1: every quest dim is an overworld-type dim; biomes carry the atmosphere. */
  private static ResourceKey<DimensionType> dimTypeForData(WorldData data) {
    return BuiltinDimensionTypes.OVERWORLD;
  }

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    // create <quest_id> <data_path> <seed>
    LiteralArgumentBuilder<CommandSourceStack> create =
        Commands.literal("create")
            .then(
                Commands.argument("quest_id", StringArgumentType.word())
                    .then(
                        Commands.argument("data_path", StringArgumentType.string())
                            .then(
                                Commands.argument("seed", LongArgumentType.longArg())
                                    .executes(DimCommand::create))));

    // delete <quest_id>
    LiteralArgumentBuilder<CommandSourceStack> delete =
        Commands.literal("delete")
            .then(
                Commands.argument("quest_id", StringArgumentType.word())
                    .executes(DimCommand::delete));

    // tp <player> <quest_id> [<x> <y> <z>]
    LiteralArgumentBuilder<CommandSourceStack> tp =
        Commands.literal("tp")
            .then(
                Commands.argument("player", EntityArgument.player())
                    .then(
                        Commands.argument("quest_id", StringArgumentType.word())
                            .executes(ctx -> tp(ctx, false))
                            .then(
                                Commands.argument("x", IntegerArgumentType.integer())
                                    .then(
                                        Commands.argument("y", IntegerArgumentType.integer())
                                            .then(
                                                Commands.argument("z", IntegerArgumentType.integer())
                                                    .executes(ctx -> tp(ctx, true)))))));

    LiteralArgumentBuilder<CommandSourceStack> root =
        Commands.literal("lorecraft")
            .requires(
                source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            .then(Commands.literal("dim").then(create).then(delete).then(tp));
    dispatcher.register(root);
  }

  // ---- create ----

  private static int create(CommandContext<CommandSourceStack> ctx) {
    CommandSourceStack source = ctx.getSource();
    String questId = StringArgumentType.getString(ctx, "quest_id");
    String dataPath = StringArgumentType.getString(ctx, "data_path");
    long seed = LongArgumentType.getLong(ctx, "seed");
    MinecraftServer server = source.getServer();
    try {
      WorldData worldData = WorldDataLoader.load(Path.of(dataPath));
      HolderGetter<Biome> biomeLookup = server.registryAccess().lookupOrThrow(Registries.BIOME);
      LoreChunkGenerator generator = new LoreChunkGenerator(worldData, biomeLookup);

      RuntimeWorldConfig config =
          new RuntimeWorldConfig()
              .setDimensionType(dimTypeForData(worldData))
              .setGenerator(generator)
              .setSeed(seed);

      Fantasy fantasy = Fantasy.get(server);
      // Idempotent: re-opening an existing dim returns the same world (config ignored).
      RuntimeWorldHandle handle = fantasy.getOrOpenPersistentWorld(dimId(questId), config);
      HANDLES.put(questId, handle);

      String dimIdStr = handle.getRegistryKey().identifier().toString();
      source.sendSuccess(() -> Component.literal("created " + dimIdStr), false);
      return 1;
    } catch (Exception e) {
      source.sendFailure(Component.literal("create failed: " + e));
      return 0;
    }
  }

  // ---- delete ----

  private static int delete(CommandContext<CommandSourceStack> ctx) {
    CommandSourceStack source = ctx.getSource();
    String questId = StringArgumentType.getString(ctx, "quest_id");
    MinecraftServer server = source.getServer();
    try {
      HANDLES.remove(questId);
      // Resolve the live world directly — the handle is just a wrapper over this ServerLevel,
      // and delete() only needs the level. Using server.getLevel keeps delete correct even
      // across restarts / when the handle wasn't tracked this session.
      ServerLevel level = server.getLevel(dimKey(questId));
      if (level == null) {
        source.sendSuccess(() -> Component.literal("not found"), false);
        return 1;
      }

      Fantasy fantasy = Fantasy.get(server);
      // delete() enqueues the world for deletion on the next Fantasy tick. The JiJ'd Fantasy's
      // own START_SERVER_TICK handler does not reliably fire in this setup, so we deterministically
      // drive the deletion ourselves via the public tickDeleteWorld(), on the server thread.
      RuntimeWorldHandle handle =
          fantasy.getOrOpenPersistentWorld(dimId(questId), placeholderConfig());
      handle.delete(); // clears player/ticket state + enqueues
      server.execute(() -> fantasy.tickDeleteWorld(level)); // unregister + delete files now

      source.sendSuccess(() -> Component.literal("deleted"), false);
      return 1;
    } catch (Exception e) {
      source.sendFailure(Component.literal("delete failed: " + e));
      return 0;
    }
  }

  /**
   * Config only used if a world unexpectedly needs constructing during delete (it won't for an
   * already-loaded world). Trivial flat generator so it never throws.
   */
  private static RuntimeWorldConfig placeholderConfig() {
    return new RuntimeWorldConfig()
        .setDimensionType(BuiltinDimensionTypes.OVERWORLD)
        .setSeed(0L);
  }

  // ---- tp ----

  private static int tp(CommandContext<CommandSourceStack> ctx, boolean explicitCoords) {
    CommandSourceStack source = ctx.getSource();
    String questId = StringArgumentType.getString(ctx, "quest_id");
    MinecraftServer server = source.getServer();
    try {
      ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
      ServerLevel level = server.getLevel(dimKey(questId));
      if (level == null) {
        source.sendFailure(
            Component.literal("unknown dimension " + dimId(questId) + " (run create first)"));
        return 0;
      }

      double x;
      double y;
      double z;
      if (explicitCoords) {
        x = IntegerArgumentType.getInteger(ctx, "x") + 0.5;
        y = IntegerArgumentType.getInteger(ctx, "y");
        z = IntegerArgumentType.getInteger(ctx, "z") + 0.5;
      } else if (level instanceof xyz.nucleoid.fantasy.RuntimeWorld) {
        // Default to the world-data spawn carried by the generator, when available.
        int[] spawn = spawnOf(level);
        if (spawn != null) {
          x = spawn[0] + 0.5;
          y = spawn[1];
          z = spawn[2] + 0.5;
        } else {
          x = 0.5;
          y = 65;
          z = 0.5;
        }
      } else {
        x = 0.5;
        y = 65;
        z = 0.5;
      }

      // Cross-dim teleport (TeleportCommand-style): single-arg overload moves the player into
      // the target ServerLevel at the absolute coords, preserving look angles.
      player.teleportTo(
          level, x, y, z, Set.<Relative>of(), player.getYRot(), player.getXRot(), false);

      final double fx = x;
      final double fy = y;
      final double fz = z;
      source.sendSuccess(
          () ->
              Component.literal(
                  "teleported "
                      + player.getName().getString()
                      + " to "
                      + dimId(questId)
                      + " "
                      + (int) fx
                      + " "
                      + (int) fy
                      + " "
                      + (int) fz),
          false);
      return 1;
    } catch (CommandSyntaxException e) {
      // No matching player (e.g. nobody online) — parsed fine, just nothing to move.
      source.sendFailure(Component.literal(e.getMessage()));
      return 0;
    } catch (Exception e) {
      source.sendFailure(Component.literal("tp failed: " + e));
      return 0;
    }
  }

  /** Pull the [x,y,z] spawn from a LoreChunkGenerator-backed level, or null. */
  private static int[] spawnOf(ServerLevel level) {
    if (level.getChunkSource().getGenerator() instanceof LoreChunkGenerator gen) {
      return gen.spawn();
    }
    return null;
  }
}
