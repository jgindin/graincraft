package com.jaygindin.graincraft.core.units

import kotlin.test.Test
import kotlin.test.assertEquals

class BoardFeetTest {
  @Test
  fun eightQuarterInnerStrip() {
    assertEquals(0.519097, boardFeet(Nominal.EIGHT_QUARTER, inch(1, 5, 8), inch(23)), 1e-6)
  }

  @Test
  fun labelsAndThicknesses() {
    assertEquals("4/4", Nominal.FOUR_QUARTER.label)
    assertEquals(inch(1, 1, 4), Nominal.FIVE_QUARTER.nominalThickness)
    assertEquals(inch(1, 1, 2), Nominal.SIX_QUARTER.nominalThickness)
    assertEquals(inch(2), Nominal.EIGHT_QUARTER.nominalThickness)
  }
}
