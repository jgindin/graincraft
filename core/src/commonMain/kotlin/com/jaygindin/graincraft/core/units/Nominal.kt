package com.jaygindin.graincraft.core.units

/** Rough-sawn hardwood thickness grades, named in quarters of an inch. */
enum class Nominal(val quarters: Int) {
  FOUR_QUARTER(4),
  FIVE_QUARTER(5),
  SIX_QUARTER(6),
  EIGHT_QUARTER(8);

  val nominalThickness: Length
    get() = Length.inches(quarters / 4, quarters % 4, 4)

  val label: String
    get() = "$quarters/4"
}

private const val QUARTERS_PER_INCH = 4.0
private const val BOARD_FOOT_DIVISOR = 144.0

/** Board feet are priced on nominal thickness: nominal inches x width x length / 144. */
fun boardFeet(nominal: Nominal, ripWidth: Length, length: Length): Double =
  (nominal.quarters / QUARTERS_PER_INCH) * ripWidth.inches * length.inches / BOARD_FOOT_DIVISOR
