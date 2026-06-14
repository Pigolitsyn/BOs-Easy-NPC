/*
 * MOD-6: client-rendered map screen with pins (Картограф / Map-It).
 *
 * Opened by the [M] keybind (EasyNPCClient) when MapState has data for the current dimension; ESC or
 * [M] closes. Extends vanilla Screen directly (no container menu). Reads the active MapData from
 * MapState (pushed by MapDefinitionMessage). Pins + the live player marker are projected from world
 * coords into a centered square map area by MapProjection (pure, unit-tested). North-up, no
 * rotation. Markers are drawn with fill/glyph + drawString — no texture assets required for v1.
 * Self-closes when MapState empties (e.g. a MapClearMessage arrives while open).
 */

package de.markusbordihn.easynpc.client.map;

import de.markusbordihn.easynpc.quest.MapData;
import de.markusbordihn.easynpc.quest.MapData.Pin;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class MapScreen extends Screen {

  // Layout.
  private static final int MARGIN = 24;
  private static final int PADDING = 6;
  private static final int HEADER_H = 14;
  private static final int FOOTER_H = 12;
  private static final int GRID_STEP = 32; // px between faint grid lines

  // Colors (ARGB), mirrored from QuestHudOverlay.
  private static final int COLOR_DIM = 0xC0000000; // backdrop dim
  private static final int COLOR_BG = 0xE61A1422; // dark translucent panel
  private static final int COLOR_BORDER = 0xFFD4AF37; // gold frame
  private static final int COLOR_TITLE = 0xFFFFD700; // gold
  private static final int COLOR_GRID = 0x22FFFFFF; // faint grid
  private static final int COLOR_FOOTER = 0xFFAAAAAA; // grey hint

  // Pin marker colors by kind.
  private static final int PIN_OBJECTIVE = 0xFFFFD700; // gold
  private static final int PIN_NPC = 0xFF55FFFF; // cyan
  private static final int PIN_START = 0xFF55FF55; // green
  private static final int PIN_DEFAULT = 0xFFAAAAAA; // grey
  private static final int PIN_LABEL = 0xFFFFFFFF; // white label
  private static final int PIN_HALF = 4; // marker half-size (px)

  // Player marker (drawn on top).
  private static final int PLAYER_COLOR = 0xFFFF4040; // red
  private static final int PLAYER_HALF = 3;

  // Computed in init(): centered square map area.
  private int areaLeft;
  private int areaTop;
  private int areaSize;

  public MapScreen() {
    super(Component.literal("LoreCraft Map"));
  }

  @Override
  protected void init() {
    int avail = Math.min(this.width, this.height) - 2 * MARGIN - HEADER_H - FOOTER_H;
    this.areaSize = Math.max(64, avail);
    this.areaLeft = (this.width - this.areaSize) / 2;
    this.areaTop = (this.height - this.areaSize) / 2;
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  @Override
  public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    // Self-close if the map went away (clear message / dimension change while open).
    MapData data = MapState.get();
    if (data == null) {
      this.onClose();
      return;
    }

    // Backdrop dim.
    guiGraphics.fill(0, 0, this.width, this.height, COLOR_DIM);

    Font font = this.font;

    // Panel = header + map area + footer, framed.
    int panelLeft = this.areaLeft - PADDING;
    int panelTop = this.areaTop - HEADER_H - PADDING;
    int panelRight = this.areaLeft + this.areaSize + PADDING;
    int panelBottom = this.areaTop + this.areaSize + FOOTER_H + PADDING;
    drawFramedBox(guiGraphics, panelLeft, panelTop, panelRight, panelBottom, COLOR_BG, COLOR_BORDER);

    // Header (title, centered).
    String title = data.title().isEmpty() ? "Карта" : data.title();
    guiGraphics.drawString(
        font, title, this.areaLeft + (this.areaSize - font.width(title)) / 2,
        panelTop + PADDING, COLOR_TITLE, true);

    // Map area frame + faint grid.
    drawFramedBox(
        guiGraphics, this.areaLeft, this.areaTop,
        this.areaLeft + this.areaSize, this.areaTop + this.areaSize, 0x40000000, COLOR_BORDER);
    drawGrid(guiGraphics);

    MapProjection projection =
        new MapProjection(
            data.originX(), data.originZ(), data.spanX(), data.spanZ(),
            this.areaLeft, this.areaTop, this.areaSize, this.areaSize);

    // Pins.
    for (Pin pin : data.pins()) {
      renderPin(guiGraphics, font, projection, pin);
    }

    // Player marker (on top).
    renderPlayer(guiGraphics, projection);

    // Footer hint (centered).
    String hint = "[M] / ESC — закрыть";
    guiGraphics.drawString(
        font, hint, this.areaLeft + (this.areaSize - font.width(hint)) / 2,
        this.areaTop + this.areaSize + PADDING, COLOR_FOOTER, true);
  }

  private void drawGrid(GuiGraphics guiGraphics) {
    int right = this.areaLeft + this.areaSize;
    int bottom = this.areaTop + this.areaSize;
    for (int x = this.areaLeft + GRID_STEP; x < right; x += GRID_STEP) {
      guiGraphics.fill(x, this.areaTop, x + 1, bottom, COLOR_GRID);
    }
    for (int y = this.areaTop + GRID_STEP; y < bottom; y += GRID_STEP) {
      guiGraphics.fill(this.areaLeft, y, right, y + 1, COLOR_GRID);
    }
  }

  private static void renderPin(
      GuiGraphics guiGraphics, Font font, MapProjection projection, Pin pin) {
    boolean inBounds = projection.inBounds(pin.x(), pin.z());
    int sx = (int) Math.round(projection.clampX(projection.screenX(pin.x())));
    int sy = (int) Math.round(projection.clampY(projection.screenY(pin.z())));
    int color = colorFor(pin.kind());

    // Filled square for in-bounds pins; hollow outline for edge-clamped (off-map) pins.
    if (inBounds) {
      guiGraphics.fill(sx - PIN_HALF, sy - PIN_HALF, sx + PIN_HALF, sy + PIN_HALF, color);
    } else {
      drawHollow(guiGraphics, sx - PIN_HALF, sy - PIN_HALF, sx + PIN_HALF, sy + PIN_HALF, color);
    }

    // Label to the right of the marker (only for in-bounds pins to limit clutter on edges).
    if (inBounds && !pin.label().isEmpty()) {
      guiGraphics.drawString(font, pin.label(), sx + PIN_HALF + 2, sy - 4, PIN_LABEL, true);
    }
  }

  private void renderPlayer(GuiGraphics guiGraphics, MapProjection projection) {
    var player = net.minecraft.client.Minecraft.getInstance().player;
    if (player == null) {
      return;
    }
    int sx = (int) Math.round(projection.clampX(projection.screenX(player.getX())));
    int sy = (int) Math.round(projection.clampY(projection.screenY(player.getZ())));
    guiGraphics.fill(
        sx - PLAYER_HALF, sy - PLAYER_HALF, sx + PLAYER_HALF, sy + PLAYER_HALF, PLAYER_COLOR);
    // White center dot to distinguish from pins.
    guiGraphics.fill(sx - 1, sy - 1, sx + 1, sy + 1, 0xFFFFFFFF);
  }

  // ---- shared drawing (mirrors QuestHudOverlay) ----

  private static void drawFramedBox(
      GuiGraphics guiGraphics, int left, int top, int right, int bottom, int bg, int border) {
    guiGraphics.fill(left, top, right, bottom, bg);
    guiGraphics.fill(left, top, right, top + 1, border); // top
    guiGraphics.fill(left, bottom - 1, right, bottom, border); // bottom
    guiGraphics.fill(left, top, left + 1, bottom, border); // left
    guiGraphics.fill(right - 1, top, right, bottom, border); // right
  }

  private static void drawHollow(
      GuiGraphics guiGraphics, int left, int top, int right, int bottom, int color) {
    guiGraphics.fill(left, top, right, top + 1, color);
    guiGraphics.fill(left, bottom - 1, right, bottom, color);
    guiGraphics.fill(left, top, left + 1, bottom, color);
    guiGraphics.fill(right - 1, top, right, bottom, color);
  }

  private static int colorFor(String kind) {
    return switch (kind) {
      case "objective" -> PIN_OBJECTIVE;
      case "npc" -> PIN_NPC;
      case "start" -> PIN_START;
      default -> PIN_DEFAULT;
    };
  }

  @Override
  public boolean keyPressed(KeyEvent keyEvent) {
    int keyCode = keyEvent.input();
    if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_M) {
      this.onClose();
      return true;
    }
    return super.keyPressed(keyEvent);
  }
}
