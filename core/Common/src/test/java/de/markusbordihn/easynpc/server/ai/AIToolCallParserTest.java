/*
 * Copyright 2025 Markus Bordihn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING
 * BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package de.markusbordihn.easynpc.server.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AIToolCallParserTest {

  @Test
  void parse_emptyInput_zeroDelta() {
    AIToolCallParser.Result r = AIToolCallParser.parse("");
    assertEquals("", r.cleanedText());
    assertEquals(0, r.relationDelta());
  }

  @Test
  void parse_noTags_passesThrough() {
    AIToolCallParser.Result r = AIToolCallParser.parse("Hello there.");
    assertEquals("Hello there.", r.cleanedText());
    assertEquals(0, r.relationDelta());
  }

  @Test
  void parse_extractsPositiveDelta() {
    AIToolCallParser.Result r =
        AIToolCallParser.parse("Thanks a lot, friend! [adjust_relation:+10]");
    assertEquals("Thanks a lot, friend!", r.cleanedText());
    assertEquals(10, r.relationDelta());
  }

  @Test
  void parse_extractsNegativeDelta() {
    AIToolCallParser.Result r = AIToolCallParser.parse("Get lost. [adjust_relation:-15]");
    assertEquals("Get lost.", r.cleanedText());
    assertEquals(-15, r.relationDelta());
  }

  @Test
  void parse_aggregatesMultipleTags() {
    AIToolCallParser.Result r =
        AIToolCallParser.parse("[adjust_relation:+5] Hi [adjust_relation:-3] bye.");
    assertEquals("Hi bye.", r.cleanedText());
    assertEquals(2, r.relationDelta());
  }

  @Test
  void parse_unsignedDigitsAreTreatedAsPositive() {
    AIToolCallParser.Result r = AIToolCallParser.parse("[adjust_relation:7] hi");
    assertEquals("hi", r.cleanedText());
    assertEquals(7, r.relationDelta());
  }
}
