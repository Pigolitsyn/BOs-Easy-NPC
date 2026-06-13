/*
 * J2: load a WorldData plan from a --world-data JSON file written by the Rust generator.
 * gson, mirroring the project's existing gson usage (AIDialogHandler). Path is resolved
 * against the server process CWD; the controller passes container paths like /data/loretest.json.
 */

package de.markusbordihn.easynpc.world;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class WorldDataLoader {

  private static final Gson GSON = new Gson();

  private WorldDataLoader() {}

  /** Parse a WorldData plan from a file path. Throws IOException on read/parse failure. */
  public static WorldData load(Path path) throws IOException {
    try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      WorldData data = GSON.fromJson(reader, WorldData.class);
      if (data == null) {
        throw new IOException("world-data is empty or not valid JSON: " + path);
      }
      validate(data, path.toString());
      return data;
    }
  }

  /** Parse a WorldData plan from a JSON string (used by tests). */
  public static WorldData fromJson(String json) {
    WorldData data = GSON.fromJson(json, WorldData.class);
    validate(data, "<string>");
    return data;
  }

  private static void validate(WorldData data, String source) {
    if (data == null) {
      throw new IllegalArgumentException("world-data parsed to null: " + source);
    }
    if (data.width <= 0 || data.depth <= 0) {
      throw new IllegalArgumentException(
          "world-data has non-positive dimensions " + data.width + "x" + data.depth + ": " + source);
    }
    int expected = data.width * data.depth;
    if (data.heightmap == null || data.heightmap.size() != expected) {
      throw new IllegalArgumentException(
          "world-data heightmap size "
              + (data.heightmap == null ? "null" : data.heightmap.size())
              + " != width*depth "
              + expected
              + ": "
              + source);
    }
    if (data.biomeMap == null || data.biomeMap.size() != expected) {
      throw new IllegalArgumentException(
          "world-data biome_map size "
              + (data.biomeMap == null ? "null" : data.biomeMap.size())
              + " != width*depth "
              + expected
              + ": "
              + source);
    }
  }
}
