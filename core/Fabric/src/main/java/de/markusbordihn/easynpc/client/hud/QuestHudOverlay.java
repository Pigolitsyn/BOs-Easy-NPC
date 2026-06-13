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

package de.markusbordihn.easynpc.client.hud;

import de.markusbordihn.easynpc.quest.QuestHudData;
import de.markusbordihn.easynpc.quest.QuestHudData.StageInfo;
import de.markusbordihn.easynpc.quest.QuestHudStatus;
import de.markusbordihn.easynpc.quest.QuestHudStatus.Status;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Top-left quest stage panel (US1). Reads the active {@link QuestHudData} from {@link QuestHudState}
 * — its per-stage {@code done} flags come from the server packet, never the client scoreboard (spike
 * hud-0). Styled after {@code AIChatDialogScreen}: dark translucent fill + gold border.
 */
public class QuestHudOverlay implements HudElement {

  // Layout.
  private static final int MARGIN_X = 6;
  private static final int MARGIN_Y = 6;
  private static final int PADDING = 5;
  private static final int LINE_HEIGHT = 11;
  private static final int MIN_WIDTH = 120;

  // Colors (ARGB), mirrored from AIChatDialogScreen.
  private static final int COLOR_BG = 0xCC1A1422; // dark translucent panel
  private static final int COLOR_BORDER = 0xFFD4AF37; // gold frame
  private static final int COLOR_TITLE = 0xFFFFD700; // gold
  private static final int COLOR_DONE = 0xFF55FF55; // green
  private static final int COLOR_CURRENT = 0xFFFFFF55; // yellow
  private static final int COLOR_LOCKED = 0xFFAAAAAA; // grey

  private static final String ICON_DONE = "✔"; // heavy check
  private static final String ICON_CURRENT = "▶"; // right-pointing triangle
  private static final String ICON_LOCKED = "🔒"; // lock (falls back to a glyph if absent)

  // Locator (Task 4): directional arrow + distance to the CURRENT objective.
  private static final int LOCATOR_TOP = 4; // y of the arrow row
  private static final String ARROW_AHEAD = "▲";
  private static final String ARROW_RIGHT = "▶";
  private static final String ARROW_BEHIND = "▼";
  private static final String ARROW_LEFT = "◀";

  @Override
  public void render(final GuiGraphics guiGraphics, final DeltaTracker deltaTracker) {
    // US7: nothing active -> draw nothing.
    if (QuestHudState.isEmpty()) {
      return;
    }
    QuestHudData data = QuestHudState.get();
    if (data == null) {
      return;
    }
    List<StageInfo> stages = data.stages();

    // Status derived from packet done flags (NOT the client scoreboard).
    boolean[] done = new boolean[stages.size()];
    for (int i = 0; i < stages.size(); i++) {
      done[i] = stages.get(i).done();
    }
    Status[] statuses = QuestHudStatus.stageStatuses(done);
    int pct = QuestHudStatus.percent(done);

    Font font = Minecraft.getInstance().font;
    String header = data.title() + "  " + pct + "%";

    // Measure panel width.
    int contentWidth = font.width(header);
    for (StageInfo stage : stages) {
      contentWidth = Math.max(contentWidth, font.width(iconFor(Status.CURRENT) + " " + stage.title()));
    }
    int panelWidth = Math.max(MIN_WIDTH, contentWidth + 2 * PADDING);
    int rows = 1 + stages.size();
    int panelHeight = 2 * PADDING + rows * LINE_HEIGHT;

    int left = MARGIN_X;
    int top = MARGIN_Y;
    int right = left + panelWidth;
    int bottom = top + panelHeight;

    // Dark translucent background.
    guiGraphics.fill(left, top, right, bottom, COLOR_BG);

    // Gold border (1px frame).
    guiGraphics.fill(left, top, right, top + 1, COLOR_BORDER); // top
    guiGraphics.fill(left, bottom - 1, right, bottom, COLOR_BORDER); // bottom
    guiGraphics.fill(left, top, left + 1, bottom, COLOR_BORDER); // left
    guiGraphics.fill(right - 1, top, right, bottom, COLOR_BORDER); // right

    int textX = left + PADDING;
    int textY = top + PADDING;

    // Header: title + percent.
    guiGraphics.drawString(font, header, textX, textY, COLOR_TITLE, true);
    textY += LINE_HEIGHT;

    // Stage lines.
    for (int i = 0; i < stages.size(); i++) {
      Status status = statuses[i];
      String line = iconFor(status) + " " + stages.get(i).title();
      guiGraphics.drawString(font, line, textX, textY, colorFor(status), true);
      textY += LINE_HEIGHT;
    }

    // Task 4: directional locator to the CURRENT objective (top-center).
    renderLocator(guiGraphics, font, stages, statuses);
  }

  /**
   * Draw a top-center directional arrow + "&lt;title&gt; · &lt;dist&gt; m" pointing at the CURRENT
   * stage's NPC. Drawn nothing when there is no current stage (quest complete/empty) or the player
   * is unavailable. Pure direction/distance math lives in {@link Locator} (unit-tested).
   */
  private static void renderLocator(
      final GuiGraphics guiGraphics,
      final Font font,
      final List<StageInfo> stages,
      final Status[] statuses) {
    // Find the CURRENT stage (the single in-progress objective).
    StageInfo current = null;
    for (int i = 0; i < stages.size(); i++) {
      if (statuses[i] == Status.CURRENT) {
        current = stages.get(i);
        break;
      }
    }
    if (current == null) {
      return; // everything done (or empty) -> no locator.
    }

    var player = Minecraft.getInstance().player;
    if (player == null) {
      return;
    }

    double px = player.getX();
    double pz = player.getZ();
    float yaw = player.getYRot();
    double tx = current.npcX();
    double tz = current.npcZ();

    double bearing = Locator.bearing(px, pz, yaw, tx, tz);
    int dist = Locator.distance(px, pz, tx, tz);

    String arrow = arrowFor(bearing);
    String label = arrow + " " + current.title() + " · " + dist + " m";

    int screenWidth = guiGraphics.guiWidth();
    int x = (screenWidth - font.width(label)) / 2;
    guiGraphics.drawString(font, label, x, LOCATOR_TOP, COLOR_TITLE, true);
  }

  /**
   * Pick a directional glyph from the bearing (degrees, [-180,180]; 0 = ahead, + = right, - =
   * left): ahead |b|&lt;45, right 45..135, behind |b|&gt;135, left -135..-45.
   */
  private static String arrowFor(final double bearing) {
    double abs = Math.abs(bearing);
    if (abs < 45.0) {
      return ARROW_AHEAD;
    }
    if (abs > 135.0) {
      return ARROW_BEHIND;
    }
    return bearing > 0 ? ARROW_RIGHT : ARROW_LEFT;
  }

  private static String iconFor(final Status status) {
    return switch (status) {
      case DONE -> ICON_DONE;
      case CURRENT -> ICON_CURRENT;
      case LOCKED -> ICON_LOCKED;
    };
  }

  private static int colorFor(final Status status) {
    return switch (status) {
      case DONE -> COLOR_DONE;
      case CURRENT -> COLOR_CURRENT;
      case LOCKED -> COLOR_LOCKED;
    };
  }
}
