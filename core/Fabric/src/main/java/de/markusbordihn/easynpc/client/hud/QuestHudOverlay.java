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
