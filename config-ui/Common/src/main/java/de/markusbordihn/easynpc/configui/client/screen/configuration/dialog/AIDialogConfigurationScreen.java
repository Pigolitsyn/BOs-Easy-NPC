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

package de.markusbordihn.easynpc.configui.client.screen.configuration.dialog;

import de.markusbordihn.easynpc.client.screen.components.Text;
import de.markusbordihn.easynpc.client.screen.components.TextButton;
import de.markusbordihn.easynpc.client.screen.components.TextField;
import de.markusbordihn.easynpc.configui.client.screen.components.CancelButton;
import de.markusbordihn.easynpc.configui.client.screen.components.Checkbox;
import de.markusbordihn.easynpc.configui.client.screen.components.SaveButton;
import de.markusbordihn.easynpc.configui.client.screen.configuration.ConfigurationScreen;
import de.markusbordihn.easynpc.configui.menu.configuration.ConfigurationMenu;
import de.markusbordihn.easynpc.configui.network.NetworkMessageHandlerManager;
import de.markusbordihn.easynpc.data.dialog.DialogDataSet;
import de.markusbordihn.easynpc.data.dialog.DialogType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class AIDialogConfigurationScreen<T extends ConfigurationMenu>
    extends ConfigurationScreen<T> {

  private enum Tab {
    CONNECTION,
    BEHAVIOR,
    SPEECH_BUBBLE,
    PERSONA
  }

  private Tab currentTab = Tab.CONNECTION;

  // Tab navigation buttons
  private Button connectionTabButton;
  private Button behaviorTabButton;
  private Button speechBubbleTabButton;
  private Button personaTabButton;

  // Connection tab fields
  private EditBox serverUrlBox;
  private EditBox modelNameBox;
  private EditBox apiKeyBox;

  // Behavior tab fields
  private EditBox systemPromptBox;
  private EditBox maxHistoryBox;

  // Speech Bubble tab fields
  private Checkbox speechBubbleEnabledBox;
  private EditBox speechBubbleRadiusBox;
  private EditBox speechBubbleIntervalBox;

  // Persona tab fields
  private EditBox personaNameBox;
  private EditBox personaRaceBox;
  private EditBox personaClassBox;
  private EditBox personaAlignmentBox;
  private EditBox personaPersonalityBox;
  private EditBox personaQuirksBox;
  private EditBox personaGoalsBox;
  private EditBox personaSpeechStyleBox;
  private EditBox personaBackstoryBox;

  // Bottom action button
  private Button saveButton;

  // Saved values for dirty-state detection
  private String serverUrlValue = "";
  private String modelNameValue = "";
  private String apiKeyValue = "";
  private String systemPromptValue = "";
  private int maxHistorySizeValue = 20;
  private boolean speechBubbleEnabledValue = false;
  private int speechBubbleRadiusValue = 8;
  private int speechBubbleIntervalValue = 1200;
  private String personaNameValue = "";
  private String personaRaceValue = "";
  private String personaClassValue = "";
  private String personaAlignmentValue = "";
  private String personaPersonalityValue = "";
  private String personaQuirksValue = "";
  private String personaGoalsValue = "";
  private String personaSpeechStyleValue = "";
  private String personaBackstoryValue = "";

  public AIDialogConfigurationScreen(T menu, Inventory inventory, Component component) {
    super(menu, inventory, component);
  }

  @Override
  public void init() {
    super.init();

    DialogDataSet ds = this.getDialogDataSet();
    if (ds != null && ds.getType() == DialogType.AI) {
      this.serverUrlValue = ds.getAIServerUrl();
      this.modelNameValue = ds.getAIModelName();
      this.apiKeyValue = ds.getAIApiKey();
      this.systemPromptValue = ds.getAISystemPrompt();
      this.maxHistorySizeValue = ds.getAIMaxHistorySize();
      this.speechBubbleEnabledValue = ds.isAISpeechBubbleEnabled();
      this.speechBubbleRadiusValue = ds.getAISpeechBubbleRadius();
      this.speechBubbleIntervalValue = ds.getAISpeechBubbleIntervalTicks();
      this.personaNameValue = ds.getAIPersonaName();
      this.personaRaceValue = ds.getAIPersonaRace();
      this.personaClassValue = ds.getAIPersonaClass();
      this.personaAlignmentValue = ds.getAIPersonaAlignment();
      this.personaPersonalityValue = ds.getAIPersonaPersonality();
      this.personaQuirksValue = ds.getAIPersonaQuirks();
      this.personaGoalsValue = ds.getAIPersonaGoals();
      this.personaSpeechStyleValue = ds.getAIPersonaSpeechStyle();
      this.personaBackstoryValue = ds.getAIPersonaBackstory();
    }

    // Tab navigation row
    this.connectionTabButton =
        this.addRenderableWidget(
            new TextButton(
                this.buttonLeftPos,
                this.buttonTopPos,
                65,
                "ai.tab.connection",
                onPress -> switchTab(Tab.CONNECTION)));
    this.behaviorTabButton =
        this.addRenderableWidget(
            new TextButton(
                this.buttonLeftPos + 65,
                this.buttonTopPos,
                55,
                "ai.tab.behavior",
                onPress -> switchTab(Tab.BEHAVIOR)));
    this.speechBubbleTabButton =
        this.addRenderableWidget(
            new TextButton(
                this.buttonLeftPos + 120,
                this.buttonTopPos,
                85,
                "ai.tab.speech_bubble",
                onPress -> switchTab(Tab.SPEECH_BUBBLE)));
    this.personaTabButton =
        this.addRenderableWidget(
            new TextButton(
                this.buttonLeftPos + 205,
                this.buttonTopPos,
                60,
                "ai.tab.persona",
                onPress -> switchTab(Tab.PERSONA)));

    // Connection tab
    this.serverUrlBox = new TextField(this.font, this.contentLeftPos, this.contentTopPos + 15, 306);
    this.serverUrlBox.setMaxLength(512);
    this.serverUrlBox.setValue(this.serverUrlValue);
    this.serverUrlBox.setHint(Component.literal("http://localhost:1234"));
    this.addRenderableWidget(this.serverUrlBox);

    this.modelNameBox = new TextField(this.font, this.contentLeftPos, this.contentTopPos + 55, 200);
    this.modelNameBox.setMaxLength(128);
    this.modelNameBox.setValue(this.modelNameValue);
    this.modelNameBox.setHint(Component.literal("llama3"));
    this.addRenderableWidget(this.modelNameBox);

    this.apiKeyBox = new TextField(this.font, this.contentLeftPos, this.contentTopPos + 95, 306);
    this.apiKeyBox.setMaxLength(256);
    this.apiKeyBox.setValue(this.apiKeyValue);
    this.apiKeyBox.setHint(Component.translatable("easynpc.dialog.ai.api_key_hint"));
    this.addRenderableWidget(this.apiKeyBox);

    // Behavior tab
    this.systemPromptBox =
        new TextField(this.font, this.contentLeftPos, this.contentTopPos + 15, 306);
    this.systemPromptBox.setMaxLength(2048);
    this.systemPromptBox.setValue(this.systemPromptValue);
    this.systemPromptBox.setHint(Component.translatable("easynpc.dialog.ai.system_prompt_hint"));
    this.addRenderableWidget(this.systemPromptBox);

    this.maxHistoryBox =
        new TextField(this.font, this.contentLeftPos, this.contentTopPos + 55, 50);
    this.maxHistoryBox.setMaxLength(3);
    this.maxHistoryBox.setValue(String.valueOf(this.maxHistorySizeValue));
    this.addRenderableWidget(this.maxHistoryBox);

    // Speech Bubble tab
    this.speechBubbleEnabledBox =
        new Checkbox(
            this.contentLeftPos,
            this.contentTopPos + 10,
            "ai.speech_bubble.enabled",
            this.speechBubbleEnabledValue,
            checkbox -> {});
    this.addRenderableWidget(this.speechBubbleEnabledBox);

    this.speechBubbleRadiusBox =
        new TextField(this.font, this.contentLeftPos, this.contentTopPos + 55, 50);
    this.speechBubbleRadiusBox.setMaxLength(3);
    this.speechBubbleRadiusBox.setValue(String.valueOf(this.speechBubbleRadiusValue));
    this.addRenderableWidget(this.speechBubbleRadiusBox);

    this.speechBubbleIntervalBox =
        new TextField(this.font, this.contentLeftPos, this.contentTopPos + 95, 50);
    this.speechBubbleIntervalBox.setMaxLength(5);
    this.speechBubbleIntervalBox.setValue(String.valueOf(this.speechBubbleIntervalValue / 20));
    this.addRenderableWidget(this.speechBubbleIntervalBox);

    // Persona tab — 2 columns of short fields + backstory below
    int col1X = this.contentLeftPos;
    int col2X = this.contentLeftPos + 155;
    int rowH = 32;
    int colW = 145;

    this.personaNameBox = makePersonaShort(col1X, this.contentTopPos + 15, colW, 64, this.personaNameValue);
    this.personaRaceBox = makePersonaShort(col1X, this.contentTopPos + 15 + rowH, colW, 64, this.personaRaceValue);
    this.personaClassBox = makePersonaShort(col1X, this.contentTopPos + 15 + 2 * rowH, colW, 64, this.personaClassValue);
    this.personaAlignmentBox = makePersonaShort(col1X, this.contentTopPos + 15 + 3 * rowH, colW, 64, this.personaAlignmentValue);

    this.personaPersonalityBox = makePersonaShort(col2X, this.contentTopPos + 15, colW, 256, this.personaPersonalityValue);
    this.personaQuirksBox = makePersonaShort(col2X, this.contentTopPos + 15 + rowH, colW, 256, this.personaQuirksValue);
    this.personaGoalsBox = makePersonaShort(col2X, this.contentTopPos + 15 + 2 * rowH, colW, 256, this.personaGoalsValue);
    this.personaSpeechStyleBox = makePersonaShort(col2X, this.contentTopPos + 15 + 3 * rowH, colW, 128, this.personaSpeechStyleValue);

    this.personaBackstoryBox =
        new TextField(this.font, this.contentLeftPos, this.contentTopPos + 15 + 4 * rowH + 12, 306);
    this.personaBackstoryBox.setMaxLength(1024);
    this.personaBackstoryBox.setValue(this.personaBackstoryValue);
    this.personaBackstoryBox.setHint(
        Component.translatable("easynpc.dialog.ai.persona.backstory_hint"));
    this.addRenderableWidget(this.personaBackstoryBox);

    // Bottom buttons
    this.addRenderableWidget(
        new CancelButton(
            this.rightPos - 130, this.bottomPos - 40, "cancel", onPress -> this.showMainScreen()));

    this.saveButton =
        this.addRenderableWidget(
            new SaveButton(
                this.contentLeftPos + 26, this.bottomPos - 40, "save", onPress -> saveConfig()));

    updateTabVisibility();
  }

  private EditBox makePersonaShort(int x, int y, int w, int max, String value) {
    EditBox box = new TextField(this.font, x, y, w);
    box.setMaxLength(max);
    box.setValue(value == null ? "" : value);
    this.addRenderableWidget(box);
    return box;
  }

  private void switchTab(Tab tab) {
    this.currentTab = tab;
    updateTabVisibility();
  }

  private void updateTabVisibility() {
    boolean conn = currentTab == Tab.CONNECTION;
    boolean beh = currentTab == Tab.BEHAVIOR;
    boolean speech = currentTab == Tab.SPEECH_BUBBLE;
    boolean persona = currentTab == Tab.PERSONA;

    this.serverUrlBox.visible = conn;
    this.modelNameBox.visible = conn;
    this.apiKeyBox.visible = conn;

    this.systemPromptBox.visible = beh;
    this.maxHistoryBox.visible = beh;

    this.speechBubbleEnabledBox.visible = speech;
    this.speechBubbleRadiusBox.visible = speech;
    this.speechBubbleIntervalBox.visible = speech;

    this.personaNameBox.visible = persona;
    this.personaRaceBox.visible = persona;
    this.personaClassBox.visible = persona;
    this.personaAlignmentBox.visible = persona;
    this.personaPersonalityBox.visible = persona;
    this.personaQuirksBox.visible = persona;
    this.personaGoalsBox.visible = persona;
    this.personaSpeechStyleBox.visible = persona;
    this.personaBackstoryBox.visible = persona;

    this.connectionTabButton.active = !conn;
    this.behaviorTabButton.active = !beh;
    this.speechBubbleTabButton.active = !speech;
    this.personaTabButton.active = !persona;
  }

  private void saveConfig() {
    DialogDataSet dialogDataSet = new DialogDataSet(DialogType.AI);
    dialogDataSet.setAIServerUrl(this.serverUrlBox.getValue());
    dialogDataSet.setAIModelName(this.modelNameBox.getValue());
    dialogDataSet.setAIApiKey(this.apiKeyBox.getValue());
    dialogDataSet.setAISystemPrompt(this.systemPromptBox.getValue());

    try {
      dialogDataSet.setAIMaxHistorySize(Integer.parseInt(this.maxHistoryBox.getValue().trim()));
    } catch (NumberFormatException ignored) {
      dialogDataSet.setAIMaxHistorySize(20);
    }

    dialogDataSet.setAISpeechBubbleEnabled(this.speechBubbleEnabledBox.selected());

    try {
      dialogDataSet.setAISpeechBubbleRadius(
          Integer.parseInt(this.speechBubbleRadiusBox.getValue().trim()));
    } catch (NumberFormatException ignored) {
      dialogDataSet.setAISpeechBubbleRadius(8);
    }

    try {
      int seconds = Integer.parseInt(this.speechBubbleIntervalBox.getValue().trim());
      dialogDataSet.setAISpeechBubbleIntervalTicks(seconds * 20);
    } catch (NumberFormatException ignored) {
      dialogDataSet.setAISpeechBubbleIntervalTicks(1200);
    }

    dialogDataSet.setAIPersonaName(this.personaNameBox.getValue());
    dialogDataSet.setAIPersonaRace(this.personaRaceBox.getValue());
    dialogDataSet.setAIPersonaClass(this.personaClassBox.getValue());
    dialogDataSet.setAIPersonaAlignment(this.personaAlignmentBox.getValue());
    dialogDataSet.setAIPersonaPersonality(this.personaPersonalityBox.getValue());
    dialogDataSet.setAIPersonaQuirks(this.personaQuirksBox.getValue());
    dialogDataSet.setAIPersonaGoals(this.personaGoalsBox.getValue());
    dialogDataSet.setAIPersonaSpeechStyle(this.personaSpeechStyleBox.getValue());
    dialogDataSet.setAIPersonaBackstory(this.personaBackstoryBox.getValue());

    NetworkMessageHandlerManager.getServerHandler()
        .saveDialogSet(this.getEasyNPCUUID(), dialogDataSet);

    // Sync saved values
    this.serverUrlValue = this.serverUrlBox.getValue();
    this.modelNameValue = this.modelNameBox.getValue();
    this.apiKeyValue = this.apiKeyBox.getValue();
    this.systemPromptValue = this.systemPromptBox.getValue();
    this.speechBubbleEnabledValue = this.speechBubbleEnabledBox.selected();
    try {
      this.maxHistorySizeValue = Integer.parseInt(this.maxHistoryBox.getValue().trim());
    } catch (NumberFormatException ignored) {
      this.maxHistorySizeValue = 20;
    }
    try {
      this.speechBubbleRadiusValue =
          Integer.parseInt(this.speechBubbleRadiusBox.getValue().trim());
    } catch (NumberFormatException ignored) {
      this.speechBubbleRadiusValue = 8;
    }
    try {
      this.speechBubbleIntervalValue =
          Integer.parseInt(this.speechBubbleIntervalBox.getValue().trim()) * 20;
    } catch (NumberFormatException ignored) {
      this.speechBubbleIntervalValue = 1200;
    }
    this.personaNameValue = this.personaNameBox.getValue();
    this.personaRaceValue = this.personaRaceBox.getValue();
    this.personaClassValue = this.personaClassBox.getValue();
    this.personaAlignmentValue = this.personaAlignmentBox.getValue();
    this.personaPersonalityValue = this.personaPersonalityBox.getValue();
    this.personaQuirksValue = this.personaQuirksBox.getValue();
    this.personaGoalsValue = this.personaGoalsBox.getValue();
    this.personaSpeechStyleValue = this.personaSpeechStyleBox.getValue();
    this.personaBackstoryValue = this.personaBackstoryBox.getValue();
  }

  @Override
  public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
    super.render(guiGraphics, mouseX, mouseY, partialTicks);
    switch (currentTab) {
      case CONNECTION -> {
        Text.drawConfigString(
            guiGraphics, this.font, "ai.server_url", this.contentLeftPos, this.contentTopPos + 5);
        Text.drawConfigString(
            guiGraphics, this.font, "ai.model_name", this.contentLeftPos, this.contentTopPos + 45);
        Text.drawConfigString(
            guiGraphics, this.font, "ai.api_key", this.contentLeftPos, this.contentTopPos + 85);
      }
      case BEHAVIOR -> {
        Text.drawConfigString(
            guiGraphics,
            this.font,
            "ai.system_prompt",
            this.contentLeftPos,
            this.contentTopPos + 5);
        Text.drawConfigString(
            guiGraphics,
            this.font,
            "ai.max_history",
            this.contentLeftPos,
            this.contentTopPos + 45);
      }
      case SPEECH_BUBBLE -> {
        Text.drawConfigString(
            guiGraphics,
            this.font,
            "ai.speech_bubble_radius",
            this.contentLeftPos,
            this.contentTopPos + 45);
        Text.drawConfigString(
            guiGraphics,
            this.font,
            "ai.speech_bubble_interval",
            this.contentLeftPos,
            this.contentTopPos + 85);
      }
      case PERSONA -> renderPersonaLabels(guiGraphics);
    }
  }

  private void renderPersonaLabels(GuiGraphics guiGraphics) {
    int col1X = this.contentLeftPos;
    int col2X = this.contentLeftPos + 155;
    int baseY = this.contentTopPos + 5;
    int rowH = 32;
    Text.drawConfigString(guiGraphics, this.font, "ai.persona.name", col1X, baseY);
    Text.drawConfigString(guiGraphics, this.font, "ai.persona.race", col1X, baseY + rowH);
    Text.drawConfigString(guiGraphics, this.font, "ai.persona.class", col1X, baseY + 2 * rowH);
    Text.drawConfigString(guiGraphics, this.font, "ai.persona.alignment", col1X, baseY + 3 * rowH);
    Text.drawConfigString(guiGraphics, this.font, "ai.persona.personality", col2X, baseY);
    Text.drawConfigString(guiGraphics, this.font, "ai.persona.quirks", col2X, baseY + rowH);
    Text.drawConfigString(guiGraphics, this.font, "ai.persona.goals", col2X, baseY + 2 * rowH);
    Text.drawConfigString(
        guiGraphics, this.font, "ai.persona.speech_style", col2X, baseY + 3 * rowH);
    Text.drawConfigString(
        guiGraphics, this.font, "ai.persona.backstory", col1X, baseY + 4 * rowH + 2);
  }

  @Override
  public void updateTick() {
    super.updateTick();
    if (saveButton == null) return;
    saveButton.active =
        !serverUrlBox.getValue().equals(serverUrlValue)
            || !modelNameBox.getValue().equals(modelNameValue)
            || !apiKeyBox.getValue().equals(apiKeyValue)
            || !systemPromptBox.getValue().equals(systemPromptValue)
            || speechBubbleEnabledBox.selected() != speechBubbleEnabledValue
            || !speechBubbleRadiusBox.getValue().equals(String.valueOf(speechBubbleRadiusValue))
            || !speechBubbleIntervalBox
                .getValue()
                .equals(String.valueOf(speechBubbleIntervalValue / 20))
            || !maxHistoryBox.getValue().equals(String.valueOf(maxHistorySizeValue))
            || !personaNameBox.getValue().equals(personaNameValue)
            || !personaRaceBox.getValue().equals(personaRaceValue)
            || !personaClassBox.getValue().equals(personaClassValue)
            || !personaAlignmentBox.getValue().equals(personaAlignmentValue)
            || !personaPersonalityBox.getValue().equals(personaPersonalityValue)
            || !personaQuirksBox.getValue().equals(personaQuirksValue)
            || !personaGoalsBox.getValue().equals(personaGoalsValue)
            || !personaSpeechStyleBox.getValue().equals(personaSpeechStyleValue)
            || !personaBackstoryBox.getValue().equals(personaBackstoryValue);
  }
}
