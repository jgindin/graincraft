package com.jaygindin.graincraft.core.design

import com.jaygindin.graincraft.core.units.Length
import kotlin.test.Test
import kotlin.test.assertEquals

class StripePresetsTest {
  private val half = Length.inches(0, 1, 2)
  private val eighth = Length.inches(0, 1, 8)

  @Test
  fun uniformCyclesNothingAndUsesEachSpeciesOnce() {
    val strips = StripePresets.uniform(listOf("a", "b", "c"), half)
    assertEquals(listOf("a", "b", "c"), strips.map { it.speciesId })
    assertEquals(listOf(half, half, half), strips.map { it.width })
  }

  @Test
  fun alternatingCyclesSpeciesForCountStrips() {
    val strips = StripePresets.alternating(listOf("a", "b"), 5, half)
    assertEquals(listOf("a", "b", "a", "b", "a"), strips.map { it.speciesId })
  }

  @Test
  fun mirrorReflectsAroundTheLastStrip() {
    val abc = StripePresets.uniform(listOf("A", "B", "C"), half)
    assertEquals(listOf("A", "B", "C", "B", "A"), StripePresets.mirror(abc).map { it.speciesId })
  }

  @Test
  fun fibonacciUsesMirroredWidthsAndAlternatingSpecies() {
    val strips = StripePresets.fibonacci("w", "m", half)
    assertEquals(
      listOf(half, half, Length.inches(1), Length.inches(1, 1, 2), Length.inches(2, 1, 2)) +
        listOf(Length.inches(1, 1, 2), Length.inches(1), half, half),
      strips.map { it.width },
    )
    assertEquals(
      listOf("w", "m", "w", "m", "w", "m", "w", "m", "w"),
      strips.map { it.speciesId },
    )
  }

  @Test
  fun pinstripePutsTheAccentBetweenEachPair() {
    val ab = listOf(Strip("A", half), Strip("B", half))
    val strips = StripePresets.pinstripe(ab, "p", eighth)
    assertEquals(listOf("A", "p", "B"), strips.map { it.speciesId })
    assertEquals(eighth, strips[1].width)
  }
}
