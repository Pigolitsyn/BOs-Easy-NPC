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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pure-logic tests for the directional locator — no Minecraft classes touched.
 *
 * <p>Fixed MC convention: yaw 0 faces +Z (south). bearing 0 = dead ahead, + = right, - = left.
 */
class LocatorTest {

  private static final double EPS = 0.01;

  @Test
  @DisplayName("target dead ahead (player yaw 0 faces +Z, target at +Z) -> ~0")
  void targetAhead() {
    // Player at origin facing south (+Z); target 10 blocks south.
    assertEquals(0.0, Locator.bearing(0, 0, 0f, 0, 10), EPS);
  }

  @Test
  @DisplayName("target behind (yaw 0 faces +Z, target at -Z) -> ~±180")
  void targetBehind() {
    double b = Locator.bearing(0, 0, 0f, 0, -10);
    assertEquals(180.0, Math.abs(b), EPS);
  }

  @Test
  @DisplayName("target to the right: yaw 0 faces +Z(south), west(-X) is the player's right -> +90")
  void targetRight() {
    // Facing south, west (negative X) is on your right hand -> positive bearing.
    assertEquals(90.0, Locator.bearing(0, 0, 0f, -10, 0), EPS);
  }

  @Test
  @DisplayName("target to the left: yaw 0 faces +Z(south), east(+X) is the player's left -> -90")
  void targetLeft() {
    // Facing south, east (positive X) is on your left hand -> negative bearing.
    assertEquals(-90.0, Locator.bearing(0, 0, 0f, 10, 0), EPS);
  }

  @Test
  @DisplayName("yaw applied: facing the target (yaw points at it) -> ~0")
  void facingTargetViaYaw() {
    // Target due west (-X) of player. Azimuth(-X) = +90; if player yaw is 90 (faces west) -> 0.
    assertEquals(0.0, Locator.bearing(0, 0, 90f, -10, 0), EPS);
    // Target due east (+X). Azimuth(+X) = -90; player yaw -90 (faces east) -> 0.
    assertEquals(0.0, Locator.bearing(0, 0, -90f, 10, 0), EPS);
  }

  @Test
  @DisplayName("bearing always normalized into [-180, 180]")
  void normalizedRange() {
    // A wrap-prone combination: large positive yaw, target behind.
    for (float yaw = -720f; yaw <= 720f; yaw += 37f) {
      double b = Locator.bearing(0, 0, yaw, 5, -7);
      assertTrue(b >= -180.0 && b <= 180.0, "bearing out of range: " + b + " at yaw " + yaw);
    }
  }

  @Test
  @DisplayName("normalize edge cases")
  void normalizeEdges() {
    assertEquals(0.0, Locator.normalize(360.0), EPS);
    assertEquals(0.0, Locator.normalize(-360.0), EPS);
    assertEquals(180.0, Locator.normalize(180.0), EPS);
    assertEquals(-90.0, Locator.normalize(270.0), EPS);
    assertEquals(90.0, Locator.normalize(-270.0), EPS);
    assertEquals(1.0, Locator.normalize(361.0), EPS);
  }

  @Test
  @DisplayName("distance: (0,0)->(3,4) = 5; Y ignored; rounding")
  void distanceBasics() {
    assertEquals(5, Locator.distance(0, 0, 3, 4));
    assertEquals(0, Locator.distance(2, 2, 2, 2));
    assertEquals(10, Locator.distance(0, 0, 0, 10));
    // 7.07 -> 7 (round half-ish)
    assertEquals(7, Locator.distance(0, 0, 5, 5));
  }
}
