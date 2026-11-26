package com.jaygindin.graincraft.core.fabrication

import com.jaygindin.graincraft.core.units.Length
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PanelSplitterTest {
  private fun inch(w: Int, n: Int = 0, d: Int = 1) = Length.inches(w, n, d)

  @Test
  fun fittingPanelIsOneGroup() {
    assertEquals(listOf(0..2), splitPanel(List(3) { inch(2) }, inch(12)))
  }

  @Test
  fun nullLimitIsOneGroup() {
    assertEquals(listOf(0..3), splitPanel(List(4) { inch(20) }, null))
  }

  @Test
  fun exampleACheckerboardSplitsInHalf() {
    val widths = listOf(inch(1, 5, 8)) + List(6) { inch(1, 1, 2) } + listOf(inch(1, 5, 8))
    assertEquals(listOf(0..3, 4..7), splitPanel(widths, inch(12)))
  }

  @Test
  fun exampleCBrickTakesLeftmostTie() {
    val widths =
      listOf(3, 0, 3, 0, 3, 0, 3, 0, 3).mapIndexed { i, w ->
        if (i % 2 == 0) inch(w) else inch(0, 1, 4)
      }
    assertEquals(listOf(0..3, 4..8), splitPanel(widths, inch(12)))
  }

  @Test
  fun tenStripsPreferWidestNarrowestThenLeftmost() {
    assertEquals(
      listOf(0..1, 2..3, 4..6, 7..9),
      splitPanel(List(10) { inch(2) }, inch(6)),
    )
  }

  @Test
  fun tooWideStripIsImpossible() {
    assertNull(splitPanel(listOf(inch(14)), inch(12)))
  }

  @Test
  fun splitEvenlyHalves() {
    assertEquals(listOf(inch(8), inch(8)), splitEvenly(inch(16), inch(12)))
  }

  @Test
  fun splitEvenlyKeepsWholeWhenItFitsOrNoLimit() {
    assertEquals(listOf(inch(10)), splitEvenly(inch(10), inch(12)))
    assertEquals(listOf(inch(30)), splitEvenly(inch(30), null))
  }

  @Test
  fun splitEvenlyPartsSumExactly() {
    val width = Length(25_400_000L * 16 + 1)
    val parts = splitEvenly(width, inch(12))
    assertEquals(2, parts.size)
    assertEquals(width.nanometres, parts.sumOf { it.nanometres })
    assertEquals(parts[0].nanometres, parts[1].nanometres + 1)
  }

  @Test
  fun subPanelNamesLetterTheParts() {
    assertEquals(listOf("1"), subPanelNames("1", 1))
    assertEquals(listOf("1A", "1B"), subPanelNames("1", 2))
    assertEquals(listOf("MA", "MB"), subPanelNames("M", 2))
  }
}
