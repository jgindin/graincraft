package com.jaygindin.graincraft.core.units

import kotlin.jvm.JvmInline
import kotlin.math.round
import kotlinx.serialization.Serializable

/**
 * A distance stored as whole nanometres. One 64th of an inch is exactly 396,875 nm, so every
 * fraction the shop works in is exact and sums never drift.
 */
@Serializable
@JvmInline
value class Length(val nanometres: Long) : Comparable<Length> {
  operator fun plus(o: Length): Length = Length(nanometres + o.nanometres)

  operator fun minus(o: Length): Length = Length(nanometres - o.nanometres)

  operator fun times(n: Int): Length = Length(nanometres * n)

  /** Divides and rounds half up to the nearest nanometre. */
  operator fun div(n: Int): Length {
    require(n > 0) { "divisor must be positive" }
    return Length((2 * nanometres + n).floorDiv(2L * n))
  }

  operator fun div(o: Length): Double = nanometres.toDouble() / o.nanometres

  /** Rounds to the nearest 64th of an inch, half up. */
  fun roundToSixtyFourth(): Length {
    val step = NM_PER_SIXTY_FOURTH
    return Length((2 * nanometres + step).floorDiv(2 * step) * step)
  }

  val inches: Double
    get() = nanometres.toDouble() / NM_PER_INCH

  val millimetres: Double
    get() = nanometres.toDouble() / NM_PER_MM

  override fun compareTo(other: Length): Int = nanometres.compareTo(other.nanometres)

  companion object {
    val ZERO = Length(0)
    const val NM_PER_INCH = 25_400_000L
    internal const val NM_PER_SIXTY_FOURTH = NM_PER_INCH / 64
    private const val NM_PER_MM = 1_000_000L
    private val BINARY_DENOMINATORS = setOf(1, 2, 4, 8, 16, 32, 64)

    /** An exact length of `whole + numerator/denominator` inches; the denominator divides 64. */
    fun inches(whole: Int, numerator: Int = 0, denominator: Int = 1): Length {
      require(denominator in BINARY_DENOMINATORS) { "denominator must divide 64: $denominator" }
      require(whole >= 0 && numerator >= 0) { "length must not be negative" }
      val sixtyFourths = whole.toLong() * 64 + numerator.toLong() * (64 / denominator)
      return Length(sixtyFourths * NM_PER_SIXTY_FOURTH)
    }

    /** Converts decimal inches, rounding to the nearest nanometre. */
    fun inchesApprox(value: Double): Length = Length(round(value * NM_PER_INCH).toLong())

    fun millimetres(value: Double): Length = Length(round(value * NM_PER_MM).toLong())
  }
}

fun max(a: Length, b: Length): Length = if (a >= b) a else b

fun min(a: Length, b: Length): Length = if (a <= b) a else b

fun Iterable<Length>.sum(): Length = fold(Length.ZERO) { acc, l -> acc + l }
