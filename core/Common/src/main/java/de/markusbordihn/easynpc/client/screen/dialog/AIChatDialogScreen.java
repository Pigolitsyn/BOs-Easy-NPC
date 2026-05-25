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

package de.markusbordihn.easynpc.client.screen.dialog;

import de.markusbordihn.easynpc.Constants;
import de.markusbordihn.easynpc.client.renderer.screen.EntityScreenRenderer;
import de.markusbordihn.easynpc.client.screen.Screen;
import de.markusbordihn.easynpc.client.screen.components.Graphics;
import de.markusbordihn.easynpc.client.screen.components.Text;
import de.markusbordihn.easynpc.compat.IntegrationRegistry;
import de.markusbordihn.easynpc.data.render.EntityRenderConfig;
import de.markusbordihn.easynpc.data.screen.AdditionalScreenData;
import de.markusbordihn.easynpc.menu.dialog.AIChatDialogMenu;
import de.markusbordihn.easynpc.network.NetworkMessageHandlerManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

public class AIChatDialogScreen<T extends AIChatDialogMenu>
    extends Screen<T, AdditionalScreenData> {

  // Bubble text area: matches DialogScreen text column (leftPos+87)
  private static final int BUBBLE_TEXT_X_OFFSET = 89;
  private static final int BUBBLE_TEXT_WRAP_WIDTH = 170;
  private static final int BUBBLE_AREA_TOP = 26;
  private static final int BUBBLE_AREA_BOTTOM = 128;

  private static final int COLOR_NPC = 0xFF222222;
  private static final int COLOR_PLAYER = 0xFF1F6E1F;
  private static final int COLOR_WAITING = 0xFF666666;

  // Input area at very bottom, full width
  private static final int SEPARATOR_Y = 170;
  private static final int INPUT_X_OFFSET = 5;
  private static final int INPUT_Y = 177;
  private static final int INPUT_HEIGHT = 18;
  private static final int SEND_BUTTON_WIDTH = 44;

  private static final int LINE_HEIGHT = 12;

  private final List<ChatEntry> chatHistory = new ArrayList<>();
  private final List<RenderedLine> cachedLines = new ArrayList<>();
  private EditBox inputBox;
  private boolean waitingForResponse = false;
  private int scrollOffset = 0;

  public AIChatDialogScreen(T menu, Inventory inventory, Component component) {
    super(menu, inventory, component, 280, 200);
  }

  @Override
  public void init() {
    super.init();

    this.titleLabelX = 10;
    this.titleLabelY = 8;

    if (this.closeButton != null) {
      this.closeButton.setX(this.leftPos + this.imageWidth - 13);
      this.closeButton.setY(this.topPos + 4);
    }

    int inputWidth = this.imageWidth - INPUT_X_OFFSET - SEND_BUTTON_WIDTH - 6;
    this.inputBox =
        new EditBox(
            this.font,
            this.leftPos + INPUT_X_OFFSET,
            this.topPos + INPUT_Y,
            inputWidth,
            INPUT_HEIGHT,
            Component.empty());
    this.inputBox.setMaxLength(512);
    this.inputBox.setHint(Component.literal("> Say something..."));
    this.inputBox.setFocused(true);
    this.addRenderableWidget(this.inputBox);

    Button sendButton =
        Button.builder(
                Component.translatable("easynpc.dialog.ai.send"),
                btn -> {
                  if (!waitingForResponse) sendMessage();
                })
            .pos(this.leftPos + INPUT_X_OFFSET + inputWidth + 2, this.topPos + INPUT_Y - 1)
            .size(SEND_BUTTON_WIDTH, INPUT_HEIGHT + 2)
            .build();
    this.addRenderableWidget(sendButton);

    rebuildCachedLines();
    NetworkMessageHandlerManager.getServerHandler().requestAIHistory(this.getEasyNPCUUID());
  }

  public void loadHistory(List<String> roles, List<String> contents) {
    this.chatHistory.clear();
    int n = Math.min(roles.size(), contents.size());
    for (int i = 0; i < n; i++) {
      boolean isUser = "user".equalsIgnoreCase(roles.get(i));
      this.chatHistory.add(new ChatEntry(isUser, contents.get(i)));
    }
    this.scrollOffset = 0;
    this.waitingForResponse = false;
    rebuildCachedLines();
  }

  @Override
  protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
    Graphics.blit(
        guiGraphics,
        Constants.TEXTURE_DIALOG_SCENE_LARGE,
        this.leftPos,
        this.topPos,
        1,
        1,
        285,
        210,
        512,
        256);
  }

  @Override
  public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    if (this.getEasyNPC() == null) {
      return;
    }

    super.render(guiGraphics, mouseX, mouseY, partialTicks);

    // NPC avatar on left, same positioning as DialogScreen
    int entityTop = this.getEasyNPC().getEasyNPCDialogData().getEntityDialogTop();
    int entityLeft = this.getEasyNPC().getEasyNPCDialogData().getEntityDialogLeft();
    int scale = this.getEasyNPC().getEasyNPCDialogData().getEntityDialogScaling();

    IntegrationRegistry.setGuiPreviewMode(true);
    EntityScreenRenderer.renderEntityRaw(
        guiGraphics,
        this.getEasyNPC(),
        EntityRenderConfig.dialog(
            this.leftPos + 40 + entityLeft, this.topPos + 80 + entityTop, scale),
        this.xMouse,
        this.yMouse);
    IntegrationRegistry.setGuiPreviewMode(false);

    // Separator line between chat and input (full width)
    guiGraphics.fill(
        this.leftPos + 3,
        this.topPos + SEPARATOR_Y,
        this.leftPos + this.imageWidth - 3,
        this.topPos + SEPARATOR_Y + 1,
        0xFF887766);

    int maxLines = (BUBBLE_AREA_BOTTOM - BUBBLE_AREA_TOP) / LINE_HEIGHT;
    renderChatMessages(guiGraphics, this.topPos + BUBBLE_AREA_TOP, this.topPos + BUBBLE_AREA_BOTTOM, maxLines);
  }

  @Override
  protected void renderLabels(GuiGraphics guiGraphics, int x, int y) {
    Text.drawString(guiGraphics, this.font, this.title, this.leftPos + this.titleLabelX, this.topPos + this.titleLabelY);
  }

  private void rebuildCachedLines() {
    cachedLines.clear();
    for (ChatEntry entry : chatHistory) {
      String prefix = entry.isUser() ? "> " : "";
      int color = entry.isUser() ? COLOR_PLAYER : COLOR_NPC;
      for (String line : wrapText(prefix + sanitize(entry.content()), BUBBLE_TEXT_WRAP_WIDTH)) {
        cachedLines.add(new RenderedLine(line, color));
      }
    }
  }

  private void renderChatMessages(GuiGraphics guiGraphics, int areaTop, int areaBottom, int maxLines) {
    List<RenderedLine> allLines = new ArrayList<>(cachedLines);
    if (waitingForResponse) {
      allLines.add(new RenderedLine("...", COLOR_WAITING));
    }

    int totalLines = allLines.size();
    int clampedOffset = Math.max(0, Math.min(scrollOffset, Math.max(0, totalLines - maxLines)));
    int startIndex = Math.max(0, totalLines - maxLines - clampedOffset);
    int y = areaTop;
    for (int i = startIndex; i < allLines.size() && y + LINE_HEIGHT <= areaBottom; i++) {
      RenderedLine rl = allLines.get(i);
      guiGraphics.drawString(this.font, rl.text(), this.leftPos + BUBBLE_TEXT_X_OFFSET, y, rl.color(), false);
      y += LINE_HEIGHT;
    }
  }

  @Override
  public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
    int delta = (int) Math.signum(scrollY);
    scrollOffset = Math.max(0, scrollOffset + delta);
    return true;
  }

  public void receiveMessage(String role, String content) {
    this.waitingForResponse = false;
    if (content != null && !content.isEmpty()) {
      this.chatHistory.add(new ChatEntry(false, content));
      this.scrollOffset = 0;
      rebuildCachedLines();
    }
  }

  private void sendMessage() {
    String text = this.inputBox.getValue().trim();
    if (text.isEmpty()) {
      return;
    }
    this.chatHistory.add(new ChatEntry(true, text));
    this.waitingForResponse = true;
    this.scrollOffset = 0;
    this.inputBox.setValue("");
    rebuildCachedLines();
    NetworkMessageHandlerManager.getServerHandler().sendAIMessage(this.getEasyNPCUUID(), text);
  }

  @Override
  public boolean keyPressed(KeyEvent keyEvent) {
    int keyCode = keyEvent.input();
    if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
      if (!waitingForResponse) {
        sendMessage();
      }
      return true;
    }
    if (this.inputBox != null && this.inputBox.isFocused()) {
      return this.inputBox.keyPressed(keyEvent);
    }
    return super.keyPressed(keyEvent);
  }

  private static String sanitize(String text) {
    if (text == null) return "";
    return text.replaceAll("§.", "");
  }

  private List<String> wrapText(String text, int maxWidth) {
    List<String> lines = new ArrayList<>();
    if (text == null || text.isEmpty()) {
      return lines;
    }
    for (String paragraph : text.split("\n", -1)) {
      if (paragraph.isEmpty()) {
        lines.add("");
        continue;
      }
      String remaining = paragraph;
      while (!remaining.isEmpty()) {
        String sub = this.font.plainSubstrByWidth(remaining, maxWidth);
        if (sub.isEmpty()) {
          sub = remaining.substring(0, 1);
        }
        lines.add(sub);
        remaining = remaining.substring(sub.length());
      }
    }
    return lines;
  }

  private record ChatEntry(boolean isUser, String content) {}

  private record RenderedLine(String text, int color) {}
}
