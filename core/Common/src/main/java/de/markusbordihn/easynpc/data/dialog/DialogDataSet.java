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

package de.markusbordihn.easynpc.data.dialog;

import de.markusbordihn.easynpc.Constants;
import de.markusbordihn.easynpc.data.condition.ConditionDataEntry;
import de.markusbordihn.easynpc.data.condition.ConditionType;
import de.markusbordihn.easynpc.data.condition.ConditionUtils;
import de.markusbordihn.easynpc.network.syncher.EntityDataSerializersManager;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DialogDataSet {

  public static final String DATA_DIALOG_DATA_SET_TAG = "DialogDataSet";
  public static final String DATA_TYPE_TAG = "Type";
  public static final StreamCodec<RegistryFriendlyByteBuf, DialogDataSet> STREAM_CODEC =
      new StreamCodec<>() {
        @Override
        public DialogDataSet decode(RegistryFriendlyByteBuf registryFriendlyByteBuf) {
          return new DialogDataSet(registryFriendlyByteBuf.readNbt());
        }

        @Override
        public void encode(
            RegistryFriendlyByteBuf registryFriendlyByteBuf, DialogDataSet dialogDataSet) {
          registryFriendlyByteBuf.writeNbt(
              EntityDataSerializersManager.validateAndGetNbt(
                  dialogDataSet.createTag(), "DialogDataSet"));
        }
      };
  public static final String DATA_AI_SERVER_URL_TAG = "AIServerUrl";
  public static final String DATA_AI_MODEL_NAME_TAG = "AIModelName";
  public static final String DATA_AI_SYSTEM_PROMPT_TAG = "AISystemPrompt";
  public static final String DATA_AI_API_KEY_TAG = "AIApiKey";
  public static final String DATA_AI_MAX_HISTORY_TAG = "AIMaxHistory";
  public static final String DATA_AI_SPEECH_BUBBLE_TAG = "AISpeechBubble";
  public static final String DATA_AI_SPEECH_BUBBLE_RADIUS_TAG = "AISpeechBubbleRadius";
  public static final String DATA_AI_SPEECH_BUBBLE_INTERVAL_TAG = "AISpeechBubbleInterval";
  public static final String DATA_AI_PERSONA_NAME_TAG = "AIPersonaName";
  public static final String DATA_AI_PERSONA_RACE_TAG = "AIPersonaRace";
  public static final String DATA_AI_PERSONA_CLASS_TAG = "AIPersonaClass";
  public static final String DATA_AI_PERSONA_ALIGNMENT_TAG = "AIPersonaAlignment";
  public static final String DATA_AI_PERSONA_PERSONALITY_TAG = "AIPersonaPersonality";
  public static final String DATA_AI_PERSONA_QUIRKS_TAG = "AIPersonaQuirks";
  public static final String DATA_AI_PERSONA_BACKSTORY_TAG = "AIPersonaBackstory";
  public static final String DATA_AI_PERSONA_GOALS_TAG = "AIPersonaGoals";
  public static final String DATA_AI_PERSONA_SPEECH_STYLE_TAG = "AIPersonaSpeechStyle";
  public static final String DATA_AI_QUEST_OBJECTIVE_TAG = "AIQuestObjective";
  public static final String DATA_AI_REQUIRED_CORRECT_TAG = "AIRequiredCorrect";
  private static final Logger log = LogManager.getLogger(Constants.LOG_NAME);
  private final HashMap<String, DialogDataEntry> dialogByLabelMap = new HashMap<>();
  private final HashMap<UUID, DialogDataEntry> dialogByIdMap = new HashMap<>();
  private DialogType dialogType = DialogType.STANDARD;
  private String aiServerUrl = "";
  private String aiModelName = "";
  private String aiSystemPrompt = "";
  private String aiApiKey = "";
  private int aiMaxHistorySize = 20;
  private boolean aiSpeechBubbleEnabled = false;
  private int aiSpeechBubbleRadius = 8;
  private int aiSpeechBubbleIntervalTicks = 1200;
  private String aiPersonaName = "";
  private String aiPersonaRace = "";
  private String aiPersonaClass = "";
  private String aiPersonaAlignment = "";
  private String aiPersonaPersonality = "";
  private String aiPersonaQuirks = "";
  private String aiPersonaBackstory = "";
  private String aiPersonaGoals = "";
  private String aiPersonaSpeechStyle = "";
  private String aiQuestObjective = "";
  private int aiRequiredCorrect = 0;

  public DialogDataSet() {}

  public DialogDataSet(DialogType dialogType) {
    this.dialogType = dialogType;
  }

  public DialogDataSet(CompoundTag compoundTag) {
    this.load(compoundTag);
  }

  public void setDialog(UUID dialogId, DialogDataEntry dialogData) {
    if (dialogData == null) {
      log.error("Dialog data is null, please check your dialog data!");
      return;
    }
    if (this.hasDialog(dialogId)) {
      removeDialog(dialogId);
    }
    this.addDialog(dialogData);
  }

  public boolean addDialog(DialogDataEntry dialogData) {
    // Pre-check dialog data, before adding it to the dialog set.
    if (dialogData == null) {
      log.error("Dialog data is null, please check your dialog data!");
      return false;
    }
    if (dialogData.getId() == null) {
      log.error("Dialog id is null, please check your dialog data!");
      return false;
    }
    if (dialogData.getLabel() == null) {
      log.error("Dialog label is null, please check your dialog data!");
      return false;
    }
    if (dialogData.getText() == null || dialogData.getText().isEmpty()) {
      log.error("Dialog text is null or empty, please check your dialog data!");
      return false;
    }

    String dialogLabel = dialogData.getLabel();
    UUID dialogId = dialogData.getId();

    // Warn about duplicated dialog ids
    DialogDataEntry existingDialogData = this.dialogByIdMap.getOrDefault(dialogId, null);
    if (existingDialogData != null && !existingDialogData.equals(dialogData)) {
      log.warn(
          "Duplicated dialog with id {} found, will overwrite existing dialog {} with {}!",
          dialogId,
          dialogData,
          existingDialogData);
    }

    this.dialogByLabelMap.put(dialogLabel, dialogData);
    this.dialogByIdMap.put(dialogId, dialogData);
    return true;
  }

  public boolean removeDialog(UUID dialogId) {
    DialogDataEntry dialogData = this.dialogByIdMap.getOrDefault(dialogId, null);
    if (dialogData != null) {
      DialogDataEntry formerDialogData = this.dialogByIdMap.remove(dialogData.getId());
      if (formerDialogData != null) {
        this.dialogByLabelMap.remove(formerDialogData.getLabel());
      }
      return true;
    }
    return false;
  }

  public boolean removeDialogButton(UUID dialogId, UUID dialogButtonId) {
    DialogDataEntry dialogData = this.dialogByIdMap.getOrDefault(dialogId, null);
    if (dialogData != null) {
      return dialogData.removeDialogButton(dialogButtonId);
    }
    return false;
  }

  public List<DialogDataEntry> getDialogsByLabel() {
    return this.dialogByLabelMap.values().stream()
        .sorted(Comparator.comparing(DialogDataEntry::getLabel))
        .toList();
  }

  public Map<String, DialogDataEntry> getDialogByLabelMap() {
    return dialogByLabelMap;
  }

  public DialogDataEntry getDialog(String label) {
    return this.dialogByLabelMap.getOrDefault(label, null);
  }

  public DialogDataEntry getDialog(UUID id) {
    return this.dialogByIdMap.getOrDefault(id, null);
  }

  public UUID getDialogId(String dialogLabel) {
    DialogDataEntry dialogData = this.dialogByLabelMap.getOrDefault(dialogLabel, null);
    if (dialogData != null) {
      return dialogData.getId();
    }
    return null;
  }

  public boolean hasDialog() {
    return !this.dialogByLabelMap.isEmpty();
  }

  public boolean hasDialog(String label) {
    return this.dialogByLabelMap.containsKey(label);
  }

  public boolean hasDialog(UUID id) {
    return this.dialogByIdMap.containsKey(id);
  }

  public boolean hasDialogButton(UUID dialogId, UUID dialogButtonId) {
    return this.dialogByIdMap.containsKey(dialogId)
        && this.dialogByIdMap.get(dialogId).hasDialogButton(dialogButtonId);
  }

  public DialogButtonEntry getDialogButton(UUID dialogId, UUID dialogButtonId) {
    DialogDataEntry dialogData = this.dialogByIdMap.getOrDefault(dialogId, null);
    if (dialogData != null) {
      return dialogData.getDialogButton(dialogButtonId);
    }
    return null;
  }

  public List<DialogDataEntry> getChildren(UUID parentId) {
    return this.dialogByIdMap.values().stream()
        .filter(d -> parentId.equals(d.getParentDialogId()))
        .sorted(Comparator.comparing(DialogDataEntry::getLabel))
        .toList();
  }

  public List<DialogDataEntry> getRootDialogs() {
    return this.dialogByIdMap.values().stream()
        .filter(DialogDataEntry::isRoot)
        .sorted(Comparator.comparing(DialogDataEntry::getLabel))
        .toList();
  }

  public int getTreeDepth(UUID dialogId) {
    int depth = 0;
    UUID current = dialogId;
    while (current != null) {
      DialogDataEntry entry = this.dialogByIdMap.get(current);
      if (entry == null || entry.getParentDialogId() == null) {
        break;
      }
      current = entry.getParentDialogId();
      depth++;
      if (depth > 64) {
        break;
      }
    }
    return depth;
  }

  public DialogDataEntry getNextAvailableDialog(ServerPlayer serverPlayer) {
    return dialogByIdMap.values().stream()
        .filter(dialog -> dialog.getPriority() >= DialogPriority.FALLBACK)
        .filter(dialog -> checkConditions(dialog, serverPlayer))
        .sorted(
            Comparator.comparingInt(DialogDataEntry::getPriority)
                .reversed()
                .thenComparing(Comparator.comparing(DialogDataEntry::getLabel)))
        .findFirst()
        .orElse(null);
  }

  private boolean checkConditions(DialogDataEntry dialog, ServerPlayer serverPlayer) {
    if (!dialog.hasConditions()) {
      log.debug("Dialog {} has no conditions, allowing", dialog.getLabel());
      return true;
    }

    if (serverPlayer == null) {
      log.debug(
          "Cannot check conditions for dialog {} without player context, allowing dialog by default",
          dialog.getLabel());
      return true;
    }

    for (ConditionDataEntry condition : dialog.getConditions()) {
      if (!condition.isValid()) {
        log.debug("Skipping invalid condition {} for dialog {}", condition, dialog.getLabel());
        continue;
      }

      boolean conditionResult = evaluateCondition(condition, serverPlayer, dialog.getId());
      log.debug(
          "Condition check for dialog {}: {} {} {} = {} (result: {})",
          dialog.getLabel(),
          condition.conditionType(),
          condition.name(),
          condition.operationType().getSymbol() + " " + condition.value(),
          conditionResult ? "PASS" : "FAIL",
          conditionResult);

      if (!conditionResult) {
        log.debug(
            "Dialog {} rejected: condition not met ({} {} {} {})",
            dialog.getLabel(),
            condition.conditionType(),
            condition.name(),
            condition.operationType().getSymbol(),
            condition.value());
        return false;
      }
    }

    log.debug("Dialog {} accepted: all conditions passed", dialog.getLabel());
    return true;
  }

  public void recordDialogExecution(DialogDataEntry dialog, ServerPlayer serverPlayer) {
    if (dialog == null || serverPlayer == null || !dialog.hasConditions()) {
      return;
    }

    for (ConditionDataEntry condition : dialog.getConditions()) {
      if (condition.conditionType() == ConditionType.EXECUTION_LIMIT && condition.isValid()) {
        ConditionUtils.recordActionExecution(condition, serverPlayer, dialog.getId());
      }
    }
  }

  private boolean evaluateCondition(
      ConditionDataEntry condition, ServerPlayer serverPlayer, UUID dialogId) {
    return switch (condition.conditionType()) {
      case SCOREBOARD -> evaluateScoreboardCondition(condition, serverPlayer);
      case EXECUTION_LIMIT -> ConditionUtils.evaluateCondition(condition, serverPlayer, dialogId);
      case NONE -> {
        log.warn("Encountered NONE condition type, skipping");
        yield true;
      }
    };
  }

  private boolean evaluateScoreboardCondition(
      ConditionDataEntry condition, ServerPlayer serverPlayer) {
    if (!condition.hasName()) {
      log.warn("Scoreboard condition missing objective name!");
      return false;
    }

    int actualValue = -1;
    try {
      Scoreboard scoreboard = serverPlayer.level().getScoreboard();
      Objective objective = scoreboard.getObjective(condition.name());
      if (objective == null) {
        log.debug(
            "Scoreboard objective '{}' not found for player {}, using default value -1",
            condition.name(),
            serverPlayer.getName().getString());
      } else {
        actualValue = scoreboard.getOrCreatePlayerScore(serverPlayer, objective).get();
      }

      // Evaluate condition
      int expectedValue = condition.value();
      boolean result = condition.operationType().evaluate(actualValue, expectedValue);
      log.debug(
          "Scoreboard check: {} (actual: {}) {} {} (expected: {}) = {}",
          condition.name(),
          actualValue,
          condition.operationType().getSymbol(),
          expectedValue,
          expectedValue,
          result);

      return result;
    } catch (Exception e) {
      log.error("Error evaluating scoreboard condition for dialog: {}", condition, e);
      return false;
    }
  }

  public DialogType getType() {
    return this.dialogType;
  }

  public String getAIServerUrl() {
    return this.aiServerUrl;
  }

  public void setAIServerUrl(String aiServerUrl) {
    this.aiServerUrl = aiServerUrl != null ? aiServerUrl : "";
  }

  public String getAIModelName() {
    return this.aiModelName;
  }

  public void setAIModelName(String aiModelName) {
    this.aiModelName = aiModelName != null ? aiModelName : "";
  }

  public String getAISystemPrompt() {
    return this.aiSystemPrompt;
  }

  public void setAISystemPrompt(String aiSystemPrompt) {
    this.aiSystemPrompt = aiSystemPrompt != null ? aiSystemPrompt : "";
  }

  public String getAIApiKey() {
    return this.aiApiKey;
  }

  public void setAIApiKey(String aiApiKey) {
    this.aiApiKey = aiApiKey != null ? aiApiKey : "";
  }

  public int getAIMaxHistorySize() {
    return this.aiMaxHistorySize;
  }

  public void setAIMaxHistorySize(int aiMaxHistorySize) {
    this.aiMaxHistorySize = Math.max(1, Math.min(100, aiMaxHistorySize));
  }

  public boolean isAISpeechBubbleEnabled() {
    return this.aiSpeechBubbleEnabled;
  }

  public void setAISpeechBubbleEnabled(boolean aiSpeechBubbleEnabled) {
    this.aiSpeechBubbleEnabled = aiSpeechBubbleEnabled;
  }

  public int getAISpeechBubbleRadius() {
    return this.aiSpeechBubbleRadius;
  }

  public void setAISpeechBubbleRadius(int aiSpeechBubbleRadius) {
    this.aiSpeechBubbleRadius = Math.max(1, Math.min(64, aiSpeechBubbleRadius));
  }

  public int getAISpeechBubbleIntervalTicks() {
    return this.aiSpeechBubbleIntervalTicks;
  }

  public void setAISpeechBubbleIntervalTicks(int aiSpeechBubbleIntervalTicks) {
    this.aiSpeechBubbleIntervalTicks = Math.max(20, aiSpeechBubbleIntervalTicks);
  }

  public boolean hasAIConfig() {
    return this.dialogType == DialogType.AI && !this.aiServerUrl.isEmpty();
  }

  public String getAIPersonaName() {
    return this.aiPersonaName;
  }

  public void setAIPersonaName(String v) {
    this.aiPersonaName = v != null ? v : "";
  }

  public String getAIPersonaRace() {
    return this.aiPersonaRace;
  }

  public void setAIPersonaRace(String v) {
    this.aiPersonaRace = v != null ? v : "";
  }

  public String getAIPersonaClass() {
    return this.aiPersonaClass;
  }

  public void setAIPersonaClass(String v) {
    this.aiPersonaClass = v != null ? v : "";
  }

  public String getAIPersonaAlignment() {
    return this.aiPersonaAlignment;
  }

  public void setAIPersonaAlignment(String v) {
    this.aiPersonaAlignment = v != null ? v : "";
  }

  public String getAIPersonaPersonality() {
    return this.aiPersonaPersonality;
  }

  public void setAIPersonaPersonality(String v) {
    this.aiPersonaPersonality = v != null ? v : "";
  }

  public String getAIPersonaQuirks() {
    return this.aiPersonaQuirks;
  }

  public void setAIPersonaQuirks(String v) {
    this.aiPersonaQuirks = v != null ? v : "";
  }

  public String getAIPersonaBackstory() {
    return this.aiPersonaBackstory;
  }

  public void setAIPersonaBackstory(String v) {
    this.aiPersonaBackstory = v != null ? v : "";
  }

  public String getAIPersonaGoals() {
    return this.aiPersonaGoals;
  }

  public void setAIPersonaGoals(String v) {
    this.aiPersonaGoals = v != null ? v : "";
  }

  public String getAIPersonaSpeechStyle() {
    return this.aiPersonaSpeechStyle;
  }

  public void setAIPersonaSpeechStyle(String v) {
    this.aiPersonaSpeechStyle = v != null ? v : "";
  }

  public String getAIQuestObjective() {
    return this.aiQuestObjective;
  }

  public void setAIQuestObjective(String aiQuestObjective) {
    this.aiQuestObjective = aiQuestObjective != null ? aiQuestObjective : "";
  }

  public int getAIRequiredCorrect() {
    return this.aiRequiredCorrect;
  }

  public void setAIRequiredCorrect(int aiRequiredCorrect) {
    this.aiRequiredCorrect = aiRequiredCorrect;
  }

  public boolean hasPersona() {
    return !aiPersonaName.isEmpty()
        || !aiPersonaRace.isEmpty()
        || !aiPersonaClass.isEmpty()
        || !aiPersonaAlignment.isEmpty()
        || !aiPersonaPersonality.isEmpty()
        || !aiPersonaQuirks.isEmpty()
        || !aiPersonaBackstory.isEmpty()
        || !aiPersonaGoals.isEmpty()
        || !aiPersonaSpeechStyle.isEmpty();
  }

  /**
   * Builds an effective system prompt by composing persona fields. If no persona is set, returns
   * the raw {@code aiSystemPrompt}. Otherwise appends the user-provided prompt at the end as
   * additional instructions.
   */
  public String compileEffectiveSystemPrompt() {
    if (!hasPersona()) {
      return aiSystemPrompt;
    }
    StringBuilder sb = new StringBuilder();
    String identity = compactIdentity();
    if (!identity.isEmpty()) {
      sb.append("You are ").append(identity).append(".\n");
    }
    appendIf(sb, "Personality", aiPersonaPersonality);
    appendIf(sb, "Quirks (ideals, bonds, flaws)", aiPersonaQuirks);
    appendIf(sb, "Backstory", aiPersonaBackstory);
    appendIf(sb, "Goals", aiPersonaGoals);
    appendIf(sb, "Speech style", aiPersonaSpeechStyle);
    sb.append("Stay strictly in character. Keep replies short unless asked.\n");
    if (aiSystemPrompt != null && !aiSystemPrompt.isBlank()) {
      sb.append("\nAdditional instructions:\n").append(aiSystemPrompt);
    }
    return sb.toString();
  }

  private String compactIdentity() {
    StringBuilder sb = new StringBuilder();
    if (!aiPersonaName.isEmpty()) {
      sb.append(aiPersonaName);
    }
    boolean hasRace = !aiPersonaRace.isEmpty();
    boolean hasClass = !aiPersonaClass.isEmpty();
    boolean hasAlign = !aiPersonaAlignment.isEmpty();
    if (hasRace || hasClass || hasAlign) {
      if (sb.length() > 0) sb.append(", ");
      if (hasAlign) sb.append(aiPersonaAlignment).append(' ');
      if (hasRace) sb.append(aiPersonaRace);
      if (hasClass) {
        if (hasRace) sb.append(' ');
        sb.append(aiPersonaClass);
      }
    }
    return sb.toString().trim();
  }

  private static void appendIf(StringBuilder sb, String label, String value) {
    if (value != null && !value.isBlank()) {
      sb.append(label).append(": ").append(value.trim()).append('\n');
    }
  }

  public void load(CompoundTag compoundTag) {
    if (compoundTag == null || !compoundTag.contains(DATA_DIALOG_DATA_SET_TAG)) {
      return;
    }

    // Load dialog type
    if (compoundTag.contains(DATA_TYPE_TAG)) {
      this.dialogType = DialogType.valueOf(compoundTag.getString(DATA_TYPE_TAG).orElse(""));
    }

    // Load AI config
    if (compoundTag.contains(DATA_AI_SERVER_URL_TAG)) {
      this.aiServerUrl = compoundTag.getString(DATA_AI_SERVER_URL_TAG).orElse("");
    }
    if (compoundTag.contains(DATA_AI_MODEL_NAME_TAG)) {
      this.aiModelName = compoundTag.getString(DATA_AI_MODEL_NAME_TAG).orElse("");
    }
    if (compoundTag.contains(DATA_AI_SYSTEM_PROMPT_TAG)) {
      this.aiSystemPrompt = compoundTag.getString(DATA_AI_SYSTEM_PROMPT_TAG).orElse("");
    }
    if (compoundTag.contains(DATA_AI_API_KEY_TAG)) {
      this.aiApiKey = compoundTag.getString(DATA_AI_API_KEY_TAG).orElse("");
    }
    if (compoundTag.contains(DATA_AI_MAX_HISTORY_TAG)) {
      this.aiMaxHistorySize = compoundTag.getInt(DATA_AI_MAX_HISTORY_TAG).orElse(20);
    }
    if (compoundTag.contains(DATA_AI_SPEECH_BUBBLE_TAG)) {
      this.aiSpeechBubbleEnabled = compoundTag.getBoolean(DATA_AI_SPEECH_BUBBLE_TAG).orElse(false);
    }
    if (compoundTag.contains(DATA_AI_SPEECH_BUBBLE_RADIUS_TAG)) {
      this.aiSpeechBubbleRadius = compoundTag.getInt(DATA_AI_SPEECH_BUBBLE_RADIUS_TAG).orElse(8);
    }
    if (compoundTag.contains(DATA_AI_SPEECH_BUBBLE_INTERVAL_TAG)) {
      this.aiSpeechBubbleIntervalTicks = compoundTag.getInt(DATA_AI_SPEECH_BUBBLE_INTERVAL_TAG).orElse(1200);
    }
    this.aiPersonaName = compoundTag.getString(DATA_AI_PERSONA_NAME_TAG).orElse("");
    this.aiPersonaRace = compoundTag.getString(DATA_AI_PERSONA_RACE_TAG).orElse("");
    this.aiPersonaClass = compoundTag.getString(DATA_AI_PERSONA_CLASS_TAG).orElse("");
    this.aiPersonaAlignment = compoundTag.getString(DATA_AI_PERSONA_ALIGNMENT_TAG).orElse("");
    this.aiPersonaPersonality = compoundTag.getString(DATA_AI_PERSONA_PERSONALITY_TAG).orElse("");
    this.aiPersonaQuirks = compoundTag.getString(DATA_AI_PERSONA_QUIRKS_TAG).orElse("");
    this.aiPersonaBackstory = compoundTag.getString(DATA_AI_PERSONA_BACKSTORY_TAG).orElse("");
    this.aiPersonaGoals = compoundTag.getString(DATA_AI_PERSONA_GOALS_TAG).orElse("");
    this.aiPersonaSpeechStyle = compoundTag.getString(DATA_AI_PERSONA_SPEECH_STYLE_TAG).orElse("");
    this.aiQuestObjective = compoundTag.getString(DATA_AI_QUEST_OBJECTIVE_TAG).orElse("");
    this.aiRequiredCorrect = compoundTag.getInt(DATA_AI_REQUIRED_CORRECT_TAG).orElse(0);

    // Load dialog data
    this.dialogByLabelMap.clear();
    this.dialogByIdMap.clear();
    ListTag dialogListTag = compoundTag.getListOrEmpty(DATA_DIALOG_DATA_SET_TAG);
    for (int i = 0; i < dialogListTag.size(); ++i) {
      CompoundTag dialogCompoundTag = dialogListTag.getCompoundOrEmpty(i);
      DialogDataEntry dialogData = new DialogDataEntry(dialogCompoundTag);
      this.addDialog(dialogData);
    }
  }

  public CompoundTag save(CompoundTag compoundTag) {
    ListTag dialogListTag = new ListTag();
    for (DialogDataEntry dialogData : this.dialogByLabelMap.values()) {
      // Skip empty dialog data
      if (dialogData == null
          || dialogData.getId() == null
          || dialogData.getLabel() == null
          || dialogData.getText().isEmpty()) {
        continue;
      }
      dialogListTag.add(dialogData.createTag());
    }
    compoundTag.put(DATA_DIALOG_DATA_SET_TAG, dialogListTag);

    // Handle dialog type to avoid wrong dialog types after using the dialog editor.
    if (this.dialogType == DialogType.AI) {
      // AI type: preserve it regardless of dialog count, save AI config
      compoundTag.putString(DATA_AI_SERVER_URL_TAG, this.aiServerUrl);
      compoundTag.putString(DATA_AI_MODEL_NAME_TAG, this.aiModelName);
      compoundTag.putString(DATA_AI_SYSTEM_PROMPT_TAG, this.aiSystemPrompt);
      compoundTag.putString(DATA_AI_API_KEY_TAG, this.aiApiKey);
      compoundTag.putInt(DATA_AI_MAX_HISTORY_TAG, this.aiMaxHistorySize);
      compoundTag.putBoolean(DATA_AI_SPEECH_BUBBLE_TAG, this.aiSpeechBubbleEnabled);
      compoundTag.putInt(DATA_AI_SPEECH_BUBBLE_RADIUS_TAG, this.aiSpeechBubbleRadius);
      compoundTag.putInt(DATA_AI_SPEECH_BUBBLE_INTERVAL_TAG, this.aiSpeechBubbleIntervalTicks);
      compoundTag.putString(DATA_AI_PERSONA_NAME_TAG, this.aiPersonaName);
      compoundTag.putString(DATA_AI_PERSONA_RACE_TAG, this.aiPersonaRace);
      compoundTag.putString(DATA_AI_PERSONA_CLASS_TAG, this.aiPersonaClass);
      compoundTag.putString(DATA_AI_PERSONA_ALIGNMENT_TAG, this.aiPersonaAlignment);
      compoundTag.putString(DATA_AI_PERSONA_PERSONALITY_TAG, this.aiPersonaPersonality);
      compoundTag.putString(DATA_AI_PERSONA_QUIRKS_TAG, this.aiPersonaQuirks);
      compoundTag.putString(DATA_AI_PERSONA_BACKSTORY_TAG, this.aiPersonaBackstory);
      compoundTag.putString(DATA_AI_PERSONA_GOALS_TAG, this.aiPersonaGoals);
      compoundTag.putString(DATA_AI_PERSONA_SPEECH_STYLE_TAG, this.aiPersonaSpeechStyle);
      compoundTag.putString(DATA_AI_QUEST_OBJECTIVE_TAG, this.aiQuestObjective);
      compoundTag.putInt(DATA_AI_REQUIRED_CORRECT_TAG, this.aiRequiredCorrect);
    } else if ((this.dialogType == DialogType.BASIC && this.dialogByIdMap.size() > 1)
        || (this.dialogType == DialogType.YES_NO && this.dialogByIdMap.size() > 3)) {
      this.dialogType = DialogType.STANDARD;
    } else if (this.dialogByIdMap.isEmpty()) {
      this.dialogType = DialogType.NONE;
    } else if (this.dialogType != DialogType.BASIC
        && this.dialogType != DialogType.YES_NO
        && this.dialogType != DialogType.STANDARD) {
      this.dialogType = DialogType.CUSTOM;
    }
    compoundTag.putString(DATA_TYPE_TAG, this.dialogType.name());

    return compoundTag;
  }

  public CompoundTag createTag() {
    return this.save(new CompoundTag());
  }

  @Override
  public String toString() {
    return "DialogDataSet [type=" + this.dialogType + ", " + this.dialogByLabelMap + "]";
  }
}
