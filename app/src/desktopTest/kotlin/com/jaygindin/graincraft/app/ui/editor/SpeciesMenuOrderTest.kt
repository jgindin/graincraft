package com.jaygindin.graincraft.app.ui.editor

import com.jaygindin.graincraft.core.wood.PoreType
import com.jaygindin.graincraft.core.wood.Rgb
import com.jaygindin.graincraft.core.wood.WoodSpecies
import com.jaygindin.graincraft.core.wood.Woods
import com.jaygindin.graincraft.core.wood.derivedTones
import com.jaygindin.graincraft.core.wood.rateCustomWood
import kotlin.test.Test
import kotlin.test.assertEquals

class SpeciesMenuOrderTest {
  private fun custom(name: String): WoodSpecies {
    val base = Rgb.hex("#8A5A3C")
    return WoodSpecies(
      id = "custom-$name",
      name = name,
      botanicalName = "Custom",
      jankaLbf = 1200,
      raw = derivedTones(base),
      oiled = derivedTones(base),
      poreType = PoreType.DIFFUSE,
      oily = false,
      bleedsColor = false,
      irritant = false,
      suitability = rateCustomWood(1200, PoreType.DIFFUSE, oily = false, irritant = false),
      notes = emptyList(),
      custom = true,
    )
  }

  @Test
  fun speciesAreAlphabeticalIgnoringCaseWithCustomWoodsMixedIn() {
    val woods = Woods(listOf(custom("zelkova"), custom("Acacia")))

    val names = speciesMenuOrder(woods).map { it.name }

    assertEquals(names.sortedBy { it.lowercase() }, names)
    assertEquals("Acacia", names.first())
    assertEquals("zelkova", names.last())
    assertEquals(woods.all.size, names.size)
  }
}
