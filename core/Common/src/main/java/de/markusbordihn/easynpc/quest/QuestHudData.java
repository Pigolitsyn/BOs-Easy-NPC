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

package de.markusbordihn.easynpc.quest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * HUD quest definition shared between server and client.
 *
 * <p>Frozen JSON contract (the Rust/Python generator writes these exact fields — DO NOT rename):
 *
 * <pre>{@code
 * {"title":str,
 *  "stages":[{"id":int,"title":str,"task_type":str,"objective":"q<code>_sN","npc":[x,y,z]}]}
 * }</pre>
 *
 * <p>{@code done} is NOT part of the JSON contract — it is computed server-side from the scoreboard
 * (see {@link QuestHudRegistry#withStatus}) and carried over the network. JSON-parsed instances
 * default {@code done} to {@code false}.
 */
public record QuestHudData(String title, List<StageInfo> stages) {

  private static final Gson GSON = new Gson();

  /** Codec for the network packet. Plain primitives only — no registry-dependent values. */
  public static final StreamCodec<RegistryFriendlyByteBuf, QuestHudData> STREAM_CODEC =
      StreamCodec.of((buffer, msg) -> msg.write(buffer), QuestHudData::read);

  public record StageInfo(
      int id,
      String title,
      String taskType,
      String objective,
      int npcX,
      int npcY,
      int npcZ,
      boolean done) {

    /** Copy of this stage with the supplied done flag. */
    public StageInfo withDone(boolean newDone) {
      return new StageInfo(id, title, taskType, objective, npcX, npcY, npcZ, newDone);
    }
  }

  /** Copy of this data with the supplied stage list (used by withStatus). */
  public QuestHudData withStages(List<StageInfo> newStages) {
    return new QuestHudData(this.title, newStages);
  }

  // ---- JSON (server-side load from generator file) ----

  /** Parse the frozen HUD JSON contract. done defaults to false (not in the contract). */
  public static QuestHudData fromJson(String json) {
    JsonObject root = GSON.fromJson(json, JsonObject.class);
    if (root == null) {
      throw new IllegalArgumentException("quest-hud json parsed to null");
    }
    String title = root.has("title") ? root.get("title").getAsString() : "";
    List<StageInfo> stages = new ArrayList<>();
    JsonArray stageArray = root.has("stages") ? root.getAsJsonArray("stages") : new JsonArray();
    for (JsonElement element : stageArray) {
      JsonObject stage = element.getAsJsonObject();
      int id = stage.has("id") ? stage.get("id").getAsInt() : 0;
      String stageTitle = stage.has("title") ? stage.get("title").getAsString() : "";
      String taskType = stage.has("task_type") ? stage.get("task_type").getAsString() : "";
      String objective = stage.has("objective") ? stage.get("objective").getAsString() : "";
      int x = 0;
      int y = 0;
      int z = 0;
      if (stage.has("npc")) {
        JsonArray npc = stage.getAsJsonArray("npc");
        if (npc.size() >= 3) {
          x = npc.get(0).getAsInt();
          y = npc.get(1).getAsInt();
          z = npc.get(2).getAsInt();
        }
      }
      stages.add(new StageInfo(id, stageTitle, taskType, objective, x, y, z, false));
    }
    return new QuestHudData(title, stages);
  }

  // ---- Network ----

  public void write(final FriendlyByteBuf buffer) {
    buffer.writeUtf(this.title, 256);
    buffer.writeCollection(
        this.stages,
        (b, stage) -> {
          b.writeVarInt(stage.id());
          b.writeUtf(stage.title(), 256);
          b.writeUtf(stage.taskType(), 64);
          b.writeUtf(stage.objective(), 128);
          b.writeVarInt(stage.npcX());
          b.writeVarInt(stage.npcY());
          b.writeVarInt(stage.npcZ());
          b.writeBoolean(stage.done());
        });
  }

  public static QuestHudData read(final FriendlyByteBuf buffer) {
    String title = buffer.readUtf(256);
    List<StageInfo> stages =
        buffer.readList(
            b ->
                new StageInfo(
                    b.readVarInt(),
                    b.readUtf(256),
                    b.readUtf(64),
                    b.readUtf(128),
                    b.readVarInt(),
                    b.readVarInt(),
                    b.readVarInt(),
                    b.readBoolean()));
    return new QuestHudData(title, stages);
  }
}
