package com.jaygindin.graincraft.core.units

import kotlin.test.Test
import kotlin.test.assertEquals

class UnitFormatterTest {
  private val sixteenths = UnitFormatter(LengthUnit.INCH, FractionPrecision.SIXTEENTH)
  private val thirtySeconds = UnitFormatter(LengthUnit.INCH, FractionPrecision.THIRTY_SECOND)
  private val metric = UnitFormatter(LengthUnit.MILLIMETRE, FractionPrecision.SIXTEENTH)

  @Test
  fun sixteenths() {
    assertEquals("1-3/4\"", sixteenths.format(inch(1, 3, 4)))
    assertEquals("13\"", sixteenths.format(inch(13)))
    assertEquals("1/8\"", sixteenths.format(inch(0, 3, 32)))
    assertEquals("1-9/16\"", sixteenths.format(inch(1, 9, 16)))
    assertEquals("0\"", sixteenths.format(inch(0, 1, 64)))
  }

  @Test
  fun thirtySeconds() {
    assertEquals("3/32\"", thirtySeconds.format(inch(0, 3, 32)))
  }

  @Test
  fun millimetres() {
    assertEquals("44.5 mm", metric.format(Length.millimetres(44.5)))
    assertEquals("25.4 mm", metric.format(inch(1)))
  }
}
