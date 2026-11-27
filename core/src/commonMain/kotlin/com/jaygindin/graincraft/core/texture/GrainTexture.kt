package com.jaygindin.graincraft.core.texture

import com.jaygindin.graincraft.core.layout.Fibre
import com.jaygindin.graincraft.core.wood.PoreType
import com.jaygindin.graincraft.core.wood.WoodTones
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * What a texture looks like: the species' tones, its pore type, the fibre direction on the face, a
 * seed (see [textureSeed]), whether it is flipped left to right, and the scale in pixels per inch.
 */
data class TextureSpec(
  val tones: WoodTones,
  val poreType: PoreType,
  val fibre: Fibre,
  val seed: Int,
  val mirrored: Boolean,
  val pixelsPerInch: Float,
)

/**
 * A seed shared by every cell cut from the same strip, so their ring patterns match. The species id
 * is hashed by hand (FNV-1a over UTF-16 units) so the value cannot depend on a platform's
 * `String.hashCode`.
 */
fun textureSeed(speciesId: String, sourceStripIndex: Int, laminationIndex: Int): Int {
  var h = FNV_OFFSET
  for (c in speciesId) h = (h xor c.code) * FNV_PRIME
  h = mix(h xor (sourceStripIndex * GOLDEN))
  return mix(h xor (laminationIndex * SECOND_PRIME + 1))
}

/** Generates ARGB pixels (row-major) that look like end grain or long grain. */
object GrainTexture {
  fun render(spec: TextureSpec, widthPx: Int, heightPx: Int): IntArray {
    val out = IntArray(widthPx * heightPx)
    val ppi = spec.pixelsPerInch.toDouble()
    val seed = spec.seed
    // Dark woods get a slightly lighter earlywood, so their rings still show when the latewood can
    // only darken a little further; pale woods are left as they are.
    val raw = spec.tones.base.argb
    val darkness =
      1.0 -
        (LUMA_R * channel(raw, RED_SHIFT) +
          LUMA_G * channel(raw, GREEN_SHIFT) +
          LUMA_B * channel(raw, 0)) / MAX_CHANNEL
    val base = lerp(raw, WHITE, EARLY_LIFT * darkness * darkness)
    val late = spec.tones.latewood.argb
    val pore = spec.tones.pore.argb
    val widthIn = widthPx / ppi
    val heightIn = heightPx / ppi
    val rng = Rng(mix(seed xor RING_SALT))

    // Pith: 3/4" to 2-1/2" beyond half the piece's longer side, so it sits outside the piece as
    // in real strip stock yet close enough for the rings to curve visibly, in a direction at least
    // 25 degrees off both axes so rings never run exactly along a pixel row or column.
    val quadrant = rng.nextInt(QUADRANTS)
    val angle =
      (quadrant * QUADRANT_DEGREES + PITH_MIN_OFF_AXIS + rng.nextDouble() * PITH_ANGLE_SPAN) * PI /
        HALF_TURN_DEGREES
    val pithDistance =
      maxOf(widthIn, heightIn) / 2 + PITH_MIN_DISTANCE + rng.nextDouble() * PITH_DISTANCE_SPAN
    val pithX = widthIn / 2 + cos(angle) * pithDistance
    val pithY = heightIn / 2 + sin(angle) * pithDistance
    val ringsPerInch = MIN_RINGS_PER_INCH + rng.nextDouble() * RINGS_PER_INCH_SPAN
    val lineAngle = rng.nextDouble()

    for (y in 0 until heightPx) {
      val yIn = (y + HALF) / ppi
      for (x in 0 until widthPx) {
        val sx = if (spec.mirrored) widthPx - 1 - x else x
        val xIn = (sx + HALF) / ppi
        out[y * widthPx + x] =
          if (spec.fibre == Fibre.Z) {
            endGrain(seed, xIn - pithX, yIn - pithY, xIn, yIn, ringsPerInch, spec, base, late, pore)
          } else {
            longGrain(seed, xIn, yIn, lineAngle, base, late)
          }
      }
    }
    return out
  }

  private fun endGrain(
    seed: Int,
    dx: Double,
    dy: Double,
    xIn: Double,
    yIn: Double,
    ringsPerInch: Double,
    spec: TextureSpec,
    base: Int,
    late: Int,
    pore: Int,
  ): Int {
    // Noise warps the spacing so rings are not perfect circles.
    val warp =
      (noise(seed + 1, xIn * WARP_FREQ, yIn * WARP_FREQ) - HALF) * WARP_AMOUNT +
        (noise(seed + 2, xIn * RIPPLE_FREQ, yIn * RIPPLE_FREQ) - HALF) * RIPPLE_AMOUNT
    val distance = sqrt(dx * dx + dy * dy) + warp
    val phase = distance * ringsPerInch
    val ring = floor(phase)
    val f = phase - ring
    val strength = MIN_RING_STRENGTH + RING_STRENGTH_SPAN * hash01(seed, ring.toInt(), RING_SALT_B)
    // Earlywood is pale; latewood darkens quickly across a narrow band at the back of the ring and
    // stops abruptly at the next ring, so each ring reads as a crisp dark arc.
    val t = ((f - LATEWOOD_START) / LATEWOOD_RAMP).coerceIn(0.0, 1.0)
    val lateness = smooth(t) * strength
    var colour = lerp(base, late, lateness)

    val density = poreDensity(spec.poreType, f)
    if (density > 0.0) {
      val cell = noise(seed + SALT_PORE, xIn * PORE_FREQ, yIn * PORE_FREQ + ring * PORE_RING_SHIFT)
      if (cell > 1.0 - density) colour = lerp(colour, pore, PORE_BLEND)
    }
    return shade(colour, noise(seed + SALT_SPECKLE, xIn * SPECKLE_FREQ, yIn * SPECKLE_FREQ))
  }

