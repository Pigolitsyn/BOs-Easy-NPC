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

package de.markusbordihn.easynpc.client.map;

/**
 * Pure world-to-screen projection for the mod-rendered map (MOD-6). MC-free and unit-tested.
 *
 * <p>The map covers the world rectangle {@code [originX, originX+spanX) × [originZ, originZ+spanZ)}
 * (block coords) and is drawn into the on-screen map area {@code (areaLeft, areaTop, areaW, areaH)}.
 * North-up, no rotation: world {@code +X} -> screen right, world {@code +Z} -> screen down. Each axis
 * is scaled independently so a non-square span fills the area. {@code spanX}/{@code spanZ} are
 * assumed positive (the JSON parser guarantees this).
 */
public final class MapProjection {

  private final int originX;
  private final int originZ;
  private final int spanX;
  private final int spanZ;
  private final int areaLeft;
  private final int areaTop;
  private final int areaW;
  private final int areaH;

  public MapProjection(
      int originX, int originZ, int spanX, int spanZ,
      int areaLeft, int areaTop, int areaW, int areaH) {
    this.originX = originX;
    this.originZ = originZ;
    this.spanX = Math.max(1, spanX);
    this.spanZ = Math.max(1, spanZ);
    this.areaLeft = areaLeft;
    this.areaTop = areaTop;
    this.areaW = areaW;
    this.areaH = areaH;
  }

  /** Screen X (pixels) for a world X (block). Not clamped. */
  public double screenX(double worldX) {
    double nx = (worldX - originX) / (double) spanX;
    return areaLeft + nx * areaW;
  }

  /** Screen Y (pixels) for a world Z (block). Not clamped (world +Z = screen down). */
  public double screenY(double worldZ) {
    double nz = (worldZ - originZ) / (double) spanZ;
    return areaTop + nz * areaH;
  }

  /** Whether a world point falls inside the mapped rectangle {@code [origin, origin+span)}. */
  public boolean inBounds(double worldX, double worldZ) {
    return worldX >= originX
        && worldX < (double) originX + spanX
        && worldZ >= originZ
        && worldZ < (double) originZ + spanZ;
  }

  /** Clamp a screen X to the map area so off-map points render at the nearest edge. */
  public double clampX(double sx) {
    return clamp(sx, areaLeft, (double) areaLeft + areaW);
  }

  /** Clamp a screen Y to the map area so off-map points render at the nearest edge. */
  public double clampY(double sy) {
    return clamp(sy, areaTop, (double) areaTop + areaH);
  }

  private static double clamp(double value, double min, double max) {
    if (value < min) {
      return min;
    }
    return Math.min(value, max);
  }
}
