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

package de.markusbordihn.easynpc.client.hud;

/**
 * Pure (MC-free) directional-locator math for the quest HUD (US navigation, Task 4). Kept free of
 * Minecraft classes so it is unit-testable with plain JUnit.
 *
 * <p>Minecraft yaw/coordinate convention (player.getYRot()):
 *
 * <pre>
 *   yaw   0 = south = +Z
 *   yaw  90 = west  = -X
 *   yaw 180 = north = -Z
 *   yaw 270 = east  = +X   (also reported as -90)
 *   yaw grows clockwise when viewed from above.
 * </pre>
 *
 * <p>Target azimuth (in the same convention as yaw) is {@code atan2(-dx, dz)}, which yields 0 for a
 * target straight along +Z. The relative {@link #bearing} is {@code normalize(targetAzimuth -
 * playerYaw)} into [-180, 180]: 0 = dead ahead, positive = to the player's right, negative = to the
 * player's left.
 */
public final class Locator {

  private Locator() {}

  /**
   * Relative angle from the player's facing to the target, in degrees, normalized to [-180, 180].
   * 0 = straight ahead, + = to the right, - = to the left. Y is ignored (horizontal bearing).
   *
   * @param px player X
   * @param pz player Z
   * @param playerYawDeg player yaw in degrees (MC convention; player.getYRot())
   * @param tx target X
   * @param tz target Z
   */
  public static double bearing(
      final double px,
      final double pz,
      final float playerYawDeg,
      final double tx,
      final double tz) {
    double dx = tx - px;
    double dz = tz - pz;
    double targetAzimuth = Math.toDegrees(Math.atan2(-dx, dz));
    return normalize(targetAzimuth - playerYawDeg);
  }

  /** Horizontal (X/Z, no Y) distance, rounded to the nearest block. */
  public static int distance(
      final double px, final double pz, final double tx, final double tz) {
    double dx = tx - px;
    double dz = tz - pz;
    return (int) Math.round(Math.sqrt(dx * dx + dz * dz));
  }

  /** Normalize an angle in degrees into the half-open-ish range [-180, 180]. */
  public static double normalize(final double angleDeg) {
    double a = angleDeg % 360.0;
    if (a > 180.0) {
      a -= 360.0;
    } else if (a < -180.0) {
      a += 360.0;
    }
    return a;
  }
}
