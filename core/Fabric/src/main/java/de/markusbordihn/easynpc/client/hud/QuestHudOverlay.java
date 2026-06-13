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
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

/**
 * Top-left quest stage panel (US1) plus stage toasts (US2), completion banner (US4) and collapse
 * toggle (US3). Reads the active {@link QuestHudData} from {@link QuestHudState} — per-stage {@code
 * done} flags come from the server packet, never the client scoreboard (spike hud-0). Toast/finale
 * events are raised in {@link QuestHudState#set} and drained here once for render + sound. Styled
 * after {@code AIChatDialogScreen}: dark translucent fill + gold border.
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

  // US2 stage toast: top-center transient banner.
  private static final long TOAST_MS = 3000L;
  private static final int TOAST_BG = 0xCC1A1422;
  private static final int TOAST_BORDER = 0xFF55FF55; // green frame for "stage done"
  private static final int TOAST_TEXT = 0xFF55FF55;

  // US4 completion banner: center-screen victory.
  private static final long FINALE_MS = 5000L;
  private static final int FINALE_BG = 0xDD1A1422;
  private static final int FINALE_BORDER = 0xFFFFD700; // gold frame
  private static final int FINALE_TEXT = 0xFFFFD700;
  private static final String FINALE_LABEL = "★ Квест пройден ★";

  // Active toast (US2): title + the client time (ms) when it should disappear.
  private String toastTitle;
  private long toastUntilMs;

  // Active finale (US4): client time (ms) when the banner should disappear (0 = inactive).
  private long finaleUntilMs;

  @Override
  public void render(final GuiGraphics guiGraphics, final DeltaTracker deltaTracker) {
    long now = nowMs();

    // Drain one-shot events raised on the network thread; play sound exactly once on pickup.
    drainEvents(now);

    // Transient overlays draw regardless of whether a panel is shown (e.g. finale at 100%).
    Font font = Minecraft.getInstance().font;
    renderToast(guiGraphics, font, now);
    renderFinale(guiGraphics, font, now);

    // US7: nothing active -> draw no panel.
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

    if (QuestHudState.isCollapsed()) {
      renderCollapsed(guiGraphics, font, stages, statuses, pct);
    } else {
      renderPanel(guiGraphics, font, data, stages, statuses, pct);
    }

    // Task 4: directional locator to the CURRENT objective (top-center).
    renderLocator(guiGraphics, font, stages, statuses);
  }

  /** Full stage list panel (US1, expanded view). */
  private static void renderPanel(
      final GuiGraphics guiGraphics,
      final Font font,
      final QuestHudData data,
      final List<StageInfo> stages,
      final Status[] statuses,
      final int pct) {
    String header = data.title() + "  " + pct + "%";

    int contentWidth = font.width(header);
    for (StageInfo stage : stages) {
      contentWidth =
          Math.max(contentWidth, font.width(iconFor(Status.CURRENT) + " " + stage.title()));
    }
    int panelWidth = Math.max(MIN_WIDTH, contentWidth + 2 * PADDING);
    int rows = 1 + stages.size();
    int panelHeight = 2 * PADDING + rows * LINE_HEIGHT;

    int left = MARGIN_X;
    int top = MARGIN_Y;
    drawFramedBox(guiGraphics, left, top, left + panelWidth, top + panelHeight);

    int textX = left + PADDING;
    int textY = top + PADDING;

    guiGraphics.drawString(font, header, textX, textY, COLOR_TITLE, true);
    textY += LINE_HEIGHT;

    for (int i = 0; i < stages.size(); i++) {
      Status status = statuses[i];
      String line = iconFor(status) + " " + stages.get(i).title();
      guiGraphics.drawString(font, line, textX, textY, colorFor(status), true);
      textY += LINE_HEIGHT;
    }
  }

  /** US3: single-line collapsed view — current objective + percent only. */
  private static void renderCollapsed(
      final GuiGraphics guiGraphics,
      final Font font,
      final List<StageInfo> stages,
      final Status[] statuses,
      final int pct) {
    StageInfo current = firstCurrent(stages, statuses);
    String line;
    int color;
    if (current != null) {
      line = ICON_CURRENT + " " + current.title() + "  " + pct + "%";
      color = COLOR_CURRENT;
    } else {
      // Everything done -> show completion in the collapsed line.
      line = ICON_DONE + "  " + pct + "%";
      color = COLOR_DONE;
    }

    int panelWidth = Math.max(MIN_WIDTH, font.width(line) + 2 * PADDING);
    int panelHeight = 2 * PADDING + LINE_HEIGHT;
    int left = MARGIN_X;
    int top = MARGIN_Y;
    drawFramedBox(guiGraphics, left, top, left + panelWidth, top + panelHeight);
    guiGraphics.drawString(font, line, left + PADDING, top + PADDING, color, true);
  }

  // ---- US2 / US4: event draining + sound (once per event) ----

  private void drainEvents(final long now) {
    Minecraft minecraft = Minecraft.getInstance();

    // Only pick up a new toast once the previous one has expired (avoids overlap).
    if (now >= toastUntilMs) {
      String next = QuestHudState.pollToast();
      if (next != null) {
        toastTitle = next;
        toastUntilMs = now + TOAST_MS;
        minecraft
            .getSoundManager()
            .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
      }
    }

    if (QuestHudState.consumeFinaleEvent()) {
      finaleUntilMs = now + FINALE_MS;
      minecraft
          .getSoundManager()
          .play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0f));
    }
  }

  /** US2: top-center "✔ Этап пройден: <title>" banner for ~3 s. */
  private void renderToast(final GuiGraphics guiGraphics, final Font font, final long now) {
    if (toastTitle == null || now >= toastUntilMs) {
      return;
    }
    String label = ICON_DONE + " Этап пройден: " + toastTitle;
    int screenWidth = guiGraphics.guiWidth();
    int textWidth = font.width(label);
    int boxWidth = textWidth + 2 * PADDING;
    int left = (screenWidth - boxWidth) / 2;
    int top = 18;
    int boxHeight = LINE_HEIGHT + 2 * PADDING;
    drawFramedBox(guiGraphics, left, top, left + boxWidth, top + boxHeight, TOAST_BG, TOAST_BORDER);
    guiGraphics.drawString(font, label, left + PADDING, top + PADDING, TOAST_TEXT, true);
  }

  /** US4: center-screen "★ Квест пройден ★" victory banner for ~5 s. */
  private void renderFinale(final GuiGraphics guiGraphics, final Font font, final long now) {
    if (finaleUntilMs == 0L || now >= finaleUntilMs) {
      return;
    }
    int screenWidth = guiGraphics.guiWidth();
    int screenHeight = guiGraphics.guiHeight();
    int textWidth = font.width(FINALE_LABEL);
    int boxWidth = textWidth + 4 * PADDING;
    int boxHeight = LINE_HEIGHT + 4 * PADDING;
    int left = (screenWidth - boxWidth) / 2;
    int top = (screenHeight - boxHeight) / 2 - 20;
    drawFramedBox(
        guiGraphics, left, top, left + boxWidth, top + boxHeight, FINALE_BG, FINALE_BORDER);
    guiGraphics.drawString(
        font, FINALE_LABEL, left + 2 * PADDING, top + 2 * PADDING, FINALE_TEXT, true);
  }

  // ---- shared drawing ----

  private static void drawFramedBox(
      final GuiGraphics guiGraphics, final int left, final int top, final int right,
      final int bottom) {
    drawFramedBox(guiGraphics, left, top, right, bottom, COLOR_BG, COLOR_BORDER);
  }

  private static void drawFramedBox(
      final GuiGraphics guiGraphics, final int left, final int top, final int right,
      final int bottom, final int bg, final int border) {
    guiGraphics.fill(left, top, right, bottom, bg);
    guiGraphics.fill(left, top, right, top + 1, border); // top
    guiGraphics.fill(left, bottom - 1, right, bottom, border); // bottom
    guiGraphics.fill(left, top, left + 1, bottom, border); // left
    guiGraphics.fill(right - 1, top, right, bottom, border); // right
  }

  private static StageInfo firstCurrent(final List<StageInfo> stages, final Status[] statuses) {
    for (int i = 0; i < stages.size(); i++) {
      if (statuses[i] == Status.CURRENT) {
        return stages.get(i);
      }
    }
    return null;
  }

  /** Wall-clock millis (toast/banner timers are independent of game pause). */
  private static long nowMs() {
    return System.currentTimeMillis();
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
    StageInfo current = firstCurrent(stages, statuses);
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
