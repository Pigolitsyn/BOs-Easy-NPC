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
import de.markusbordihn.easynpc.quest.QuestHudTransitions;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Client-side store for the currently active quest HUD. Populated by {@code
 * QuestHudDefinitionMessage.handleClient} and emptied by {@code QuestHudClearMessage.handleClient}.
 * The {@code done} flags come straight from the packet — the client does not read the server
 * scoreboard (spike hud-0).
 *
 * <p>On each {@link #set(QuestHudData)} the previous per-stage done flags are diffed against the new
 * ones (pure logic in {@link QuestHudTransitions}) to raise one-shot UI events: a queue of
 * stage-completion toasts (US2) and a quest-completion banner (US4). The Fabric overlay drains these
 * for rendering and sound. {@link #collapsed} backs the [J] toggle (US3). MC-free so unit-testable.
 */
public final class QuestHudState {

  private static volatile QuestHudData current;

  /** Done flags from the previous update, for transition detection. */
  private static volatile boolean[] previousDone;

  /** Pending stage-completion toasts (US2): newly-done stage titles, FIFO. */
  private static final Deque<String> toastQueue = new ArrayDeque<>();

  /** Set true once on the update that completes the quest (US4); consumed by the overlay. */
  private static volatile boolean finaleEvent;

  /** Collapsed view toggle (US3): show only the current objective line. */
  private static volatile boolean collapsed;

  private QuestHudState() {}

  /**
   * Set the active quest definition (with live done flags) and raise toast/finale events for any
   * stages that became done relative to the previous update.
   */
  public static synchronized void set(final QuestHudData data) {
    current = data;
    boolean[] cur = doneFlags(data);
    int[] newly = QuestHudTransitions.newlyDone(previousDone, cur);
    List<StageInfo> stages = data == null ? List.of() : data.stages();
    for (int index : newly) {
      if (index >= 0 && index < stages.size()) {
        toastQueue.addLast(stages.get(index).title());
      }
    }
    if (QuestHudTransitions.justCompleted(previousDone, cur)) {
      finaleEvent = true;
    }
    previousDone = cur;
  }

  /** Clear the active quest definition (hide the HUD) and reset transition tracking + events. */
  public static synchronized void clear() {
    current = null;
    previousDone = null;
    toastQueue.clear();
    finaleEvent = false;
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

  // ---- US2: stage-completion toast events ----

  /** Pop the next pending stage-completion toast title, or {@code null} when none. */
  public static synchronized String pollToast() {
    return toastQueue.pollFirst();
  }

  // ---- US4: quest-completion finale event ----

  /** Consume the one-shot finale event (true exactly once after the quest completes). */
  public static synchronized boolean consumeFinaleEvent() {
    boolean value = finaleEvent;
    finaleEvent = false;
    return value;
  }

  // ---- US3: collapse toggle ----

  /** Whether the panel is collapsed to a single line. */
  public static boolean isCollapsed() {
    return collapsed;
  }

  /** Flip the collapsed state (bound to [J]). */
  public static void toggleCollapsed() {
    collapsed = !collapsed;
  }

  /** Extract per-stage done flags from a (possibly null) definition. */
  private static boolean[] doneFlags(final QuestHudData data) {
    if (data == null) {
      return new boolean[0];
    }
    List<StageInfo> stages = data.stages();
    boolean[] flags = new boolean[stages.size()];
    for (int i = 0; i < stages.size(); i++) {
      flags[i] = stages.get(i).done();
    }
    return flags;
  }
}
