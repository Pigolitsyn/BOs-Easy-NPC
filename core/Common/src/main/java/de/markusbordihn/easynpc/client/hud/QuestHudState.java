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

/**
 * Client-side store for the currently active quest HUD. Populated by {@code
 * QuestHudDefinitionMessage.handleClient} and emptied by {@code QuestHudClearMessage.handleClient}.
 * The {@code done} flags come straight from the packet — the client does not read the server
 * scoreboard (spike hud-0). Pure status/percent logic lives in {@link
 * de.markusbordihn.easynpc.quest.QuestHudStatus} so it can be unit-tested without Minecraft.
 */
public final class QuestHudState {

  private static volatile QuestHudData current;

  private QuestHudState() {}

  /** Set the active quest definition (with live done flags). */
  public static void set(final QuestHudData data) {
    current = data;
  }

  /** Clear the active quest definition (hide the HUD). */
  public static void clear() {
    current = null;
  }

  /** Active quest definition, or {@code null} when nothing should be shown (US7). */
  public static QuestHudData get() {
    return current;
  }

  /** True when there is nothing to render. */
  public static boolean isEmpty() {
    QuestHudData data = current;
    return data == null || data.stages().isEmpty();
  }
}
