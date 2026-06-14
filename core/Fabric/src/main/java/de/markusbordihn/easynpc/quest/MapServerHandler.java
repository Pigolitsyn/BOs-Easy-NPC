/*
 * MOD-6: server-side push of the mod-rendered map to players.
 *
 * - On dimension entry (ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD) and on login
 *   (ServerPlayConnectionEvents.JOIN): if the destination dimension has a registered map, push a
 *   MapDefinitionMessage; otherwise push a MapClearMessage.
 *
 * Unlike the quest HUD there is no per-tick poll loop — map data is static per dimension, so it is
 * pushed once on entry and re-pushed only on re-entry. Fabric-only (MapRegistry/messages live in
 * Common; the Fabric listeners + NetworkHandler do the wiring). dimKey is the destination/player
 * level dimension id, e.g. "lorecraft:quest_<hex>".
 */

package de.markusbordihn.easynpc.quest;

import de.markusbordihn.easynpc.network.NetworkHandlerManager;
import de.markusbordihn.easynpc.network.message.client.MapClearMessage;
import de.markusbordihn.easynpc.network.message.client.MapDefinitionMessage;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

public final class MapServerHandler {

  private MapServerHandler() {}

  /** Register dimension-entry + login listeners. Call from EasyNPCMain.onInitialize (server). */
  public static void register() {
    ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(
        (player, origin, destination) ->
            pushForDimension(player, destination.dimension().identifier().toString()));

    ServerPlayConnectionEvents.JOIN.register(
        (handler, sender, server) -> {
          ServerPlayer player = handler.player;
          if (player != null) {
            pushForDimension(player, player.level().dimension().identifier().toString());
          }
        });
  }

  /** Push the map definition or a clear, depending on whether dimKey has a registered map. */
  private static void pushForDimension(ServerPlayer player, String dimKey) {
    MapData data = MapRegistry.get(dimKey);
    if (data != null) {
      NetworkHandlerManager.sendMessageToPlayer(new MapDefinitionMessage(data), player);
    } else {
      NetworkHandlerManager.sendMessageToPlayer(new MapClearMessage(), player);
    }
  }
}
