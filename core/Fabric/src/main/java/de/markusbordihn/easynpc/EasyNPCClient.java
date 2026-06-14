/*
 * Copyright 2023 Markus Bordihn
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

package de.markusbordihn.easynpc;

import de.markusbordihn.easynpc.client.ClientEventHandler;
import de.markusbordihn.easynpc.client.hud.QuestHudOverlay;
import de.markusbordihn.easynpc.client.map.MapScreen;
import de.markusbordihn.easynpc.client.map.MapState;
import de.markusbordihn.easynpc.client.model.ModModelLayer;
import de.markusbordihn.easynpc.client.renderer.BlockEntityRenderer;
import de.markusbordihn.easynpc.client.renderer.EntityRenderer;
import de.markusbordihn.easynpc.client.screen.ClientScreens;
import de.markusbordihn.easynpc.entity.LivingEntityEventHandler;
import de.markusbordihn.easynpc.network.NetworkHandlerManager;
import de.markusbordihn.easynpc.network.NetworkHandlerManagerType;
import de.markusbordihn.easynpc.network.NetworkMessageHandlerManager;
import de.markusbordihn.easynpc.network.ServerNetworkMessageHandler;
import de.markusbordihn.easynpc.client.hud.QuestHudState;
import de.markusbordihn.easynpc.tabs.ModTabs;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class EasyNPCClient implements ClientModInitializer {

  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);

  @Override
  public void onInitializeClient() {
    log.info("Initializing {} (Fabric-Client) ...", Constants.MOD_NAME);

    log.info("{} Renderer ...", Constants.LOG_REGISTER_PREFIX);
    BlockEntityRenderer.register();
    BlockEntityRenderer.registerRenderLayers();
    EntityRenderer.register();

    log.info("{} Entity Layer Definitions ...", Constants.LOG_REGISTER_PREFIX);
    ModModelLayer.registerEntityLayerDefinitions();

    log.info("{} Entity Client Events ...", Constants.LOG_REGISTER_PREFIX);
    LivingEntityEventHandler.registerClientEntityEvents();

    log.info("{} Tabs ...", Constants.LOG_REGISTER_PREFIX);
    ModTabs.handleCreativeModeTabRegister();

    log.info("{} Client Network Handler ...", Constants.LOG_REGISTER_PREFIX);
    NetworkHandlerManager.registerNetworkMessages(NetworkHandlerManagerType.CLIENT);
    NetworkMessageHandlerManager.registerServerHandler(new ServerNetworkMessageHandler());

    log.info("{} Client Screens ...", Constants.LOG_REGISTER_PREFIX);
    ClientScreens.registerScreens();

    log.info("{} Client Event Handler ...", Constants.LOG_REGISTER_PREFIX);
    ClientEventHandler.registerClientEvents();

    log.info("{} Quest HUD Overlay ...", Constants.LOG_REGISTER_PREFIX);
    HudElementRegistry.addLast(
        Identifier.fromNamespaceAndPath(Constants.MOD_ID, "quest_hud"), new QuestHudOverlay());

    log.info("{} Quest HUD Toggle Keybind ...", Constants.LOG_REGISTER_PREFIX);
    registerQuestHudToggle();

    log.info("{} Map Screen Keybind ...", Constants.LOG_REGISTER_PREFIX);
    registerMapToggle();
  }

  /** MOD-6: bind [M] (default, MISC category) to open the mod-rendered map when one is available. */
  private static void registerMapToggle() {
    KeyMapping open =
        KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                "key.easy_npc.map_open",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                KeyMapping.Category.MISC));
    ClientTickEvents.END_CLIENT_TICK.register(
        client -> {
          while (open.consumeClick()) {
            Minecraft minecraft = Minecraft.getInstance();
            // Only open from in-world (no other screen up) and only when a map is available.
            if (minecraft.screen == null && !MapState.isEmpty()) {
              minecraft.setScreen(new MapScreen());
            }
          }
        });
  }

  /** US3: bind [J] (default, MISC category) to collapse/expand the quest HUD panel. */
  private static void registerQuestHudToggle() {
    KeyMapping toggle =
        KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                "key.easy_npc.quest_hud_toggle",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                KeyMapping.Category.MISC));
    ClientTickEvents.END_CLIENT_TICK.register(
        client -> {
          while (toggle.consumeClick()) {
            QuestHudState.toggleCollapsed();
          }
        });
  }
}
