package com.jaygindin.graincraft.core.units

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LengthParseTest {
  private fun ok(text: String, unit: LengthUnit = LengthUnit.INCH): Length {
    val result = parseLength(text, unit)
    assertIs<LengthParse.Ok>(result, "parsing '$text'")
    return result.length
  }

  private fun assertError(text: String) {
    val result = parseLength(text, LengthUnit.INCH)
    assertIs<LengthParse.Error>(result, "parsing '$text'")
    assertTrue(result.reason.isNotBlank())
  }

  @Test
  fun inchForms() {
    for (text in listOf("1-3/4", "1 3/4", "1.75", "1-3/4\"", "1-3/4 in", "1-3/4″")) {
      assertEquals(inch(1, 3, 4), ok(text), text)
    }
    assertEquals(inch(0, 3, 32), ok("3/32"))
  }

  @Test
  fun metricForms() {
    for (text in listOf("44.5mm", "44.5 mm", "4.45cm")) {
      assertEquals(Length.millimetres(44.5), ok(text), text)
    }
    assertEquals(Length.millimetres(44.5), ok("44.5", LengthUnit.MILLIMETRE))
  }

  @Test
  fun commonTypingVariants() {
    assertEquals(inch(1, 3, 4), ok("  1   3/4  "))
    assertEquals(inch(0, 3, 4), ok("¾"))
    assertEquals(inch(1, 1, 2), ok("1½"))
    assertEquals(Length.millimetres(44.5), ok("44,5 mm"))
  }

  @Test
  fun rejectsBadInput() {
    for (text in listOf("", "abc", "1/0", "-2", "1/3", "2 ft", "1/2 mm")) assertError(text)
  }
}
