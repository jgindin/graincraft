package com.jaygindin.graincraft.core.units

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

fun inch(w: Int, n: Int = 0, d: Int = 1) = Length.inches(w, n, d)

class LengthTest {
  @Test
  fun sixtyFourthIsExact() {
    assertEquals(396_875L, Length.inches(0, 1, 64).nanometres)
  }

  @Test
  fun sumsDoNotDrift() {
    assertEquals(inch(1), List(64) { inch(0, 1, 64) }.sum())
  }

  @Test
  fun divisionRoundsHalfUp() {
    assertEquals(Length(3), Length(5) / 2)
    assertEquals(Length(2), Length(4) / 2)
  }

  @Test
  fun roundToSixtyFourth() {
    assertEquals(inch(3, 21, 64), Length.inchesApprox(10.0 / 3).roundToSixtyFourth())
  }

  @Test
  fun rejectsNonBinaryDenominator() {
    assertFailsWith<IllegalArgumentException> { inch(0, 1, 3) }
  }

  @Test
  fun arithmeticAndComparison() {
    assertEquals(inch(2), inch(1) + inch(1))
    assertEquals(inch(0, 1, 2), inch(1) - inch(0, 1, 2))
    assertEquals(inch(3), inch(1) * 3)
    assertEquals(2.0, inch(2) / inch(1))
    assertEquals(inch(2), max(inch(1), inch(2)))
    assertEquals(inch(1), min(inch(1), inch(2)))
    assertEquals(1.5, inch(1, 1, 2).inches)
    assertEquals(25.4, inch(1).millimetres)
    assertEquals(inch(1), Length.millimetres(25.4))
  }

  @Test
  fun divisorMustBePositive() {
    assertFailsWith<IllegalArgumentException> { inch(1) / 0 }
  }
}
