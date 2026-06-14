/*
 * Copyright 2026 Markus Bordihn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.markusbordihn.easynpc.server.ai;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import de.markusbordihn.easynpc.Constants;
import de.markusbordihn.easynpc.security.ParkurCommandWhitelist;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Executes a single command carried by an NPC dialog reply for the issuing player.
 *
 * <p>Defence-in-depth: the command is re-checked against {@link ParkurCommandWhitelist} here even
 * though the caller already validated it. A command that fails the allow-list is logged and dropped
 * — it is NEVER executed. Runs at GAMEMASTERS permission (op level 2) because worldborder/tp require
 * it, scoped to the issuing player via {@code withEntity(serverPlayer)}.
 */
public final class WhitelistedCommandExecutor {

  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  // op level 2 — minimum for worldborder / tp. Hard-coded; not LLM-controllable.
  private static final int PERMISSION_LEVEL = 2;

  private WhitelistedCommandExecutor() {}

  /**
   * Validate {@code rawCommand} against the allow-list and, only if it passes, run it for
   * {@code serverPlayer}. Must be called on the server thread.
   *
   * @return {@code true} if the command was allow-listed and dispatched, {@code false} otherwise.
   */
  public static boolean runForPlayer(ServerPlayer serverPlayer, String rawCommand) {
    if (serverPlayer == null) {
      return false;
    }
    if (!ParkurCommandWhitelist.isAllowed(rawCommand)) {
      log.warn(
          "[AI] Ignoring non-allowlisted NPC command for player {}: {}",
          serverPlayer.getName().getString(),
          rawCommand);
      return false;
    }

    MinecraftServer server = serverPlayer.level().getServer();
    if (server == null) {
      log.error("[AI] No server available to run allowlisted command for {}", serverPlayer);
      return false;
    }

    String command = rawCommand.strip();
    if (command.startsWith("/")) {
      command = command.substring(1).strip();
    }

    Commands commands = server.getCommands();
    // Exact chain verified against CommandExecutor.createPlayerCommandSourceStack
    // (lines 256-266) for MC 1.21.11 Mojang mappings.
    CommandSourceStack source =
        server
            .createCommandSourceStack()
            .withEntity(serverPlayer)
            .withPosition(serverPlayer.position())
            .withRotation(serverPlayer.getRotationVector())
            .withPermission(
                LevelBasedPermissionSet.forLevel(PermissionLevel.byId(PERMISSION_LEVEL)))
            .withLevel(serverPlayer.level())
            .withSuppressedOutput();
    CommandDispatcher<CommandSourceStack> dispatcher = commands.getDispatcher();
    ParseResults<CommandSourceStack> parseResults = dispatcher.parse(command, source);
    log.info(
        "[AI] Running allowlisted NPC command for player {}: {}",
        serverPlayer.getName().getString(),
        command);
    commands.performCommand(parseResults, command);
    return true;
  }
}
