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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON dialog protocol between the AI model and the server. The model is expected to reply with a
 * JSON object: {"say": string, "options": [{"id", "label"}], "question_asked": bool,
 * "answer_verdict": "correct"|"wrong"|null, "stage_complete_claim": bool}.
 */
public final class AIProtocol {

  private AIProtocol() {}

  public record Reply(
      String say,
      List<Option> options,
      boolean questionAsked,
      String answerVerdict,
      boolean stageCompleteClaim,
      String command,
      boolean parsed) {}

  public record Option(String id, String label) {}

  public static Reply parse(String raw) {
    if (raw == null) {
      return new Reply("", List.of(), false, null, false, null, false);
    }
    String s = raw.strip();
    if (s.startsWith("```")) {
      int nl = s.indexOf('\n');
      int end = s.lastIndexOf("```");
      if (nl >= 0 && end > nl) {
        s = s.substring(nl + 1, end).strip();
      }
    }
    try {
      JsonObject o = JsonParser.parseString(s).getAsJsonObject();
      List<Option> opts = new ArrayList<>();
      if (o.has("options") && o.get("options").isJsonArray()) {
        for (JsonElement e : o.getAsJsonArray("options")) {
          JsonObject oo = e.getAsJsonObject();
          opts.add(new Option(optString(oo, "id"), optString(oo, "label")));
        }
      }
      return new Reply(
          optString(o, "say"),
          List.copyOf(opts),
          optBool(o, "question_asked"),
          optString(o, "answer_verdict"),
          optBool(o, "stage_complete_claim"),
          optString(o, "command"),
          true);
    } catch (Exception e) {
      return new Reply(raw, List.of(), false, null, false, null, false);
    }
  }

  private static String optString(JsonObject o, String key) {
    if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
      return null;
    }
    JsonElement e = o.get(key);
    return e.isJsonPrimitive() ? e.getAsString() : null;
  }

  private static boolean optBool(JsonObject o, String key) {
    if (o == null || !o.has(key) || o.get(key).isJsonNull()) {
      return false;
    }
    JsonElement e = o.get(key);
    return e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean() && e.getAsBoolean();
  }
}
