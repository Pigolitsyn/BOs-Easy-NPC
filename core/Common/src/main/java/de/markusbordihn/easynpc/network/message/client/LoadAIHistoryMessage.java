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

package de.markusbordihn.easynpc.network.message.client;

import de.markusbordihn.easynpc.Constants;
import de.markusbordihn.easynpc.client.screen.dialog.AIChatDialogScreen;
import de.markusbordihn.easynpc.network.message.NetworkMessageRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record LoadAIHistoryMessage(UUID npcUUID, List<String> roles, List<String> contents)
    implements NetworkMessageRecord {

  public static final Identifier MESSAGE_ID =
      Identifier.fromNamespaceAndPath(Constants.MOD_ID, "load_ai_history");
  public static final Type<LoadAIHistoryMessage> PAYLOAD_TYPE = new Type<>(MESSAGE_ID);
  public static final StreamCodec<RegistryFriendlyByteBuf, LoadAIHistoryMessage> STREAM_CODEC =
      StreamCodec.of((buffer, msg) -> msg.write(buffer), LoadAIHistoryMessage::create);

  public static LoadAIHistoryMessage create(final FriendlyByteBuf buffer) {
    UUID id = buffer.readUUID();
    int n = buffer.readVarInt();
    List<String> roles = new ArrayList<>(n);
    List<String> contents = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      roles.add(buffer.readUtf(32));
      contents.add(buffer.readUtf(4096));
    }
    return new LoadAIHistoryMessage(id, roles, contents);
  }

  @Override
  public void write(final FriendlyByteBuf buffer) {
    buffer.writeUUID(this.npcUUID);
    int n = Math.min(this.roles.size(), this.contents.size());
    buffer.writeVarInt(n);
    for (int i = 0; i < n; i++) {
      buffer.writeUtf(this.roles.get(i), 32);
      buffer.writeUtf(this.contents.get(i), 4096);
    }
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
  public void handleClient() {
    Minecraft minecraft = Minecraft.getInstance();
    if (minecraft.screen instanceof AIChatDialogScreen<?> chatScreen) {
      chatScreen.loadHistory(this.roles, this.contents);
    }
  }
}
