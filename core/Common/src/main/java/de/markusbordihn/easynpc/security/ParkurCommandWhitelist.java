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

import java.util.regex.Pattern;

/**
 * Allow-list for commands carried in an NPC dialog reply's {@code command} field.
 *
 * <p>The dialog reply is produced by an UNTRUSTED LLM. This class is the security boundary for the
 * parkur_true_false mechanic: only the four parameterized command shapes required by the mechanic
 * (worldborder center/set, tp @s into coordinates, scoreboard players set on a single objective) are
 * permitted. Everything else — op, give, kill, fill, execute wrappers, chained/smuggled commands,
 * malformed coordinates — returns {@code false} and the caller MUST NOT run it.
 *
 * <p>Patterns are fully anchored ({@code ^...$}) so no suffix can be smuggled, and chaining
 * characters ({@code ; \n \r}) are rejected before pattern matching.
 */
public final class ParkurCommandWhitelist {

  // Signed integer, e.g. 100, -1000000. No relative (~ ^) or decimal coords.
  private static final String INT = "-?\\d+";

  // worldborder center <x> <z>
  private static final Pattern WORLDBORDER_CENTER =
      Pattern.compile("^worldborder center " + INT + " " + INT + "$");

  // worldborder set <n>   (n must be a positive integer size)
  private static final Pattern WORLDBORDER_SET = Pattern.compile("^worldborder set \\d+$");

  // tp @s <x> <y> <z>     (only the issuing player as target; absolute integer coords)
  private static final Pattern TP_SELF =
      Pattern.compile("^tp @s " + INT + " " + INT + " " + INT + "$");

  // scoreboard players set @p|@s <objective> <value>
  // objective name: vanilla allows [A-Za-z0-9_.+-]; value is a signed integer.
  private static final Pattern SCOREBOARD_SET =
      Pattern.compile("^scoreboard players set @[ps] [A-Za-z0-9_.+\\-]+ " + INT + "$");

  private ParkurCommandWhitelist() {}

  /**
   * @return {@code true} only if {@code rawCommand} matches one of the four approved shapes.
   */
  public static boolean isAllowed(String rawCommand) {
    if (rawCommand == null) {
      return false;
    }
    String command = rawCommand.strip();
    if (command.startsWith("/")) {
      command = command.substring(1).strip();
    }
    if (command.isEmpty()) {
      return false;
    }
    // Reject any command-chaining / multiline smuggling outright.
    if (command.indexOf(';') >= 0
        || command.indexOf('\n') >= 0
        || command.indexOf('\r') >= 0) {
      return false;
    }
    return WORLDBORDER_CENTER.matcher(command).matches()
        || WORLDBORDER_SET.matcher(command).matches()
        || TP_SELF.matcher(command).matches()
        || SCOREBOARD_SET.matcher(command).matches();
  }
}
