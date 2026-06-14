/*
 * Copyright 2026 Markus Bordihn
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

package de.markusbordihn.easynpc.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ParkurCommandWhitelistTest {

  // --- allowed shapes ---

  @Test
  void allowsWorldborderCenter() {
    assertTrue(ParkurCommandWhitelist.isAllowed("worldborder center 1000000 66"));
    assertTrue(ParkurCommandWhitelist.isAllowed("worldborder center -1000000 -66"));
  }

  @Test
  void allowsWorldborderSet() {
    assertTrue(ParkurCommandWhitelist.isAllowed("worldborder set 64"));
  }

  @Test
  void allowsTpSelf() {
    assertTrue(ParkurCommandWhitelist.isAllowed("tp @s 1000000 100 66"));
    assertTrue(ParkurCommandWhitelist.isAllowed("tp @s -1000000 100 -66"));
  }

  @Test
  void allowsScoreboardSetOnQuestObjective() {
    assertTrue(ParkurCommandWhitelist.isAllowed("scoreboard players set @s q1_s1 1"));
    assertTrue(ParkurCommandWhitelist.isAllowed("scoreboard players set @p q1_s1p 0"));
  }

  @Test
  void allowsLeadingSlashAndExtraWhitespace() {
    assertTrue(ParkurCommandWhitelist.isAllowed("/worldborder set 64"));
    assertTrue(ParkurCommandWhitelist.isAllowed("  tp @s 0 100 0  "));
  }

  // --- rejected: dangerous roots ---

  @Test
  void rejectsOp() {
    assertFalse(ParkurCommandWhitelist.isAllowed("op @s"));
    assertFalse(ParkurCommandWhitelist.isAllowed("op Steve"));
  }

  @Test
  void rejectsGive() {
    assertFalse(ParkurCommandWhitelist.isAllowed("give @s minecraft:diamond 64"));
  }

  @Test
  void rejectsKill() {
    assertFalse(ParkurCommandWhitelist.isAllowed("kill @a"));
  }

  @Test
  void rejectsFill() {
    assertFalse(ParkurCommandWhitelist.isAllowed("fill 0 0 0 10 10 10 minecraft:tnt"));
  }

  @Test
  void rejectsExecuteWrapper() {
    // execute can smuggle anything (run <cmd>) -> never allowed.
    assertFalse(ParkurCommandWhitelist.isAllowed("execute as @a run kill @a"));
    assertFalse(ParkurCommandWhitelist.isAllowed("execute run worldborder set 64"));
  }

  // --- rejected: right root, wrong/forbidden subcommand or shape ---

  @Test
  void rejectsTpToOtherPlayer() {
    assertFalse(ParkurCommandWhitelist.isAllowed("tp @a 0 0 0"));
    assertFalse(ParkurCommandWhitelist.isAllowed("tp Steve 0 0 0"));
    assertFalse(ParkurCommandWhitelist.isAllowed("tp @s @p"));
  }

  @Test
  void rejectsWorldborderDamageOrOtherSub() {
    assertFalse(ParkurCommandWhitelist.isAllowed("worldborder damage amount 100"));
    assertFalse(ParkurCommandWhitelist.isAllowed("worldborder add 5000"));
  }

  @Test
  void rejectsScoreboardNonSetOperation() {
    assertFalse(ParkurCommandWhitelist.isAllowed("scoreboard objectives remove q1_s1"));
    assertFalse(ParkurCommandWhitelist.isAllowed("scoreboard players add @s q1_s1 5"));
    assertFalse(ParkurCommandWhitelist.isAllowed("scoreboard players set @a q1_s1 1"));
  }

  @Test
  void rejectsNonNumericCoordinates() {
    assertFalse(ParkurCommandWhitelist.isAllowed("tp @s ~ ~10 ~"));
    assertFalse(ParkurCommandWhitelist.isAllowed("tp @s 1000000 100"));
    assertFalse(ParkurCommandWhitelist.isAllowed("worldborder set abc"));
  }

  @Test
  void rejectsTrailingChainedCommand() {
    // no command separators / smuggling.
    assertFalse(ParkurCommandWhitelist.isAllowed("worldborder set 64; op @s"));
    assertFalse(ParkurCommandWhitelist.isAllowed("tp @s 0 100 0\nkill @a"));
  }

  @Test
  void rejectsNullAndBlank() {
    assertFalse(ParkurCommandWhitelist.isAllowed(null));
    assertFalse(ParkurCommandWhitelist.isAllowed(""));
    assertFalse(ParkurCommandWhitelist.isAllowed("   "));
  }
}
