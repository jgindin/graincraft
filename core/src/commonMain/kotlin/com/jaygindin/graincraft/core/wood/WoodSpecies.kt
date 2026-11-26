package com.jaygindin.graincraft.core.wood

import kotlin.math.pow
import kotlinx.serialization.Serializable

/** An opaque sRGB colour packed as ARGB. */
@Serializable
data class Rgb(val argb: Int) {
  /** WCAG relative luminance: 0.0 is black, 1.0 is white. */
  val luminance: Double
    get() =
      LUMA_RED * linear((argb shr RED_SHIFT) and CHANNEL_MASK) +
        LUMA_GREEN * linear((argb shr GREEN_SHIFT) and CHANNEL_MASK) +
        LUMA_BLUE * linear(argb and CHANNEL_MASK)

  companion object {
    private const val OPAQUE = 0xFF000000.toInt()
    private const val HEX_RADIX = 16
    private const val RGB_DIGITS = 6

    /** Parses `#RRGGBB` (the `#` is optional). */
    fun hex(s: String): Rgb {
      val digits = s.removePrefix("#")
      require(digits.length == RGB_DIGITS) { "Expected six hex digits, got '$s'" }
      val value = requireNotNull(digits.toIntOrNull(HEX_RADIX)) { "Not a hex colour: '$s'" }
      return Rgb(OPAQUE or value)
    }

    /** From 0..255 channels. */
    fun of(red: Int, green: Int, blue: Int): Rgb =
      Rgb(
        OPAQUE or
          (red.coerceIn(0, CHANNEL_MASK) shl RED_SHIFT) or
          (green.coerceIn(0, CHANNEL_MASK) shl GREEN_SHIFT) or
          blue.coerceIn(0, CHANNEL_MASK),
      )
  }
}

private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val CHANNEL_MASK = 0xFF
private const val LUMA_RED = 0.2126
private const val LUMA_GREEN = 0.7152
private const val LUMA_BLUE = 0.0722
private const val SRGB_KNEE = 0.04045
private const val SRGB_LOW_DIVISOR = 12.92
private const val SRGB_OFFSET = 0.055
private const val SRGB_SCALE = 1.055
private const val SRGB_GAMMA = 2.4

private fun linear(channel: Int): Double {
  val c = channel / CHANNEL_MASK.toDouble()
  return if (c <= SRGB_KNEE) c / SRGB_LOW_DIVISOR
  else ((c + SRGB_OFFSET) / SRGB_SCALE).pow(SRGB_GAMMA)
}

/** The three tones a renderer needs for grain contrast. */
@Serializable data class WoodTones(val base: Rgb, val latewood: Rgb, val pore: Rgb)

enum class PoreType {
  DIFFUSE,
  SEMI_RING,
  RING_OPEN,
  /** Ring-porous with pores closed by tyloses. */
  RING_CLOSED,
}

/** Ordered best to worst, so the worst of several ratings is the maximum. */
enum class Rating {
  RECOMMENDED,
  CAUTION,
  NOT_SUITABLE,
}

@Serializable data class GrainRating(val rating: Rating, val reasons: List<String>)

@Serializable data class Suitability(val edgeGrain: GrainRating, val endGrain: GrainRating)

@Serializable
data class WoodSpecies(
  val id: String,
  val name: String,
  val botanicalName: String,
  val jankaLbf: Int,
  val raw: WoodTones,
  val oiled: WoodTones,
  val poreType: PoreType,
  val oily: Boolean,
  val bleedsColor: Boolean,
  val irritant: Boolean,
  val suitability: Suitability,
  val notes: List<String>,
  val custom: Boolean = false,
)
