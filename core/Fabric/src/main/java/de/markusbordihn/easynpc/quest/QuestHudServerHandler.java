/*
 * Task 2: server-side push of quest HUD status to players.
 *
 * - On dimension entry (ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD) and on login
 *   (ServerPlayConnectionEvents.JOIN): if the destination dimension has a registered quest, push a
 *   QuestHudDefinitionMessage carrying live per-stage done flags; otherwise push a clear message.
 * - Every 20 server ticks (~1s): for each online player standing in a registered quest dimension,
 *   recompute status and re-send the definition only when the done-flag hash changed since last poll
 *   (cached per player UUID). Cache is cleaned for dimension changes/clears.
 *
 * Fabric-only (QuestHudRegistry/messages live in Common; the Fabric listeners + NetworkHandler do
 * the wiring). dimKey is the destination/player level dimension id, e.g. "lorecraft:quest_<hex>".
 */

package de.markusbordihn.easynpc.quest;

import de.markusbordihn.easynpc.Constants;
import de.markusbordihn.easynpc.network.NetworkHandlerManager;
import de.markusbordihn.easynpc.network.message.client.QuestHudClearMessage;
import de.markusbordihn.easynpc.network.message.client.QuestHudDefinitionMessage;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class QuestHudServerHandler {

  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);
  private static final int POLL_INTERVAL_TICKS = 20;

  /** player UUID -> last pushed done-flag hash (for change detection in the poll loop). */
  private static final Map<UUID, Long> LAST_HASH = new ConcurrentHashMap<>();

  private static int tickCounter = 0;

  private QuestHudServerHandler() {}

  /** Register dimension-entry + login listeners. Call from EasyNPCMain.onInitialize (server). */
  public static void register() {
    ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(
        (player, origin, destination) ->
            pushForDimension(
                player, destination.dimension().identifier().toString()));

    ServerPlayConnectionEvents.JOIN.register(
        (handler, sender, server) -> {
          ServerPlayer player = handler.player;
          if (player != null) {
            pushForDimension(player, player.level().dimension().identifier().toString());
          }
        });
  }

  /** Push the definition (with status) or a clear, depending on whether dimKey is registered. */
  private static void pushForDimension(ServerPlayer player, String dimKey) {
    MinecraftServer server = player.level().getServer();
    if (server == null) {
      return;
    }
    QuestHudData status = QuestHudRegistry.withStatus(server, player, dimKey);
    if (status != null) {
      NetworkHandlerManager.sendMessageToPlayer(new QuestHudDefinitionMessage(status), player);
      LAST_HASH.put(player.getUUID(), QuestHudRegistry.doneHash(status));
    } else {
      NetworkHandlerManager.sendMessageToPlayer(new QuestHudClearMessage(), player);
      LAST_HASH.remove(player.getUUID());
    }
  }

  /** Poll loop. Hook to ServerTickEvents.END_SERVER_TICK. */
  public static void onServerTick(MinecraftServer server) {
    if (++tickCounter < POLL_INTERVAL_TICKS) {
      return;
    }
    tickCounter = 0;
    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
      String dimKey = player.level().dimension().identifier().toString();
      QuestHudData status = QuestHudRegistry.withStatus(server, player, dimKey);
      if (status == null) {
        // Player not in a registered quest dim; forget its cached hash.
        LAST_HASH.remove(player.getUUID());
        continue;
      }
      long hash = QuestHudRegistry.doneHash(status);
      Long previous = LAST_HASH.get(player.getUUID());
      if (previous == null || previous.longValue() != hash) {
        NetworkHandlerManager.sendMessageToPlayer(new QuestHudDefinitionMessage(status), player);
        LAST_HASH.put(player.getUUID(), hash);
        log.debug(
            "[QuestHud] poll re-sent status to {} (dim {})",
            player.getName().getString(),
            dimKey);
      }
    }
  }
}
