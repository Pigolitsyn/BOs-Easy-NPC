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

package de.markusbordihn.easynpc.network.message.server;

import de.markusbordihn.easynpc.Constants;
import de.markusbordihn.easynpc.data.dialog.DialogDataSet;
import de.markusbordihn.easynpc.data.dialog.DialogType;
import de.markusbordihn.easynpc.entity.LivingEntityManager;
import de.markusbordihn.easynpc.entity.easynpc.EasyNPC;
import de.markusbordihn.easynpc.entity.easynpc.data.DialogDataCapable;
import de.markusbordihn.easynpc.network.message.NetworkMessageRecord;
import de.markusbordihn.easynpc.server.ai.AIDialogHandler;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public record SendAIMessageMessage(UUID npcUUID, String message) implements NetworkMessageRecord {

  public static final Identifier MESSAGE_ID =
      Identifier.fromNamespaceAndPath(Constants.MOD_ID, "send_ai_message");
  public static final Type<SendAIMessageMessage> PAYLOAD_TYPE = new Type<>(MESSAGE_ID);
  public static final StreamCodec<RegistryFriendlyByteBuf, SendAIMessageMessage> STREAM_CODEC =
      StreamCodec.of((buffer, msg) -> msg.write(buffer), SendAIMessageMessage::create);

  public static SendAIMessageMessage create(final FriendlyByteBuf buffer) {
    return new SendAIMessageMessage(buffer.readUUID(), buffer.readUtf(1024));
  }

  @Override
  public void write(final FriendlyByteBuf buffer) {
    buffer.writeUUID(this.npcUUID);
    buffer.writeUtf(this.message, 1024);
  }

  @Override
  public Identifier id() {
    return MESSAGE_ID;
  }

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return PAYLOAD_TYPE;
  }

  @Override
  public void handleServer(final ServerPlayer serverPlayer) {
    if (this.message == null || this.message.isBlank()) {
      return;
    }

    EasyNPC<?> easyNPC = LivingEntityManager.getEasyNPCEntityByUUID(this.npcUUID, serverPlayer);
    if (easyNPC == null) {
      return;
    }

    DialogDataCapable<?> dialogDataCapable = easyNPC.getEasyNPCDialogData();
    if (dialogDataCapable == null) {
      return;
    }

    DialogDataSet dialogDataSet = dialogDataCapable.getDialogDataSet();
    if (dialogDataSet == null || dialogDataSet.getType() != DialogType.AI) {
      return;
    }

    AIDialogHandler.processMessage(
        this.npcUUID,
        serverPlayer,
        this.message,
        dialogDataSet.getAIServerUrl(),
        dialogDataSet.getAIModelName(),
        dialogDataSet.compileEffectiveSystemPrompt(),
        dialogDataSet.getAIApiKey(),
        dialogDataSet.getAIQuestObjective(),
        dialogDataSet.getAIRequiredCorrect());
  }
}
