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

package de.markusbordihn.easynpc.data.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

class DialogDataSetPersonaTest {

  @Test
  void hasPersona_falseWhenEmpty() {
    DialogDataSet ds = new DialogDataSet(DialogType.AI);
    assertFalse(ds.hasPersona());
  }

  @Test
  void hasPersona_trueAfterAnyField() {
    DialogDataSet ds = new DialogDataSet(DialogType.AI);
    ds.setAIPersonaName("Griselda");
    assertTrue(ds.hasPersona());
  }

  @Test
  void compileEffectiveSystemPrompt_returnsRawWhenNoPersona() {
    DialogDataSet ds = new DialogDataSet(DialogType.AI);
    ds.setAISystemPrompt("raw prompt");
    assertEquals("raw prompt", ds.compileEffectiveSystemPrompt());
  }

  @Test
  void compileEffectiveSystemPrompt_buildsIdentityLine() {
    DialogDataSet ds = new DialogDataSet(DialogType.AI);
    ds.setAIPersonaName("Griselda");
    ds.setAIPersonaRace("Witch");
    ds.setAIPersonaClass("Trader");
    ds.setAIPersonaAlignment("Chaotic Neutral");
    String out = ds.compileEffectiveSystemPrompt();
    assertTrue(out.startsWith("You are Griselda"), out);
    assertTrue(out.contains("Witch"));
    assertTrue(out.contains("Trader"));
    assertTrue(out.contains("Chaotic Neutral"));
  }

  @Test
  void compileEffectiveSystemPrompt_includesAllPopulatedSections() {
    DialogDataSet ds = new DialogDataSet(DialogType.AI);
    ds.setAIPersonaName("Bob");
    ds.setAIPersonaPersonality("Gruff");
    ds.setAIPersonaQuirks("Hates wolves");
    ds.setAIPersonaBackstory("Lost his family to a wolf pack.");
    ds.setAIPersonaGoals("Avenge them.");
    ds.setAIPersonaSpeechStyle("Old-timer drawl");
    String out = ds.compileEffectiveSystemPrompt();
    assertTrue(out.contains("Personality: Gruff"));
    assertTrue(out.contains("Quirks (ideals, bonds, flaws): Hates wolves"));
    assertTrue(out.contains("Backstory: Lost his family to a wolf pack."));
    assertTrue(out.contains("Goals: Avenge them."));
    assertTrue(out.contains("Speech style: Old-timer drawl"));
    assertTrue(out.contains("Stay strictly in character"));
  }

  @Test
  void compileEffectiveSystemPrompt_appendsRawPromptAsAdditional() {
    DialogDataSet ds = new DialogDataSet(DialogType.AI);
    ds.setAIPersonaName("X");
    ds.setAISystemPrompt("Never mention dragons.");
    String out = ds.compileEffectiveSystemPrompt();
    assertTrue(out.contains("Additional instructions"), out);
    assertTrue(out.contains("Never mention dragons."));
  }

  @Test
  void nbtRoundtrip_preservesPersona() {
    DialogDataSet src = new DialogDataSet(DialogType.AI);
    src.setAIServerUrl("http://x");
    src.setAIPersonaName("Griselda");
    src.setAIPersonaRace("Witch");
    src.setAIPersonaBackstory("Long story...");
    src.setAIPersonaGoals("Find herbs");

    CompoundTag tag = new CompoundTag();
    src.save(tag);

    DialogDataSet copy = new DialogDataSet(tag);
    assertEquals("Griselda", copy.getAIPersonaName());
    assertEquals("Witch", copy.getAIPersonaRace());
    assertEquals("Long story...", copy.getAIPersonaBackstory());
    assertEquals("Find herbs", copy.getAIPersonaGoals());
  }
}