  private fun poreDensity(type: PoreType, f: Double): Double =
    when (type) {
      PoreType.RING_OPEN -> if (f < OPEN_EARLY_ZONE) OPEN_EARLY_DENSITY else OPEN_LATE_DENSITY
      PoreType.SEMI_RING -> if (f < SEMI_EARLY_ZONE) SEMI_EARLY_DENSITY else SEMI_LATE_DENSITY
      PoreType.DIFFUSE,
      PoreType.RING_CLOSED -> 0.0
    }

  private fun longGrain(
    seed: Int,
    xIn: Double,
    yIn: Double,
    lineAngle: Double,
    base: Int,
    late: Int,
  ): Int {
    // Lines run along X; slow waviness shifts them up and down, and they drift a little with x.
    val wave = (noise(seed + SALT_WAVE, xIn * WAVE_X_FREQ, yIn * WAVE_Y_FREQ) - HALF) * WAVE_AMOUNT
    val yy = yIn + wave + xIn * DRIFT * (lineAngle - HALF)
    val broad = noise(seed + SALT_BROAD, xIn * BROAD_X, yy * BROAD_Y)
    val fine = noise(seed + SALT_FINE, xIn * FINE_X, yy * FINE_Y)
    val streak = noise(seed + SALT_STREAK, xIn * STREAK_X, yy * STREAK_Y)
    val mixed = BROAD_WEIGHT * broad + FINE_WEIGHT * fine + STREAK_WEIGHT * streak
    val lateness = (mixed - LINE_BIAS).coerceIn(0.0, 1.0) * LINE_STRENGTH
    return shade(lerp(base, late, lateness), noise(seed + SALT_GRAIN, xIn * GRAIN_X, yy * GRAIN_Y))
  }

  /** Brightness jitter of up to about plus or minus 3 percent. */
  private fun shade(argb: Int, n: Double): Int {
    val k = 1.0 + (n - HALF) * SHADE_SPREAD
    return pack(
      (channel(argb, RED_SHIFT) * k).toInt().coerceIn(0, MAX_CHANNEL),
      (channel(argb, GREEN_SHIFT) * k).toInt().coerceIn(0, MAX_CHANNEL),
      (channel(argb, 0) * k).toInt().coerceIn(0, MAX_CHANNEL),
    )
  }

  private fun lerp(a: Int, b: Int, t: Double): Int {
    fun one(shift: Int): Int {
      val from = channel(a, shift)
      return (from + (channel(b, shift) - from) * t + HALF).toInt().coerceIn(0, MAX_CHANNEL)
    }
    return pack(one(RED_SHIFT), one(GREEN_SHIFT), one(0))
  }

  private fun channel(argb: Int, shift: Int): Int = (argb shr shift) and MAX_CHANNEL

  private fun pack(r: Int, g: Int, b: Int): Int =
    (OPAQUE shl ALPHA_SHIFT) or (r shl RED_SHIFT) or (g shl GREEN_SHIFT) or b

  /** Smooth value noise in 0..1 with a lattice spacing of one unit. */
  private fun noise(seed: Int, x: Double, y: Double): Double {
    val x0 = floor(x)
    val y0 = floor(y)
    val fx = smooth(x - x0)
    val fy = smooth(y - y0)
    val ix = x0.toInt()
    val iy = y0.toInt()
    val top = hash01(seed, ix, iy) * (1 - fx) + hash01(seed, ix + 1, iy) * fx
    val bottom = hash01(seed, ix, iy + 1) * (1 - fx) + hash01(seed, ix + 1, iy + 1) * fx
    return top * (1 - fy) + bottom * fy
  }

  private fun smooth(t: Double): Double = t * t * (3 - 2 * t)

  private fun hash01(seed: Int, x: Int, y: Int): Double {
    val h = mix(seed xor (x * HASH_X) xor (y * HASH_Y))
    return (h ushr HASH_DROP) / HASH_RANGE
  }

  /** A tiny xorshift generator, so output is the same on every platform. */
  private class Rng(seed: Int) {
    private var state = if (seed == 0) 1 else seed

    fun next(): Int {
      var s = state
      s = s xor (s shl XORSHIFT_A)
      s = s xor (s ushr XORSHIFT_B)
      s = s xor (s shl XORSHIFT_C)
      state = s
      return s
    }

    fun nextInt(bound: Int): Int = (next() ushr 1) % bound

    fun nextDouble(): Double = (next() ushr HASH_DROP) / HASH_RANGE
  }

