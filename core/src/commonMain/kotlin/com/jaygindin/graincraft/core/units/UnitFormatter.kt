package com.jaygindin.graincraft.core.units

enum class FractionPrecision(val denominator: Int) {
  SIXTEENTH(16),
  THIRTY_SECOND(32),
}

/** The only route from a [Length] to text the user reads. */
class UnitFormatter(val unit: LengthUnit, val precision: FractionPrecision) {
  fun format(length: Length): String =
    when (unit) {
      LengthUnit.INCH -> formatInches(length)
      LengthUnit.MILLIMETRE -> formatMillimetres(length)
    }

  private fun formatInches(length: Length): String {
    val d = precision.denominator
    val step = Length.NM_PER_INCH / d
    val parts = (2 * length.nanometres + step).floorDiv(2 * step)
    val whole = parts / d
    val rem = (parts % d).toInt()
    if (rem == 0) return "$whole\""
    val g = gcd(rem, d)
    val fraction = "${rem / g}/${d / g}"
    return if (whole == 0L) "$fraction\"" else "$whole-$fraction\""
  }

  private fun formatMillimetres(length: Length): String {
    val tenths = (length.nanometres + HALF_TENTH_MM).floorDiv(TENTH_MM)
    return "${tenths / 10}.${tenths % 10} mm"
  }

  private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

  private companion object {
    const val TENTH_MM = 100_000L
    const val HALF_TENTH_MM = TENTH_MM / 2
  }
}
