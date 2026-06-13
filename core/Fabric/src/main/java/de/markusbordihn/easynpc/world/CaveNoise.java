/*
 * J3: deterministic 3D value-noise cave carving. Pure functions only — no Minecraft
 * runtime, no external noise libs (the Rust side has its own; the Java side just needs a
 * stable, seed-driven 3D field to decide air vs solid below the surface). This makes the
 * carve decision a chunk-position-independent function of (worldX, worldY, worldZ), so a
 * block carved when generating chunk A is carved identically when its neighbour regenerates.
 *
 * Algorithm: trilinearly-interpolated integer-lattice value noise at two octaves, hashed
 * from the cave seed. Output is in [0,1]. {@link #isCarved} carves where the field exceeds a
 * threshold derived from density (higher density -> lower threshold -> more air).
 */

package de.markusbordihn.easynpc.world;

public final class CaveNoise {

  // Lattice cell size in blocks. Larger -> bigger, smoother caverns.
  private static final double SCALE = 14.0;
  // Second octave is finer and contributes less; adds tunnel-like irregularity.
  private static final double SCALE2 = 6.0;
  private static final double OCTAVE2_WEIGHT = 0.35;

  private CaveNoise() {}

  /**
   * Deterministic carve test for a world block. Same (x,y,z,seed,density) always yields the
   * same boolean. {@code density} is the WorldData cave density (typically 0.1..0.4); higher
   * density carves more. density <= 0 never carves.
   */
  public static boolean isCarved(int x, int y, int z, long seed, double density) {
    if (density <= 0.0) {
      return false;
    }
    double n = sample(x, y, z, seed);
    // density 0.1 -> threshold ~0.78 (sparse), density 0.4 -> threshold ~0.6 (roomy).
    double threshold = 0.82 - clamp(density, 0.0, 0.6);
    return n > threshold;
  }

  /** Normalized noise value in [0,1] for a world block. Exposed for tests. */
  public static double sample(int x, int y, int z, long seed) {
    double base = valueNoise(x / SCALE, y / SCALE, z / SCALE, seed);
    double fine = valueNoise(x / SCALE2, y / SCALE2, z / SCALE2, seed ^ 0x9E3779B97F4A7C15L);
    return (base + OCTAVE2_WEIGHT * fine) / (1.0 + OCTAVE2_WEIGHT);
  }

  /** Trilinear value noise on the integer lattice; result in [0,1]. */
  private static double valueNoise(double fx, double fy, double fz, long seed) {
    int x0 = floor(fx);
    int y0 = floor(fy);
    int z0 = floor(fz);
    double tx = smooth(fx - x0);
    double ty = smooth(fy - y0);
    double tz = smooth(fz - z0);

    double c000 = lattice(x0, y0, z0, seed);
    double c100 = lattice(x0 + 1, y0, z0, seed);
    double c010 = lattice(x0, y0 + 1, z0, seed);
    double c110 = lattice(x0 + 1, y0 + 1, z0, seed);
    double c001 = lattice(x0, y0, z0 + 1, seed);
    double c101 = lattice(x0 + 1, y0, z0 + 1, seed);
    double c011 = lattice(x0, y0 + 1, z0 + 1, seed);
    double c111 = lattice(x0 + 1, y0 + 1, z0 + 1, seed);

    double x00 = lerp(c000, c100, tx);
    double x10 = lerp(c010, c110, tx);
    double x01 = lerp(c001, c101, tx);
    double x11 = lerp(c011, c111, tx);
    double y0v = lerp(x00, x10, ty);
    double y1v = lerp(x01, x11, ty);
    return lerp(y0v, y1v, tz);
  }

  /** Hash an integer lattice point to a deterministic value in [0,1]. */
  private static double lattice(int x, int y, int z, long seed) {
    long h = seed;
    h = mix(h + 0x9E3779B97F4A7C15L * x);
    h = mix(h + 0xC2B2AE3D27D4EB4FL * y);
    h = mix(h + 0x165667B19E3779F9L * z);
    // Top 53 bits -> [0,1).
    return (h >>> 11) * 0x1.0p-53;
  }

  private static long mix(long z) {
    z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
    z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
    return z ^ (z >>> 31);
  }

  private static double smooth(double t) {
    // 6t^5 - 15t^4 + 10t^3 (smootherstep) for C2 continuity between cells.
    return t * t * t * (t * (t * 6 - 15) + 10);
  }

  private static double lerp(double a, double b, double t) {
    return a + (b - a) * t;
  }

  private static int floor(double v) {
    int i = (int) v;
    return (v < i) ? i - 1 : i;
  }

  private static double clamp(double v, double lo, double hi) {
    return v < lo ? lo : (v > hi ? hi : v);
  }
}