  // Pith placement: distance from the piece centre and the angle kept off both axes.
  private const val QUADRANTS = 4
  private const val QUADRANT_DEGREES = 90.0
  private const val HALF_TURN_DEGREES = 180.0
  private const val PITH_MIN_OFF_AXIS = 25.0
  private const val PITH_ANGLE_SPAN = 40.0
  private const val PITH_MIN_DISTANCE = 0.75
  private const val PITH_DISTANCE_SPAN = 1.75

  // Ring density per inch of radius: wide enough that each ring is a clear arc at a whole-board
  // view, about 50 pixels per inch.
  private const val MIN_RINGS_PER_INCH = 2.5
  private const val RINGS_PER_INCH_SPAN = 2.5

  // Ring-spacing warp: a broad wobble plus a fine ripple, in inches.
  private const val WARP_FREQ = 1.3
  private const val WARP_AMOUNT = 0.16
  private const val RIPPLE_FREQ = 5.0
  private const val RIPPLE_AMOUNT = 0.03

  // Ring colour: per-ring strength range, and where latewood begins within a ring.
  private const val MIN_RING_STRENGTH = 0.8
  private const val RING_STRENGTH_SPAN = 0.2
  private const val RING_SALT_B = 7
  private const val LATEWOOD_START = 0.68
  private const val LATEWOOD_RAMP = 0.14

  // Earlywood lift toward white for dark bases, scaled by the square of their darkness.
  private const val EARLY_LIFT = 0.22
  private const val LUMA_R = 0.299
  private const val LUMA_G = 0.587
  private const val LUMA_B = 0.114
  private const val WHITE = 0xFFFFFF

  // Pores: noise scale, blend into the pore tone, and density by pore type and ring position.
  private const val PORE_FREQ = 38.0
  private const val PORE_RING_SHIFT = 3.7
  private const val PORE_BLEND = 0.55
  private const val OPEN_EARLY_ZONE = 0.3
  private const val OPEN_EARLY_DENSITY = 0.3
  private const val OPEN_LATE_DENSITY = 0.04
  private const val SEMI_EARLY_ZONE = 0.5
  private const val SEMI_EARLY_DENSITY = 0.18
  private const val SEMI_LATE_DENSITY = 0.05

  // Long grain: waviness, drift, three line scales with their weights, and the tone strength.
  private const val WAVE_X_FREQ = 0.35
  private const val WAVE_Y_FREQ = 0.5
  private const val WAVE_AMOUNT = 0.35
  private const val DRIFT = 0.01
  private const val BROAD_X = 0.25
  private const val BROAD_Y = 9.0
  private const val FINE_X = 0.6
  private const val FINE_Y = 55.0
  private const val STREAK_X = 1.5
  private const val STREAK_Y = 22.0
  private const val BROAD_WEIGHT = 0.55
  private const val FINE_WEIGHT = 0.3
  private const val STREAK_WEIGHT = 0.15
  private const val LINE_BIAS = 0.25
  private const val LINE_STRENGTH = 0.9

  // Fine brightness jitter: noise scale and total spread (about plus or minus 3 percent).
  private const val SPECKLE_FREQ = 40.0
  private const val GRAIN_X = 3.0
  private const val GRAIN_Y = 90.0
  private const val SHADE_SPREAD = 0.06
  private const val HALF = 0.5

  // Offsets that give each noise layer its own pattern.
  private const val SALT_PORE = 3
  private const val SALT_SPECKLE = 4
  private const val SALT_WAVE = 5
  private const val SALT_BROAD = 6
  private const val SALT_FINE = 7
  private const val SALT_STREAK = 8
  private const val SALT_GRAIN = 9
  private const val HASH_DROP = 8

  // Xorshift32 shifts.
  private const val XORSHIFT_A = 13
  private const val XORSHIFT_B = 17
  private const val XORSHIFT_C = 5
  private const val HASH_X = 0x27d4eb2d
  private const val HASH_Y = 0x165667b1
  private const val HASH_RANGE = 16777216.0
  private const val RING_SALT = 0x5bd1e995
  private const val OPAQUE = 0xFF
  private const val ALPHA_SHIFT = 24
  private const val RED_SHIFT = 16
  private const val GREEN_SHIFT = 8
  private const val MAX_CHANNEL = 0xFF
}

private const val FNV_OFFSET = -2128831035
private const val FNV_PRIME = 16777619
private const val GOLDEN = -1640531527
private const val SECOND_PRIME = 668265263
private const val MIX_SHIFT_WIDE = 16
private const val MIX_SHIFT_NARROW = 13
private const val MIX_MULTIPLY_A = -2048144789
private const val MIX_MULTIPLY_B = -1028477387

/** A 32-bit finaliser (murmur3 style) that scrambles every bit. */
private fun mix(value: Int): Int {
  var h = value
  h = h xor (h ushr MIX_SHIFT_WIDE)
  h *= MIX_MULTIPLY_A
  h = h xor (h ushr MIX_SHIFT_NARROW)
  h *= MIX_MULTIPLY_B
  h = h xor (h ushr MIX_SHIFT_WIDE)
  return h
}
