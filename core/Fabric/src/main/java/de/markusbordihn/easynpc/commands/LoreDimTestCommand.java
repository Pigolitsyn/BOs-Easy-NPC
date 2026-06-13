/*
 * J2 test harness command: `/easy_npc lore-dim-test <data_path>`.
 * Loads a Rust WorldData plan from <data_path> and opens a persistent Fantasy dimension
 * `lorecraft:loretest` backed by LoreChunkGenerator, so the generator can be exercised live
 * via RCON. Temporary scaffolding — the full `lorecraft dim create/delete/tp` suite is J4.
 *
 * Registered as a child of the existing `easy_npc` root (Brigadier merges same-named literals
 * across registrations), so it appears under /easy_npc without pulling Fantasy into Common.
 */

package de.markusbordihn.easynpc.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import de.markusbordihn.easynpc.world.LoreChunkGenerator;
import de.markusbordihn.easynpc.world.WorldData;
import de.markusbordihn.easynpc.world.WorldDataLoader;
import java.nio.file.Path;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

public final class LoreDimTestCommand {

  public static final Identifier LORE_TEST_DIM_ID =
      Identifier.fromNamespaceAndPath("lorecraft", "loretest");

  private LoreDimTestCommand() {}

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    LiteralArgumentBuilder<CommandSourceStack> root =
        Commands.literal("easy_npc")
            .then(
                Commands.literal("lore-dim-test")
                    .requires(
                        source ->
                            source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .then(
                        Commands.argument("data_path", StringArgumentType.greedyString())
                            .executes(LoreDimTestCommand::run)));
    dispatcher.register(root);
  }

  private static int run(CommandContext<CommandSourceStack> ctx) {
    CommandSourceStack source = ctx.getSource();
    String dataPath = StringArgumentType.getString(ctx, "data_path");
    MinecraftServer server = source.getServer();
    try {
      WorldData worldData = WorldDataLoader.load(Path.of(dataPath));
      HolderGetter<Biome> biomeLookup = server.registryAccess().lookupOrThrow(Registries.BIOME);
      LoreChunkGenerator generator = new LoreChunkGenerator(worldData, biomeLookup);

      RuntimeWorldConfig config =
          new RuntimeWorldConfig()
              .setDimensionType(BuiltinDimensionTypes.OVERWORLD)
              .setGenerator(generator)
              .setSeed(worldData.seed);

      Fantasy fantasy = Fantasy.get(server);
      RuntimeWorldHandle handle = fantasy.getOrOpenPersistentWorld(LORE_TEST_DIM_ID, config);
      String dimId = handle.getRegistryKey().identifier().toString();
      source.sendSuccess(
          () ->
              Component.literal(
                  "[lore-dim-test] open "
                      + dimId
                      + " ("
                      + worldData.width
                      + "x"
                      + worldData.depth
                      + ", sea="
                      + worldData.waterSeaLevel()
                      + ", biomes="
                      + worldData.biomeMap.stream().distinct().count()
                      + ")"),
          false);
      return 1;
    } catch (Exception e) {
      source.sendFailure(Component.literal("[lore-dim-test] failed: " + e));
      return 0;
    }
  }
}
