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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AIProtocolTest {

  @Test
  void parseValidJson() {
    String raw =
        "{\"say\": \"Hello!\", \"options\": [{\"id\": \"a\", \"label\": \"Yes\"},"
            + " {\"id\": \"b\", \"label\": \"No\"}], \"question_asked\": true,"
            + " \"answer_verdict\": \"correct\", \"stage_complete_claim\": false}";
    AIProtocol.Reply reply = AIProtocol.parse(raw);
    assertTrue(reply.parsed());
    assertEquals("Hello!", reply.say());
    assertEquals(2, reply.options().size());
    assertEquals("a", reply.options().get(0).id());
    assertEquals("Yes", reply.options().get(0).label());
    assertEquals("b", reply.options().get(1).id());
    assertTrue(reply.questionAsked());
    assertEquals("correct", reply.answerVerdict());
    assertFalse(reply.stageCompleteClaim());
  }

  @Test
  void parseJsonWithCodeFences() {
    String raw =
        "```json\n{\"say\": \"Fenced\", \"question_asked\": false,"
            + " \"stage_complete_claim\": true}\n```";
    AIProtocol.Reply reply = AIProtocol.parse(raw);
    assertTrue(reply.parsed());
    assertEquals("Fenced", reply.say());
    assertTrue(reply.options().isEmpty());
    assertFalse(reply.questionAsked());
    assertTrue(reply.stageCompleteClaim());
  }

  @Test
  void parseBrokenInputReturnsUnparsed() {
    String raw = "Just a plain text answer, no JSON here.";
    AIProtocol.Reply reply = AIProtocol.parse(raw);
    assertFalse(reply.parsed());
    assertEquals(raw, reply.say());
    assertTrue(reply.options().isEmpty());
    assertFalse(reply.questionAsked());
    assertNull(reply.answerVerdict());
    assertFalse(reply.stageCompleteClaim());
  }

  @Test
  void parseMissingFieldsUsesDefaults() {
    AIProtocol.Reply reply = AIProtocol.parse("{\"say\": \"Only say\"}");
    assertTrue(reply.parsed());
    assertEquals("Only say", reply.say());
    assertTrue(reply.options().isEmpty());
    assertFalse(reply.questionAsked());
    assertNull(reply.answerVerdict());
    assertFalse(reply.stageCompleteClaim());
  }

  @Test
  void parseNullFieldsAreNullSafe() {
    AIProtocol.Reply reply =
        AIProtocol.parse(
            "{\"say\": null, \"options\": null, \"question_asked\": null,"
                + " \"answer_verdict\": null, \"stage_complete_claim\": null}");
    assertTrue(reply.parsed());
    assertNull(reply.say());
    assertTrue(reply.options().isEmpty());
    assertFalse(reply.questionAsked());
    assertNull(reply.answerVerdict());
    assertFalse(reply.stageCompleteClaim());
  }

  @Test
  void parseNullInputReturnsUnparsed() {
    AIProtocol.Reply reply = AIProtocol.parse(null);
    assertFalse(reply.parsed());
    assertTrue(reply.options().isEmpty());
  }

  @Test
  void parseTopLevelJsonArrayReturnsUnparsed() {
    AIProtocol.Reply reply = AIProtocol.parse("[{\"say\":\"x\"}]");
    assertFalse(reply.parsed());
    assertTrue(reply.options().isEmpty());
  }

  @Test
  void parseTopLevelNumberReturnsUnparsed() {
    AIProtocol.Reply reply = AIProtocol.parse("42");
    assertFalse(reply.parsed());
    assertTrue(reply.options().isEmpty());
  }

  @Test
  void parseEmptyStringReturnsUnparsed() {
    AIProtocol.Reply reply = AIProtocol.parse("");
    assertFalse(reply.parsed());
    assertTrue(reply.options().isEmpty());
  }

  @Test
  void parseExtractsCommandField() {
    String raw =
        "{\"say\": \"Let's go!\", \"command\": \"worldborder center 100 200\","
            + " \"options\": [{\"id\": \"yes\", \"label\": \"Да\"}]}";
    AIProtocol.Reply reply = AIProtocol.parse(raw);
    assertTrue(reply.parsed());
    assertEquals("worldborder center 100 200", reply.command());
    assertEquals("Да", reply.options().get(0).label());
  }

  @Test
  void parseCommandDefaultsToNullWhenAbsent() {
    AIProtocol.Reply reply = AIProtocol.parse("{\"say\": \"Hi\"}");
    assertTrue(reply.parsed());
    assertNull(reply.command());
  }

  @Test
  void parseCommandNullSafe() {
    AIProtocol.Reply reply = AIProtocol.parse("{\"say\": \"Hi\", \"command\": null}");
    assertTrue(reply.parsed());
    assertNull(reply.command());
  }

  @Test
  void parseUnparsedReplyHasNullCommand() {
    AIProtocol.Reply reply = AIProtocol.parse("plain text, not json");
    assertFalse(reply.parsed());
    assertNull(reply.command());
  }
}
