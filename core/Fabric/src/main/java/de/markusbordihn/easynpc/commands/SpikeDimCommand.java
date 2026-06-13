/*
 * SPIKE 0.2 (throwaway): RCON-driven command that opens a persistent Fantasy dimension
 * backed by SpikeGenerator. Proves: Fantasy lib + custom 1.21.11 ChunkGenerator + persistent
 * runtime world via getOrOpenPersistentWorld. Registered as root literal: /spike_dim.
 */

package de.markusbordihn.easynpc.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import de.markusbordihn.easynpc.world.SpikeGenerator;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.RuntimeWorldHandle;

public final class SpikeDimCommand {

  public static final Identifier SPIKE_DIM_ID = Identifier.fromNamespaceAndPath("lorecraft", "spike");

  private SpikeDimCommand() {}

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    LiteralArgumentBuilder<CommandSourceStack> root =
        Commands.literal("spike_dim")
            .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            .executes(ctx -> openSpikeDim(ctx.getSource()));
    dispatcher.register(root);
  }

  private static int openSpikeDim(CommandSourceStack source) {
    MinecraftServer server = source.getServer();
    try {
      Fantasy fantasy = Fantasy.get(server);
      Registry<Biome> biomeRegistry = server.registryAccess().lookupOrThrow(Registries.BIOME);
      SpikeGenerator generator = SpikeGenerator.ofPlains(biomeRegistry);

      RuntimeWorldConfig config =
          new RuntimeWorldConfig()
              .setDimensionType(BuiltinDimensionTypes.OVERWORLD)
              .setGenerator(generator)
              .setSeed(1L);

      RuntimeWorldHandle handle = fantasy.getOrOpenPersistentWorld(SPIKE_DIM_ID, config);
      String dimId = handle.getRegistryKey().identifier().toString();
      source.sendSuccess(
          () -> Component.literal("[SPIKE] persistent dimension open: " + dimId), false);
      return 1;
    } catch (Exception e) {
      source.sendFailure(Component.literal("[SPIKE] failed: " + e));
      return 0;
    }
  }
}
