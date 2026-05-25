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

package de.markusbordihn.easynpc.server.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.datafixers.util.Pair;
import de.markusbordihn.easynpc.commands.Command;
import de.markusbordihn.easynpc.commands.arguments.DialogArgument;
import de.markusbordihn.easynpc.commands.arguments.EasyNPCArgument;
import de.markusbordihn.easynpc.data.action.ActionDataEntry;
import de.markusbordihn.easynpc.data.action.ActionDataSet;
import de.markusbordihn.easynpc.data.action.ActionDataType;
import de.markusbordihn.easynpc.data.dialog.DialogButtonEntry;
import de.markusbordihn.easynpc.data.dialog.DialogButtonType;
import de.markusbordihn.easynpc.data.dialog.DialogDataEntry;
import de.markusbordihn.easynpc.data.dialog.DialogDataSet;
import de.markusbordihn.easynpc.data.dialog.DialogPriority;
import de.markusbordihn.easynpc.data.dialog.DialogType;
import de.markusbordihn.easynpc.entity.easynpc.EasyNPC;
import de.markusbordihn.easynpc.entity.easynpc.data.DialogDataCapable;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public class DialogCommand extends Command {

  private static final String PARENT_LABEL_ARG = "parentLabel";
  private static final String CHILD_LABEL_ARG = "childLabel";
  private static final String BUTTON_TEXT_ARG = "buttonText";
  private static final String LABEL_ARG = "label";
  private static final String TEXT_ARG = "text";
  private static final String SERVER_URL_ARG = "serverUrl";
  private static final String MODEL_ARG = "model";
  private static final String API_KEY_ARG = "apiKey";

  private DialogCommand() {}

  public static ArgumentBuilder<CommandSourceStack, ?> register() {
    return Commands.literal("dialog")
        .then(
            Commands.literal("set")
                .then(
                    Commands.literal("priority")
                        .then(
                            Commands.argument(NPC_TARGET_ARG, EasyNPCArgument.npc())
                                .then(
                                    Commands.argument(DIALOG_ARG, DialogArgument.uuidOrLabel())
                                        .then(
                                            Commands.argument(
                                                    "priority", IntegerArgumentType.integer())
                                                .executes(
                                                    context ->
                                                        setPriority(
                                                            context.getSource(),
                                                            EasyNPCArgument.getEntityWithAccess(
                                                                context, NPC_TARGET_ARG),
                                                            DialogArgument.getUuidOrLabel(
                                                                context, DIALOG_ARG),
                                                            IntegerArgumentType.getInteger(
                                                                context, "priority")))))))
                .then(
                    Commands.literal("ai")
                        .then(
                            Commands.argument(NPC_TARGET_ARG, EasyNPCArgument.npc())
                                .then(
                                    Commands.argument(SERVER_URL_ARG, StringArgumentType.word())
                                        .executes(
                                            context ->
                                                setAIDialog(
                                                    context.getSource(),
                                                    EasyNPCArgument.getEntityWithAccess(
                                                        context, NPC_TARGET_ARG),
                                                    StringArgumentType.getString(
                                                        context, SERVER_URL_ARG),
                                                    "",
                                                    ""))
                                        .then(
                                            Commands.argument(
                                                    MODEL_ARG, StringArgumentType.word())
                                                .executes(
                                                    context ->
                                                        setAIDialog(
                                                            context.getSource(),
                                                            EasyNPCArgument.getEntityWithAccess(
                                                                context, NPC_TARGET_ARG),
                                                            StringArgumentType.getString(
                                                                context, SERVER_URL_ARG),
                                                            StringArgumentType.getString(
                                                                context, MODEL_ARG),
                                                            ""))
                                                .then(
                                                    Commands.argument(
                                                            API_KEY_ARG,
                                                            StringArgumentType.greedyString())
                                                        .executes(
                                                            context ->
                                                                setAIDialog(
                                                                    context.getSource(),
                                                                    EasyNPCArgument
                                                                        .getEntityWithAccess(
                                                                            context, NPC_TARGET_ARG),
                                                                    StringArgumentType.getString(
                                                                        context, SERVER_URL_ARG),
                                                                    StringArgumentType.getString(
                                                                        context, MODEL_ARG),
                                                                    StringArgumentType.getString(
                                                                        context,
                                                                        API_KEY_ARG)))))))))
        .then(
            Commands.literal("open")
                .then(
                    Commands.argument(NPC_TARGET_ARG, EasyNPCArgument.npc())
                        .then(
                            Commands.argument(PLAYER_ARG, EntityArgument.player())
                                .executes(
                                    context ->
                                        openDialog(
                                            context.getSource(),
                                            EasyNPCArgument.getEntityWithAccess(
                                                context, NPC_TARGET_ARG),
                                            EntityArgument.getPlayer(context, PLAYER_ARG)))
                                .then(
                                    Commands.argument(DIALOG_ARG, DialogArgument.uuidOrLabel())
                                        .executes(
                                            context ->
                                                openDialog(
                                                    context.getSource(),
                                                    EasyNPCArgument.getEntityWithAccess(
                                                        context, NPC_TARGET_ARG),
                                                    EntityArgument.getPlayer(context, PLAYER_ARG),
                                                    DialogArgument.getUuidOrLabel(
                                                        context, DIALOG_ARG)))))))
        .then(
            Commands.literal("close")
                .then(
                    Commands.argument(PLAYER_ARG, EntityArgument.player())
                        .executes(
                            context ->
                                closeDialog(
                                    context.getSource(),
                                    EntityArgument.getPlayer(context, PLAYER_ARG)))))
        .then(
            Commands.literal("tree")
                .then(
                    Commands.literal("create")
                        .then(
                            Commands.argument(NPC_TARGET_ARG, EasyNPCArgument.npc())
                                .then(
                                    Commands.argument(LABEL_ARG, StringArgumentType.word())
                                        .then(
                                            Commands.argument(
                                                    TEXT_ARG, StringArgumentType.greedyString())
                                                .executes(
                                                    context ->
                                                        treeCreate(
                                                            context.getSource(),
                                                            EasyNPCArgument.getEntityWithAccess(
                                                                context, NPC_TARGET_ARG),
                                                            StringArgumentType.getString(
                                                                context, LABEL_ARG),
                                                            StringArgumentType.getString(
                                                                context, TEXT_ARG)))))))
                .then(
                    Commands.literal("add")
                        .then(
                            Commands.argument(NPC_TARGET_ARG, EasyNPCArgument.npc())
                                .then(
                                    Commands.argument(PARENT_LABEL_ARG, StringArgumentType.word())
                                        .then(
                                            Commands.argument(
                                                    BUTTON_TEXT_ARG, StringArgumentType.word())
                                                .then(
                                                    Commands.argument(
                                                            CHILD_LABEL_ARG,
                                                            StringArgumentType.word())
                                                        .then(
                                                            Commands.argument(
                                                                    TEXT_ARG,
                                                                    StringArgumentType
                                                                        .greedyString())
                                                                .executes(
                                                                    context ->
                                                                        treeAdd(
                                                                            context.getSource(),
                                                                            EasyNPCArgument
                                                                                .getEntityWithAccess(
                                                                                    context,
                                                                                    NPC_TARGET_ARG),
                                                                            StringArgumentType
                                                                                .getString(
                                                                                    context,
                                                                                    PARENT_LABEL_ARG),
                                                                            StringArgumentType
                                                                                .getString(
                                                                                    context,
                                                                                    BUTTON_TEXT_ARG),
                                                                            StringArgumentType
                                                                                .getString(
                                                                                    context,
                                                                                    CHILD_LABEL_ARG),
                                                                            StringArgumentType
                                                                                .getString(
                                                                                    context,
                                                                                    TEXT_ARG)))))))))
                .then(
                    Commands.literal("link")
                        .then(
                            Commands.argument(NPC_TARGET_ARG, EasyNPCArgument.npc())
                                .then(
                                    Commands.argument(PARENT_LABEL_ARG, StringArgumentType.word())
                                        .then(
                                            Commands.argument(
                                                    CHILD_LABEL_ARG, StringArgumentType.word())
                                                .then(
                                                    Commands.argument(
                                                            BUTTON_TEXT_ARG,
                                                            StringArgumentType.greedyString())
                                                        .executes(
                                                            context ->
                                                                treeLink(
                                                                    context.getSource(),
                                                                    EasyNPCArgument
                                                                        .getEntityWithAccess(
                                                                            context, NPC_TARGET_ARG),
                                                                    StringArgumentType.getString(
                                                                        context, PARENT_LABEL_ARG),
                                                                    StringArgumentType.getString(
                                                                        context, CHILD_LABEL_ARG),
                                                                    StringArgumentType.getString(
                                                                        context,
                                                                        BUTTON_TEXT_ARG))))))))
                .then(
                    Commands.literal("info")
                        .then(
                            Commands.argument(NPC_TARGET_ARG, EasyNPCArgument.npc())
                                .executes(
                                    context ->
                                        treeInfo(
                                            context.getSource(),
                                            EasyNPCArgument.getEntityWithAccess(
                                                context, NPC_TARGET_ARG))))));
  }

  public static int setAIDialog(
      CommandSourceStack context,
      EasyNPC<?> easyNPC,
      String serverUrl,
      String model,
      String apiKey) {
    DialogDataCapable<?> dialogData = easyNPC.getEasyNPCDialogData();
    if (dialogData == null) {
      return sendFailureMessageNoDialogData(context, easyNPC);
    }

    DialogDataSet dataSet = new DialogDataSet(DialogType.AI);
    dataSet.setAIServerUrl(serverUrl);
    dataSet.setAIModelName(model != null ? model : "");
    dataSet.setAIApiKey(apiKey != null ? apiKey : "");
    dialogData.setDialogDataSet(dataSet);

    return sendSuccessMessage(
        context,
        "► Set AI dialog for "
            + easyNPC
            + " | URL: "
            + serverUrl
            + " | model: "
            + (model != null && !model.isEmpty() ? model : "(default)")
            + " | apiKey: "
            + (apiKey != null && !apiKey.isEmpty() ? "***" : "(none)"),
        ChatFormatting.GREEN);
  }

  public static int treeCreate(
      CommandSourceStack context, EasyNPC<?> easyNPC, String label, String text) {
    DialogDataCapable<?> dialogData = easyNPC.getEasyNPCDialogData();
    if (dialogData == null) {
      return sendFailureMessageNoDialogData(context, easyNPC);
    }

    DialogDataSet dataSet = dialogData.getDialogDataSet();
    if (dataSet.hasDialog(label)) {
      return sendFailureMessage(context, "Dialog with label '" + label + "' already exists!");
    }

    DialogDataEntry rootDialog = new DialogDataEntry(label, label, text);
    dataSet.addDialog(rootDialog);
    dialogData.setDialogDataSet(dataSet);

    return sendSuccessMessage(
        context,
        "► Created root dialog '" + label + "' for " + easyNPC,
        ChatFormatting.GREEN);
  }

  public static int treeAdd(
      CommandSourceStack context,
      EasyNPC<?> easyNPC,
      String parentLabel,
      String buttonText,
      String childLabel,
      String childText) {
    DialogDataCapable<?> dialogData = easyNPC.getEasyNPCDialogData();
    if (dialogData == null) {
      return sendFailureMessageNoDialogData(context, easyNPC);
    }

    DialogDataSet dataSet = dialogData.getDialogDataSet();

    if (!dataSet.hasDialog(parentLabel)) {
      return sendFailureMessage(
          context, "Parent dialog '" + parentLabel + "' not found!");
    }
    if (dataSet.hasDialog(childLabel)) {
      return sendFailureMessage(
          context, "Dialog with label '" + childLabel + "' already exists!");
    }

    DialogDataEntry parentDialog = dataSet.getDialog(parentLabel);

    // Create child dialog with parent reference
    DialogDataEntry childDialog = new DialogDataEntry(childLabel, childLabel, childText);
    childDialog.setParentDialogId(parentDialog.getId());
    dataSet.addDialog(childDialog);

    // Add navigation button to parent
    ActionDataSet actionDataSet = new ActionDataSet();
    actionDataSet.add(new ActionDataEntry(ActionDataType.OPEN_NAMED_DIALOG, childLabel));
    DialogButtonEntry button =
        new DialogButtonEntry(buttonText, null, DialogButtonType.DEFAULT, actionDataSet);
    parentDialog.setDialogButton(button.id(), button);

    dialogData.setDialogDataSet(dataSet);

    return sendSuccessMessage(
        context,
        "► Added child dialog '"
            + childLabel
            + "' to '"
            + parentLabel
            + "' with button '"
            + buttonText
            + "'",
        ChatFormatting.GREEN);
  }

  public static int treeLink(
      CommandSourceStack context,
      EasyNPC<?> easyNPC,
      String parentLabel,
      String childLabel,
      String buttonText) {
    DialogDataCapable<?> dialogData = easyNPC.getEasyNPCDialogData();
    if (dialogData == null) {
      return sendFailureMessageNoDialogData(context, easyNPC);
    }

    DialogDataSet dataSet = dialogData.getDialogDataSet();

    if (!dataSet.hasDialog(parentLabel)) {
      return sendFailureMessage(context, "Parent dialog '" + parentLabel + "' not found!");
    }
    if (!dataSet.hasDialog(childLabel)) {
      return sendFailureMessage(context, "Child dialog '" + childLabel + "' not found!");
    }

    DialogDataEntry parentDialog = dataSet.getDialog(parentLabel);
    DialogDataEntry childDialog = dataSet.getDialog(childLabel);

    // Set parent reference if not already set
    if (childDialog.isRoot()) {
      childDialog.setParentDialogId(parentDialog.getId());
    }

    // Add navigation button to parent
    ActionDataSet actionDataSet = new ActionDataSet();
    actionDataSet.add(new ActionDataEntry(ActionDataType.OPEN_NAMED_DIALOG, childLabel));
    DialogButtonEntry button =
        new DialogButtonEntry(buttonText, null, DialogButtonType.DEFAULT, actionDataSet);
    parentDialog.setDialogButton(button.id(), button);

    dialogData.setDialogDataSet(dataSet);

    return sendSuccessMessage(
        context,
        "► Linked dialog '"
            + childLabel
            + "' to '"
            + parentLabel
            + "' with button '"
            + buttonText
            + "'",
        ChatFormatting.GREEN);
  }

  public static int treeInfo(CommandSourceStack context, EasyNPC<?> easyNPC) {
    DialogDataCapable<?> dialogData = easyNPC.getEasyNPCDialogData();
    if (dialogData == null || !dialogData.hasDialog()) {
      return sendFailureMessageNoDialogData(context, easyNPC);
    }

    DialogDataSet dataSet = dialogData.getDialogDataSet();
    sendSuccessMessage(
        context,
        "► Dialog tree for " + easyNPC + ":",
        ChatFormatting.GOLD);

    List<DialogDataEntry> roots = dataSet.getRootDialogs();
    if (roots.isEmpty()) {
      sendSuccessMessage(context, "  (no root dialogs - all dialogs have parents)", ChatFormatting.YELLOW);
    }
    for (DialogDataEntry root : roots) {
      printTreeNode(context, dataSet, root, 0);
    }

    return SINGLE_SUCCESS;
  }

  private static void printTreeNode(
      CommandSourceStack context, DialogDataSet dataSet, DialogDataEntry node, int depth) {
    String indent = "  ".repeat(depth);
    String prefix = depth == 0 ? "■ " : "└─ ";
    sendSuccessMessage(
        context,
        indent + prefix + node.getLabel() + " [" + node.getName() + "] (" + node.getNumberOfDialogButtons() + " btn)",
        depth == 0 ? ChatFormatting.WHITE : ChatFormatting.GRAY);

    List<DialogDataEntry> children = dataSet.getChildren(node.getId());
    for (DialogDataEntry child : children) {
      printTreeNode(context, dataSet, child, depth + 1);
    }
  }

  public static int setPriority(
      CommandSourceStack context, EasyNPC<?> easyNPC, Pair<UUID, String> dialogPair, int priority) {
    if (dialogPair.getFirst() != null) {
      return setPriority(context, easyNPC, dialogPair.getFirst(), priority);
    } else if (dialogPair.getSecond() != null) {
      return setPriority(context, easyNPC, dialogPair.getSecond(), priority);
    }
    return sendFailureMessage(context, "Invalid dialog UUID or label!");
  }

  public static int setPriority(
      CommandSourceStack context, EasyNPC<?> easyNPC, String dialogLabel, int priority) {
    if (!dialogLabel.isEmpty() && !easyNPC.getEasyNPCDialogData().hasDialog(dialogLabel)) {
      return sendFailureMessage(
          context,
          "Found no Dialog with label "
              + dialogLabel
              + " for EasyNPC with UUID "
              + easyNPC.getEntityUUID()
              + "!");
    }
    return setPriority(
        context, easyNPC, easyNPC.getEasyNPCDialogData().getDialogId(dialogLabel), priority);
  }

  public static int setPriority(
      CommandSourceStack context, EasyNPC<?> easyNPC, UUID dialogUUID, int priority) {

    if (easyNPC.getEasyNPCDialogData() == null
        || !easyNPC.getEasyNPCDialogData().hasDialog(dialogUUID)) {
      return sendFailureMessageNoDialogData(context, easyNPC);
    }

    DialogDataSet dialogDataSet = easyNPC.getEasyNPCDialogData().getDialogDataSet();
    DialogDataEntry dialog = dialogDataSet.getDialog(dialogUUID);
    if (dialog != null) {
      dialog.setPriority(priority);
      return sendSuccessMessage(
          context,
          "► Set priority for dialog "
              + dialog.getLabel()
              + " to "
              + priority
              + " ("
              + DialogPriority.getNameForPriority(priority)
              + ")",
          ChatFormatting.GREEN);
    }

    return sendFailureMessage(context, "Dialog not found!");
  }

  public static int openDialog(
      CommandSourceStack context, EasyNPC<?> easyNPC, ServerPlayer serverPlayer) {

    // Verify Player
    if (!serverPlayer.isAlive()) {
      return sendFailureMessage(context, "Player is death!");
    }

    // Verify dialog data
    if (easyNPC.getEasyNPCDialogData() == null || !easyNPC.getEasyNPCDialogData().hasDialog()) {
      return sendFailureMessageNoDialogData(context, easyNPC);
    }

    // Open dialog
    easyNPC.getEasyNPCDialogData().openDefaultDialog(serverPlayer);
    return sendSuccessMessage(
        context, "► Open dialog for " + easyNPC + " with " + serverPlayer, ChatFormatting.GREEN);
  }

  public static int openDialog(
      CommandSourceStack context,
      EasyNPC<?> easyNPC,
      ServerPlayer serverPlayer,
      Pair<UUID, String> dialogPair) {
    if (dialogPair.getFirst() != null) {
      return openDialog(context, easyNPC, serverPlayer, dialogPair.getFirst());
    } else if (dialogPair.getSecond() != null) {
      return openDialog(context, easyNPC, serverPlayer, dialogPair.getSecond());
    }
    return sendFailureMessage(context, "Invalid dialog UUID or label!");
  }

  public static int openDialog(
      CommandSourceStack context,
      EasyNPC<?> easyNPC,
      ServerPlayer serverPlayer,
      String dialogLabel) {

    // Verify dialog label, if any
    if (!dialogLabel.isEmpty() && !easyNPC.getEasyNPCDialogData().hasDialog(dialogLabel)) {
      return sendFailureMessage(
          context,
          "Found no Dialog with label "
              + dialogLabel
              + " for EasyNPC with UUID "
              + easyNPC.getEntityUUID()
              + "!");
    }
    return openDialog(
        context, easyNPC, serverPlayer, easyNPC.getEasyNPCDialogData().getDialogId(dialogLabel));
  }

  public static int openDialog(
      CommandSourceStack context, EasyNPC<?> easyNPC, ServerPlayer serverPlayer, UUID dialogUUID) {

    // Verify Player
    if (!serverPlayer.isAlive()) {
      return sendFailureMessage(context, "Player is death!");
    }

    // Verify dialog data
    if (easyNPC.getEasyNPCDialogData() == null || !easyNPC.getEasyNPCDialogData().hasDialog()) {
      return sendFailureMessageNoDialogData(context, easyNPC);
    }

    // Verify dialog label, if any
    if (!easyNPC.getEasyNPCDialogData().hasDialog(dialogUUID)) {
      return sendFailureMessage(
          context,
          "Found no Dialog with UUID "
              + dialogUUID
              + " for EasyNPC with UUID "
              + easyNPC.getEntityUUID()
              + "!");
    }

    // Open dialog
    easyNPC.getEasyNPCDialogData().openDialog(serverPlayer, dialogUUID);
    return sendSuccessMessage(
        context,
        "► Open dialog for " + easyNPC + " with " + serverPlayer + " and dialog " + dialogUUID,
        ChatFormatting.GREEN);
  }

  public static int closeDialog(CommandSourceStack context, ServerPlayer serverPlayer) {
    // Close dialog screen (client side)
    serverPlayer.closeContainer();

    return sendSuccessMessage(
        context, "► Closed dialog screen for player " + serverPlayer, ChatFormatting.YELLOW);
  }
}
