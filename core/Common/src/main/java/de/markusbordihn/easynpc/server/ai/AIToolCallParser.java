/*
 * Copyright 2025 Markus Bordihn
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

package de.markusbordihn.easynpc.server.ai;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses inline tool calls out of an AI reply. Currently supports:
 *
 * <pre>[adjust_relation:+5]   [adjust_relation:-10]</pre>
 *
 * Returns the visible reply (tool tags stripped) and the aggregated relation delta.
 */
public final class AIToolCallParser {

  private static final Pattern ADJUST_RELATION =
      Pattern.compile("\\[adjust_relation:([+\\-]?\\d{1,3})]");

  private AIToolCallParser() {}

  public static Result parse(String input) {
    if (input == null || input.isEmpty()) {
      return new Result("", 0);
    }
    Matcher m = ADJUST_RELATION.matcher(input);
    int total = 0;
    StringBuilder sb = new StringBuilder();
    while (m.find()) {
      try {
        total += Integer.parseInt(m.group(1));
      } catch (NumberFormatException ignored) {
        // skip
      }
      m.appendReplacement(sb, "");
    }
    m.appendTail(sb);
    return new Result(sb.toString().replaceAll("\\s+", " ").trim(), total);
  }

  public record Result(String cleanedText, int relationDelta) {}
}
