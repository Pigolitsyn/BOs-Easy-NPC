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
 * Mod-rendered map definition (MOD-6, Картограф / Map-It) shared between server and client.
 *
 * <p>The map covers the world rectangle {@code [originX, originX+spanX) × [originZ, originZ+spanZ)}
 * (block coords). It matches the {@code [0,width)} measurement frame of {@code LoreChunkGenerator}
 * (no offset), so it aligns with raw player coordinates. {@link Pin}s and the player marker are
 * world coords projected into the on-screen map area by {@code MapProjection} (client-side).
 *
 * <p>Frozen JSON contract (the Python / QM-4 generator writes these exact fields — DO NOT rename):
 *
 * <pre>{@code
 * {"title":str,
 *  "origin":[ox,oz],
 *  "span":[sx,sz],
 *  "pins":[{"id":int,"label":str,"x":int,"z":int,"kind":str}]}
 * }</pre>
 *
 * <p>Optional fields tolerate absence: {@code title} -> "", missing {@code origin}/{@code span} ->
 * 0 / 1 (1 avoids divide-by-zero in projection), missing {@code pins} -> empty, {@code kind} -> "".
 * Unknown {@code kind} values render with the default marker style (forward-compatible — QM-4 may
 * add kinds without a mod change).
 */
public record MapData(
    String title, int originX, int originZ, int spanX, int spanZ, List<Pin> pins) {

  private static final Gson GSON = new Gson();

  /** Codec for the network packet. Plain primitives only — no registry-dependent values. */
  public static final StreamCodec<RegistryFriendlyByteBuf, MapData> STREAM_CODEC =
      StreamCodec.of((buffer, msg) -> msg.write(buffer), MapData::read);

  /** A single map marker in world block coords (2D top-down map — no y). */
  public record Pin(int id, String label, int x, int z, String kind) {}

  // ---- JSON (server-side load from generator file) ----

  /** Parse the frozen map JSON contract. Tolerant of missing optional fields (see class doc). */
  public static MapData fromJson(String json) {
    JsonObject root = GSON.fromJson(json, JsonObject.class);
    if (root == null) {
      throw new IllegalArgumentException("map json parsed to null");
    }
    String title = root.has("title") ? root.get("title").getAsString() : "";

    int ox = 0;
    int oz = 0;
    if (root.has("origin")) {
      JsonArray origin = root.getAsJsonArray("origin");
      if (origin.size() >= 2) {
        ox = origin.get(0).getAsInt();
        oz = origin.get(1).getAsInt();
      }
    }

    int sx = 1;
    int sz = 1;
    if (root.has("span")) {
      JsonArray span = root.getAsJsonArray("span");
      if (span.size() >= 2) {
        sx = span.get(0).getAsInt();
        sz = span.get(1).getAsInt();
      }
    }
    // Guard against a zero/negative span breaking projection math.
    if (sx <= 0) {
      sx = 1;
    }
    if (sz <= 0) {
      sz = 1;
    }

    List<Pin> pins = new ArrayList<>();
    JsonArray pinArray = root.has("pins") ? root.getAsJsonArray("pins") : new JsonArray();
    for (JsonElement element : pinArray) {
      JsonObject pin = element.getAsJsonObject();
      int id = pin.has("id") ? pin.get("id").getAsInt() : 0;
      String label = pin.has("label") ? pin.get("label").getAsString() : "";
      int x = pin.has("x") ? pin.get("x").getAsInt() : 0;
      int z = pin.has("z") ? pin.get("z").getAsInt() : 0;
      String kind = pin.has("kind") ? pin.get("kind").getAsString() : "";
      pins.add(new Pin(id, label, x, z, kind));
    }
    return new MapData(title, ox, oz, sx, sz, pins);
  }

  // ---- Network ----

  public void write(final FriendlyByteBuf buffer) {
    buffer.writeUtf(this.title, 256);
    buffer.writeVarInt(this.originX);
    buffer.writeVarInt(this.originZ);
    buffer.writeVarInt(this.spanX);
    buffer.writeVarInt(this.spanZ);
    buffer.writeCollection(
        this.pins,
        (b, pin) -> {
          b.writeVarInt(pin.id());
          b.writeUtf(pin.label(), 256);
          b.writeVarInt(pin.x());
          b.writeVarInt(pin.z());
          b.writeUtf(pin.kind(), 64);
        });
  }

  public static MapData read(final FriendlyByteBuf buffer) {
    String title = buffer.readUtf(256);
    int ox = buffer.readVarInt();
    int oz = buffer.readVarInt();
    int sx = buffer.readVarInt();
    int sz = buffer.readVarInt();
    List<Pin> pins =
        buffer.readList(
            b -> new Pin(b.readVarInt(), b.readUtf(256), b.readVarInt(), b.readVarInt(),
                b.readUtf(64)));
    return new MapData(title, ox, oz, sx, sz, pins);
  }
}
