/*
 * Task 2 production commands: `/lorecraft quest load|unload`.
 *
 * Frozen contract (Python writes against these exact names/args — DO NOT rename):
 *   lorecraft quest load <hex:word> <path:string>
 *   lorecraft quest unload <hex:word>
 *
 * load reads the HUD-json contract from <path> (resolved against the server process CWD, same as
 * DimCommand; the controller passes container paths like /data/quest_<hex>.json — pass it quoted),
 * parses it via QuestHudData.fromJson and registers it under dimKey "lorecraft:quest_<hex>".
 * unload removes that registration. Fabric-only. Permission: gamemaster.
 */

package de.markusbordihn.easynpc.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import de.markusbordihn.easynpc.quest.QuestHudData;
import de.markusbordihn.easynpc.quest.QuestHudRegistry;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

public final class QuestCommand {

  /** dimKey for a quest hex: lorecraft:quest_<hex> (matches DimCommand.dimId). */
  public static String dimKey(String hex) {
    return DimCommand.NAMESPACE + ":" + DimCommand.QUEST_PREFIX + hex;
  }

  private QuestCommand() {}

  public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    // load <hex> <path>
    LiteralArgumentBuilder<CommandSourceStack> load =
        Commands.literal("load")
            .then(
                Commands.argument("hex", StringArgumentType.word())
                    .then(
                        Commands.argument("path", StringArgumentType.string())
                            .executes(QuestCommand::load)));

    // unload <hex>
    LiteralArgumentBuilder<CommandSourceStack> unload =
        Commands.literal("unload")
            .then(
                Commands.argument("hex", StringArgumentType.word())
                    .executes(QuestCommand::unload));

    LiteralArgumentBuilder<CommandSourceStack> root =
        Commands.literal(DimCommand.NAMESPACE)
            .requires(
                source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
            .then(Commands.literal("quest").then(load).then(unload));
    dispatcher.register(root);
  }

  private static int load(CommandContext<CommandSourceStack> ctx) {
    CommandSourceStack source = ctx.getSource();
    String hex = StringArgumentType.getString(ctx, "hex");
    String path = StringArgumentType.getString(ctx, "path");
    try {
      String json = Files.readString(Path.of(path), StandardCharsets.UTF_8);
      QuestHudData data = QuestHudData.fromJson(json);
      String key = dimKey(hex);
      QuestHudRegistry.put(key, data);
      source.sendSuccess(
          () ->
              Component.literal(
                  "loaded quest "
                      + key
                      + " ("
                      + data.stages().size()
                      + " stage(s)): "
                      + data.title()),
          false);
      return 1;
    } catch (Exception e) {
      source.sendFailure(Component.literal("quest load failed: " + e));
      return 0;
    }
  }

  private static int unload(CommandContext<CommandSourceStack> ctx) {
    CommandSourceStack source = ctx.getSource();
    String hex = StringArgumentType.getString(ctx, "hex");
    String key = dimKey(hex);
    QuestHudData removed = QuestHudRegistry.remove(key);
    if (removed == null) {
      source.sendSuccess(() -> Component.literal("quest " + key + " not loaded"), false);
    } else {
      source.sendSuccess(() -> Component.literal("unloaded quest " + key), false);
    }
    return 1;
  }
}
