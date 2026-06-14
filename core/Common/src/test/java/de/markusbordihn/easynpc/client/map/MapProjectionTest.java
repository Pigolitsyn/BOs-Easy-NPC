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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MapProjectionTest {

  private static final double EPS = 1e-6;

  /** World rect [0,100)x[0,100) drawn into screen area at (10,20) sized 200x200. */
  private static MapProjection square() {
    return new MapProjection(0, 0, 100, 100, 10, 20, 200, 200);
  }

  @Test
  @DisplayName("min corner maps to area top-left")
  void minCorner() {
    MapProjection p = square();
    assertEquals(10.0, p.screenX(0), EPS);
    assertEquals(20.0, p.screenY(0), EPS);
  }

  @Test
  @DisplayName("center maps to area center")
  void center() {
    MapProjection p = square();
    assertEquals(110.0, p.screenX(50), EPS); // 10 + 0.5*200
    assertEquals(120.0, p.screenY(50), EPS); // 20 + 0.5*200
  }

  @Test
  @DisplayName("max corner maps to area bottom-right")
  void maxCorner() {
    MapProjection p = square();
    assertEquals(210.0, p.screenX(100), EPS); // 10 + 1.0*200
    assertEquals(220.0, p.screenY(100), EPS); // 20 + 1.0*200
  }

  @Test
  @DisplayName("non-square span scales each axis independently")
  void nonSquareSpan() {
    // span 200 (x) by 50 (z) into a 100x100 area.
    MapProjection p = new MapProjection(0, 0, 200, 50, 0, 0, 100, 100);
    assertEquals(50.0, p.screenX(100), EPS); // half of x span -> half of width
    assertEquals(50.0, p.screenY(25), EPS); // half of z span -> half of height
  }

  @Test
  @DisplayName("negative origin offsets correctly")
  void negativeOrigin() {
    MapProjection p = new MapProjection(-100, -100, 200, 200, 0, 0, 100, 100);
    assertEquals(50.0, p.screenX(0), EPS); // (0 - -100)/200 = 0.5 -> 50
    assertEquals(50.0, p.screenY(0), EPS);
  }

  @Test
  @DisplayName("inBounds is true inside [origin, origin+span) and false outside")
  void inBounds() {
    MapProjection p = square();
    assertTrue(p.inBounds(0, 0));
    assertTrue(p.inBounds(99.9, 99.9));
    assertFalse(p.inBounds(100, 100), "upper edge is exclusive");
    assertFalse(p.inBounds(-1, 50));
    assertFalse(p.inBounds(50, 150));
  }

  @Test
  @DisplayName("clamp pins off-map to nearest area edge")
  void clampToEdge() {
    MapProjection p = square();
    // World x = 200 -> screen 410, clamps to area right edge 210.
    assertEquals(210.0, p.clampX(p.screenX(200)), EPS);
    // World z = -50 -> screen below 20, clamps to area top edge 20.
    assertEquals(20.0, p.clampY(p.screenY(-50)), EPS);
  }

  @Test
  @DisplayName("zero/negative span is clamped to 1 (no divide-by-zero, finite output)")
  void guardsSpan() {
    MapProjection p = new MapProjection(0, 0, 0, -5, 0, 0, 100, 100);
    double x = p.screenX(0);
    double y = p.screenY(0);
    assertTrue(Double.isFinite(x));
    assertTrue(Double.isFinite(y));
  }
}
