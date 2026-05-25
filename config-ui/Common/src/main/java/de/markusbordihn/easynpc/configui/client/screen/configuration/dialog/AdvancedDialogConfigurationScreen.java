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
import de.markusbordihn.easynpc.configui.Constants;
import de.markusbordihn.easynpc.configui.client.screen.components.AddButton;
import de.markusbordihn.easynpc.configui.client.screen.components.CopyButton;
import de.markusbordihn.easynpc.configui.client.screen.components.EditButton;
import de.markusbordihn.easynpc.configui.client.screen.components.TextEditButton;
import de.markusbordihn.easynpc.configui.menu.configuration.ConfigurationMenu;
import de.markusbordihn.easynpc.configui.network.NetworkMessageHandlerManager;
import de.markusbordihn.easynpc.data.dialog.DialogDataEntry;
import de.markusbordihn.easynpc.data.dialog.DialogDataSet;
import de.markusbordihn.easynpc.data.dialog.DialogPriority;
import de.markusbordihn.easynpc.network.components.TextComponent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class AdvancedDialogConfigurationScreen<T extends ConfigurationMenu>
    extends DialogConfigurationScreen<T> {

  private static final int COLUMN_PRIORITY_WIDTH = 25;
  private static final int COLUMN_LABEL_WIDTH = 95;
  private static final int COLUMN_NAME_WIDTH = 96;
  private static final int COLUMN_TEXT_WIDTH = 96;

  private static final int COLUMN_PRIORITY_START = 4;
  private static final int COLUMN_LABEL_START = COLUMN_PRIORITY_START + COLUMN_PRIORITY_WIDTH + 4;
  private static final int COLUMN_NAME_START = COLUMN_LABEL_START + COLUMN_LABEL_WIDTH;
  private static final int COLUMN_TEXT_START = COLUMN_NAME_START + COLUMN_NAME_WIDTH;

  private static final int LIST_AREA_TOP_OFFSET = 20;
  private static final int LIST_AREA_BOTTOM = 190;
  private static final int HEADER_HEIGHT = 18;
  private static final int FOOTER_HEIGHT = 20;

  private static final float TEXT_SCALE = 0.75f;

  Button newDialogButton;
  Button toggleViewButton;

  DialogList dialogList;
  TreeDialogList treeDialogList;

  private boolean treeViewActive = false;

  public AdvancedDialogConfigurationScreen(T menu, Inventory inventory, Component component) {
    super(menu, inventory, component);
  }

  private void toggleView() {
    treeViewActive = !treeViewActive;
    toggleViewButton.setMessage(
        TextComponent.getText(treeViewActive ? "List View" : "Tree View"));

    // Rebuild lists so they reflect current data
    if (treeViewActive) {
      treeDialogList = new TreeDialogList();
    } else {
      dialogList = new DialogList();
    }
  }

  @Override
  public void init() {
    super.init();

    // Default button stats
    this.advancedDialogButton.active = false;

    // Toggle List/Tree view button
    this.toggleViewButton =
        this.addRenderableWidget(
            new TextButton(
                this.contentLeftPos + 4,
                this.contentTopPos - 14,
                64,
                16,
                "Tree View",
                onPress -> toggleView()));

    // Add new dialog button
    this.newDialogButton =
        this.addRenderableWidget(
            new AddButton(
                this.contentLeftPos + 4,
                this.contentTopPos + 193,
                300,
                "dialog.add",
                onPress ->
                    NetworkMessageHandlerManager.getServerHandler()
                        .openDialogEditor(this.getEasyNPCUUID())));

    // Dialog List (default view)
    this.dialogList = new DialogList();
    this.addWidget(this.dialogList);

    // Tree list (built lazily on toggle)
    this.treeDialogList = null;
  }

  @Override
  public void extractRenderState(
      GuiGraphicsExtractor guiGraphics, int x, int y, float partialTicks) {
    super.extractRenderState(guiGraphics, x, y, partialTicks);

    int listLeft = this.leftPos + COLUMN_PRIORITY_START;
    int listRight = this.leftPos + COLUMN_TEXT_START + COLUMN_TEXT_WIDTH + 4;
    int listTop = this.contentTopPos + LIST_AREA_TOP_OFFSET;
    int listBottom = this.contentTopPos + LIST_AREA_BOTTOM + FOOTER_HEIGHT;

    // Gray background for dialog list
    guiGraphics.fill(listLeft, listTop, listRight, listBottom, 0xffeeeeee);

    if (!treeViewActive) {
      renderListView(guiGraphics, x, y, partialTicks, listLeft, listRight, listTop, listBottom);
    } else {
      renderTreeView(guiGraphics, x, y, partialTicks, listLeft, listRight, listTop, listBottom);
    }

    // Header background
    guiGraphics.fill(
        listLeft, this.contentTopPos, listRight, this.contentTopPos + HEADER_HEIGHT, 0xffaaaaaa);

    // Footer background
    guiGraphics.fill(
        listLeft, this.contentTopPos + LIST_AREA_BOTTOM + 1, listRight, listBottom, 0xffc6c6c6);

    // Header labels
    int headerLeft = this.leftPos + COLUMN_PRIORITY_START + 5;
    if (!treeViewActive) {
      Text.drawString(
          guiGraphics,
          this.font,
          "Prio",
          headerLeft,
          this.contentTopPos + 5,
          Constants.FONT_COLOR_BLACK);
      Text.drawConfigString(
          guiGraphics,
          this.font,
          "label_id",
          this.leftPos + COLUMN_LABEL_START + 3,
          this.contentTopPos + 5,
          Constants.FONT_COLOR_BLACK);
      Text.drawString(
          guiGraphics,
          this.font,
          "Name",
          this.leftPos + COLUMN_NAME_START + 3,
          this.contentTopPos + 5,
          Constants.FONT_COLOR_BLACK);
      Text.drawString(
          guiGraphics,
          this.font,
          "Text",
          this.leftPos + COLUMN_TEXT_START + 2,
          this.contentTopPos + 5,
          Constants.FONT_COLOR_BLACK);

      // Vertical separator lines for headers
      guiGraphics.fill(
          this.leftPos + COLUMN_LABEL_START - 1,
          this.contentTopPos,
          this.leftPos + COLUMN_LABEL_START,
          this.contentTopPos + HEADER_HEIGHT,
          0xff666666);
      guiGraphics.fill(
          this.leftPos + COLUMN_NAME_START,
          this.contentTopPos,
          this.leftPos + COLUMN_NAME_START + 1,
          this.contentTopPos + HEADER_HEIGHT,
          0xff666666);
      guiGraphics.fill(
          this.leftPos + COLUMN_TEXT_START - 1,
          this.contentTopPos,
          this.leftPos + COLUMN_TEXT_START,
          this.contentTopPos + HEADER_HEIGHT,
          0xff666666);
    } else {
      Text.drawString(
          guiGraphics,
          this.font,
          "Dialog Tree",
          headerLeft,
          this.contentTopPos + 5,
          Constants.FONT_COLOR_BLACK);
    }

    // Re-render button for visibility
    if (this.newDialogButton != null) {
      this.newDialogButton.extractRenderState(guiGraphics, x, y, partialTicks);
    }
    if (this.toggleViewButton != null) {
      this.toggleViewButton.render(guiGraphics, x, y, partialTicks);
    }
  }

  private void renderListView(
      GuiGraphics guiGraphics,
      int x,
      int y,
      float partialTicks,
      int listLeft,
      int listRight,
      int listTop,
      int listBottom) {
    // Vertical separator lines for entries
    guiGraphics.fill(
        this.leftPos + COLUMN_LABEL_START - 1,
        listTop,
        this.leftPos + COLUMN_LABEL_START,
        this.contentTopPos + LIST_AREA_BOTTOM,
        0xffbbbbbb);
    guiGraphics.fill(
        this.leftPos + COLUMN_NAME_START,
        listTop,
        this.leftPos + COLUMN_NAME_START + 1,
        this.contentTopPos + LIST_AREA_BOTTOM,
        0xffbbbbbb);
    guiGraphics.fill(
        this.leftPos + COLUMN_TEXT_START - 1,
        listTop,
        this.leftPos + COLUMN_TEXT_START,
        this.contentTopPos + LIST_AREA_BOTTOM,
        0xffbbbbbb);

    if (this.dialogList != null) {
      this.dialogList.renderSelectionList(guiGraphics, x, y, partialTicks);
    }
  }

  private void renderTreeView(
      GuiGraphics guiGraphics,
      int x,
      int y,
      float partialTicks,
      int listLeft,
      int listRight,
      int listTop,
      int listBottom) {
    if (this.treeDialogList != null) {
      this.treeDialogList.renderSelectionList(guiGraphics, x, y, partialTicks);
    }
  }

  /** Flat list view (original). */
  class DialogList
      extends ObjectSelectionList<AdvancedDialogConfigurationScreen<?>.DialogList.Entry> {
    DialogList() {
      super(
          AdvancedDialogConfigurationScreen.this.minecraft,
          AdvancedDialogConfigurationScreen.this.width + 60,
          177,
          AdvancedDialogConfigurationScreen.this.contentTopPos + 15,
          19);

      AdvancedDialogConfigurationScreen.this.getDialogDataSet().getDialogsByLabel().stream()
          .filter(dialogData -> dialogData != null && dialogData.getId() != null)
          .sorted(
              Comparator.comparingInt(DialogDataEntry::getPriority)
                  .reversed()
                  .thenComparing(Comparator.comparing(DialogDataEntry::getLabel)))
          .forEach(
              dialogData ->
                  this.addEntry(
                      new AdvancedDialogConfigurationScreen<?>.DialogList.Entry(dialogData)));
    }

    public void renderSelectionList(
        GuiGraphicsExtractor guiGraphics, int x, int y, float partialTicks) {
      if (this.getItemCount() > 0) {
        super.extractRenderState(guiGraphics, x, y, partialTicks);
      }
    }

    @Override
    protected void extractSelection(
        GuiGraphicsExtractor guiGraphics,
        AdvancedDialogConfigurationScreen<?>.DialogList.Entry entry,
        int color) {}

    @Override
    protected void extractListSeparators(GuiGraphicsExtractor guiGraphics) {
      // Do not render list separators.
    }

    @Override
    protected void extractListBackground(GuiGraphicsExtractor guiGraphics) {
      // Do not render list background.
    }

    class Entry
        extends ObjectSelectionList.Entry<AdvancedDialogConfigurationScreen<?>.DialogList.Entry> {

      final DialogDataEntry dialogData;
      final EditButton editButton;
      final CopyButton copyLabelButton;
      final TextEditButton textEditButton;

      public Entry(DialogDataEntry dialogData) {
        super();
        this.dialogData = dialogData;
        this.editButton =
            new EditButton(
                0,
                0,
                onPress ->
                    NetworkMessageHandlerManager.getServerHandler()
                        .openDialogEditor(
                            AdvancedDialogConfigurationScreen.this.getEasyNPCUUID(),
                            this.dialogData.getId()));
        this.copyLabelButton =
            new CopyButton(
                0,
                0,
                onPress -> {
                  Minecraft minecraft = Minecraft.getInstance();
                  minecraft.keyboardHandler.setClipboard(dialogData.getLabel());
                });
        this.textEditButton =
            new TextEditButton(
                0,
                0,
                onPress ->
                    NetworkMessageHandlerManager.getServerHandler()
                        .openDialogTextEditor(
                            AdvancedDialogConfigurationScreen.this.getEasyNPCUUID(),
                            this.dialogData.getId()));
      }

      @Override
      public Component getNarration() {
        return TextComponent.getTextComponent(dialogData.getName());
      }

      @Override
      public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        super.mouseClicked(mouseButtonEvent, doubleClick);
        this.copyLabelButton.mouseClicked(mouseButtonEvent, doubleClick);
        this.editButton.mouseClicked(mouseButtonEvent, doubleClick);
        this.textEditButton.mouseClicked(mouseButtonEvent, doubleClick);
        return mouseButtonEvent.button() == 0;
      }

      @Override
      public void extractContent(
          GuiGraphicsExtractor guiGraphics,
          int mouseX,
          int mouseY,
          boolean isHovered,
          float partialTicks) {

        int top = this.getY();
        int left = this.getX();
        int leftPos = left - 80;
        int buttonWidth = 16;

        this.editButton.setX(leftPos + COLUMN_NAME_START - buttonWidth - 7);
        this.editButton.setY(top);
        this.editButton.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
        if (this.editButton.isHovered()) {
          guiGraphics.setTooltipForNextFrame(
              AdvancedDialogConfigurationScreen.this.font,
              TextComponent.getTranslatedConfigText("dialog.edit_dialog", dialogData.getName()),
              mouseX,
              mouseY);
        }

        this.copyLabelButton.setX(this.editButton.getX() - this.editButton.getWidth());
        this.copyLabelButton.setY(top);
        this.copyLabelButton.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
        if (this.copyLabelButton.isHovered()) {
          guiGraphics.setTooltipForNextFrame(
              AdvancedDialogConfigurationScreen.this.font,
              TextComponent.getTranslatedConfigText(
                  "dialog.copy_dialog_label", dialogData.getLabel()),
              mouseX,
              mouseY);
        }

        this.textEditButton.setX(leftPos + COLUMN_TEXT_START - 5);
        this.textEditButton.setY(top);
        this.textEditButton.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
        if (this.textEditButton.isHovered()) {
          guiGraphics.setTooltipForNextFrame(
              AdvancedDialogConfigurationScreen.this.font,
              TextComponent.getTranslatedConfigText(
                  "dialog.edit_dialog_text", dialogData.getText()),
              mouseX,
              mouseY);
        }

        int dialogDataTopPos = Math.round((top + 5) / TEXT_SCALE);
        int fontColor =
            switch (dialogData.getPriority()) {
              case DialogPriority.CRITICAL -> Constants.FONT_COLOR_RED;
              case DialogPriority.HIGH -> Constants.FONT_COLOR_DARK_GREEN;
              case DialogPriority.NORMAL -> Constants.FONT_COLOR_BLACK;
              case DialogPriority.LOW -> Constants.FONT_COLOR_GRAY;
              case DialogPriority.FALLBACK -> Constants.FONT_COLOR_LIGHT_GRAY;
              case DialogPriority.MANUAL_ONLY -> Constants.FONT_COLOR_GRAY;
              default -> Constants.FONT_COLOR_BLACK;
            };

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().scale(TEXT_SCALE, TEXT_SCALE);
        Text.drawString(
            guiGraphics,
            AdvancedDialogConfigurationScreen.this.font,
            String.valueOf(dialogData.getPriority()),
            Math.round((leftPos + COLUMN_PRIORITY_START - 1) / TEXT_SCALE),
            dialogDataTopPos,
            fontColor);
        Text.drawString(
            guiGraphics,
            AdvancedDialogConfigurationScreen.this.font,
            dialogData.getLabel(14),
            Math.round((leftPos + COLUMN_LABEL_START - 4) / TEXT_SCALE),
            dialogDataTopPos,
            fontColor);
        Text.drawString(
            guiGraphics,
            AdvancedDialogConfigurationScreen.this.font,
            dialogData.getName(21),
            Math.round((leftPos + COLUMN_NAME_START - 2) / TEXT_SCALE),
            dialogDataTopPos,
            fontColor);
        Text.drawString(
            guiGraphics,
            AdvancedDialogConfigurationScreen.this.font,
            dialogData.getText(17),
            Math.round((leftPos + COLUMN_TEXT_START + 14) / TEXT_SCALE),
            dialogDataTopPos,
            fontColor);
        guiGraphics.pose().popMatrix();

        int listLeft = AdvancedDialogConfigurationScreen.this.leftPos + COLUMN_PRIORITY_START;
        int listRight =
            AdvancedDialogConfigurationScreen.this.leftPos
                + COLUMN_TEXT_START
                + COLUMN_TEXT_WIDTH
                + 4;
        guiGraphics.fill(listLeft, top + 17, listRight, top + 18, 0xffaaaaaa);
      }
    }
  }

  /** Tree view - shows dialogs indented by parent-child depth. */
  class TreeDialogList
      extends ObjectSelectionList<AdvancedDialogConfigurationScreen<?>.TreeDialogList.Entry> {

    TreeDialogList() {
      super(
          AdvancedDialogConfigurationScreen.this.minecraft,
          AdvancedDialogConfigurationScreen.this.width + 60,
          177,
          AdvancedDialogConfigurationScreen.this.contentTopPos + 15,
          19);

      DialogDataSet dataSet = AdvancedDialogConfigurationScreen.this.getDialogDataSet();
      List<DialogDataEntry> roots = dataSet.getRootDialogs();

      if (roots.isEmpty()) {
        // Fallback: show all sorted by label when no parent info is set
        dataSet.getDialogsByLabel().stream()
            .filter(d -> d != null && d.getId() != null)
            .forEach(d -> this.addEntry(new Entry(d, 0)));
      } else {
        for (DialogDataEntry root : roots) {
          addTreeEntries(dataSet, root, 0);
        }
      }
    }

    private void addTreeEntries(DialogDataSet dataSet, DialogDataEntry node, int depth) {
      this.addEntry(new Entry(node, depth));
      List<DialogDataEntry> children = new ArrayList<>(dataSet.getChildren(node.getId()));
      children.sort(Comparator.comparing(DialogDataEntry::getLabel));
      for (DialogDataEntry child : children) {
        addTreeEntries(dataSet, child, depth + 1);
      }
    }

    public void renderSelectionList(GuiGraphics guiGraphics, int x, int y, float partialTicks) {
      if (this.getItemCount() > 0) {
        super.render(guiGraphics, x, y, partialTicks);
      }
    }

    @Override
    protected void renderSelection(
        GuiGraphics guiGraphics,
        AdvancedDialogConfigurationScreen<?>.TreeDialogList.Entry entry,
        int color) {}

    @Override
    protected void renderListSeparators(GuiGraphics guiGraphics) {}

    @Override
    protected void renderListBackground(GuiGraphics guiGraphics) {}

    class Entry
        extends ObjectSelectionList.Entry<
            AdvancedDialogConfigurationScreen<?>.TreeDialogList.Entry> {

      final DialogDataEntry dialogData;
      final EditButton editButton;
      final int depth;

      public Entry(DialogDataEntry dialogData, int depth) {
        super();
        this.dialogData = dialogData;
        this.depth = depth;
        this.editButton =
            new EditButton(
                0,
                0,
                onPress ->
                    NetworkMessageHandlerManager.getServerHandler()
                        .openDialogEditor(
                            AdvancedDialogConfigurationScreen.this.getEasyNPCUUID(),
                            this.dialogData.getId()));
      }

      @Override
      public Component getNarration() {
        return TextComponent.getTextComponent(dialogData.getName());
      }

      @Override
      public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean doubleClick) {
        super.mouseClicked(mouseButtonEvent, doubleClick);
        this.editButton.mouseClicked(mouseButtonEvent, doubleClick);
        return mouseButtonEvent.button() == 0;
      }

      @Override
      public void renderContent(
          GuiGraphics guiGraphics, int mouseX, int mouseY, boolean isHovered, float partialTicks) {

        int top = this.getY();
        int left = this.getX();
        int leftPos = left - 80;

        // Edit button at right side
        this.editButton.setX(leftPos + COLUMN_NAME_START - 16 - 7);
        this.editButton.setY(top);
        this.editButton.render(guiGraphics, mouseX, mouseY, partialTicks);
        if (this.editButton.isHovered()) {
          guiGraphics.renderTooltip(
              AdvancedDialogConfigurationScreen.this.font,
              Collections.singletonList(
                  ClientTooltipComponent.create(
                      TextComponent.getTranslatedConfigText(
                              "dialog.edit_dialog", dialogData.getName())
                          .getVisualOrderText())),
              mouseX,
              mouseY,
              DefaultTooltipPositioner.INSTANCE,
              null);
        }

        // Depth indicator: root = "■", children = "└─"
        String depthPrefix = depth == 0 ? "■ " : "  ".repeat(depth) + "└ ";
        String displayText =
            depthPrefix + dialogData.getLabel(12) + " [" + dialogData.getName(10) + "]";
        int fontColor = depth == 0 ? Constants.FONT_COLOR_BLACK : Constants.FONT_COLOR_GRAY;

        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().scale(TEXT_SCALE, TEXT_SCALE);
        Text.drawString(
            guiGraphics,
            AdvancedDialogConfigurationScreen.this.font,
            displayText,
            Math.round((leftPos + COLUMN_PRIORITY_START + 2) / TEXT_SCALE),
            Math.round((top + 5) / TEXT_SCALE),
            fontColor);
        guiGraphics.pose().popMatrix();

        int listLeft = AdvancedDialogConfigurationScreen.this.leftPos + COLUMN_PRIORITY_START;
        int listRight =
            AdvancedDialogConfigurationScreen.this.leftPos
                + COLUMN_TEXT_START
                + COLUMN_TEXT_WIDTH
                + 4;
        guiGraphics.fill(listLeft, top + 17, listRight, top + 18, 0xffaaaaaa);
      }
    }
  }
}
