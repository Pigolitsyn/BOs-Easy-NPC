/*
 * MOD-6 production commands: `/lorecraft map load|unload`.
 *
 * Frozen contract (Python / QM-4 writes against these exact names/args — DO NOT rename):
 *   lorecraft map load <hex:word> <path:string>
 *   lorecraft map unload <hex:word>
 *
 * load reads the map-json contract from <path> (resolved against the server process CWD, same as
 * QuestCommand; the controller passes container paths like /data/map_<hex>.json — pass it quoted),
 * parses it via MapData.fromJson and registers it under dimKey "lorecraft:quest_<hex>"
 * (QuestCommand.dimKey). unload removes that registration. Fabric-only. Permission: gamemaster.
 */

package de.markusbordihn.easynpc.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import de.markusbordihn.easynpc.quest.MapData;
import de.markusbordihn.easynpc.quest.MapRegistry;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

public final class MapCommand {

  private MapCommand() {}

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    // load <hex> <path>
    LiteralArgumentBuilder<CommandSourceStack> load =
        Commands.literal("load")
            .then(
                Commands.argument("hex", StringArgumentType.word())
                    .then(
                        Commands.argument("path", StringArgumentType.string())
                            .executes(MapCommand::load)));

    // unload <hex>
    LiteralArgumentBuilder<CommandSourceStack> unload =
        Commands.literal("unload")
            .then(
                Commands.argument("hex", StringArgumentType.word())
                    .executes(MapCommand::unload));

    LiteralArgumentBuilder<CommandSourceStack> root =
        Commands.literal(DimCommand.NAMESPACE)
            .requires(
                source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            .then(Commands.literal("map").then(load).then(unload));
    dispatcher.register(root);
  }

  private static int load(CommandContext<CommandSourceStack> ctx) {
    CommandSourceStack source = ctx.getSource();
    String hex = StringArgumentType.getString(ctx, "hex");
    String path = StringArgumentType.getString(ctx, "path");
    try {
      String json = Files.readString(Path.of(path), StandardCharsets.UTF_8);
      MapData data = MapData.fromJson(json);
      String key = QuestCommand.dimKey(hex);
      MapRegistry.put(key, data);
      source.sendSuccess(
          () ->
              Component.literal(
                  "loaded map "
                      + key
                      + " ("
                      + data.pins().size()
                      + " pin(s)): "
                      + data.title()),
          false);
      return 1;
    } catch (Exception e) {
      source.sendFailure(Component.literal("map load failed: " + e));
      return 0;
    }
  }

  private static int unload(CommandContext<CommandSourceStack> ctx) {
    CommandSourceStack source = ctx.getSource();
    String hex = StringArgumentType.getString(ctx, "hex");
    String key = QuestCommand.dimKey(hex);
    MapData removed = MapRegistry.remove(key);
    if (removed == null) {
      source.sendSuccess(() -> Component.literal("map " + key + " not loaded"), false);
    } else {
      source.sendSuccess(() -> Component.literal("unloaded map " + key), false);
    }
    return 1;
  }
}
