/*
 * J3 cave tests: CaveNoise is a pure, deterministic 3D field — no Minecraft runtime. We assert
 * that the same (x,y,z,seed,density) always carves identically, that different seeds diverge,
 * that density monotonically increases carved volume, and that some air is actually produced.
 */

package de.markusbordihn.easynpc.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CaveNoiseTest {

  private static int carvedCount(long seed, double density) {
    int n = 0;
    for (int x = 0; x < 32; x++) {
      for (int y = -32; y < 16; y++) {
        for (int z = 0; z < 32; z++) {
          if (CaveNoise.isCarved(x, y, z, seed, density)) {
            n++;
          }
        }
      }
    }
    return n;
  }

  @Test
  void deterministicForSameInputs() {
    for (int i = 0; i < 200; i++) {
      int x = i * 7 - 50;
      int y = i - 64;
      int z = i * 3 - 20;
      boolean a = CaveNoise.isCarved(x, y, z, 42L, 0.2);
      boolean b = CaveNoise.isCarved(x, y, z, 42L, 0.2);
      assertEquals(a, b, "isCarved must be a pure function of its inputs");
      assertEquals(CaveNoise.sample(x, y, z, 42L), CaveNoise.sample(x, y, z, 42L), 0.0);
    }
  }

  @Test
  void sampleInUnitRange() {
    for (int i = 0; i < 500; i++) {
      double v = CaveNoise.sample(i * 13, i - 100, i * 5, 7L);
      assertTrue(v >= 0.0 && v <= 1.0, "sample out of [0,1]: " + v);
    }
  }

  @Test
  void differentSeedsDiverge() {
    assertTrue(carvedCount(1L, 0.2) != carvedCount(2L, 0.2), "distinct seeds should differ");
  }

  @Test
  void higherDensityCarvesMore() {
    int low = carvedCount(42L, 0.1);
    int mid = carvedCount(42L, 0.2);
    int high = carvedCount(42L, 0.4);
    assertTrue(low < mid, "0.2 should carve more than 0.1");
    assertTrue(mid < high, "0.4 should carve more than 0.2");
  }

  @Test
  void zeroDensityNeverCarves() {
    assertEquals(0, carvedCount(42L, 0.0));
    assertFalse(CaveNoise.isCarved(5, -10, 5, 42L, -1.0));
  }

  @Test
  void producesSomeAirAtTypicalDensity() {
    assertTrue(carvedCount(42L, 0.2) > 0, "expected at least one carved block at density 0.2");
  }
}
