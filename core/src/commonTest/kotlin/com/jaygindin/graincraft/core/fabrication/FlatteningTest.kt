package com.jaygindin.graincraft.core.fabrication

import com.jaygindin.graincraft.core.design.FlatteningMethod
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.Length
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FlatteningTest {
  private fun inch(w: Int, n: Int = 0, d: Int = 1) = Length.inches(w, n, d)

  @Test
  fun defaultProfileUsesPlanerWhenItFits() {
    val f = longGrainFlattening(inch(12, 1, 4), ShopProfile())
    assertEquals(FlatteningTool.PLANER, f.tool)
    assertEquals(inch(13), f.widthLimit)
    assertEquals(inch(0, 1, 16), f.allowance)
  }

  @Test
  fun drumSanderTakesOverWhenPlanerIsTooNarrow() {
    val profile = ShopProfile(drumSanderMaxWidth = inch(16))
    val f = longGrainFlattening(inch(15), profile)
    assertEquals(FlatteningTool.DRUM_SANDER, f.tool)
    assertEquals(inch(16), f.widthLimit)
    assertEquals(inch(0, 1, 16), f.allowance)
  }

  @Test
  fun planerIsKeptAndPanelSplitWhenNothingFits() {
    val f = longGrainFlattening(inch(15), ShopProfile())
    assertEquals(FlatteningTool.PLANER, f.tool)
    assertEquals(inch(13), f.widthLimit)
  }

  @Test
  fun widestMachineWinsWhenNothingFits() {
    val profile = ShopProfile(planerMaxWidth = inch(13), drumSanderMaxWidth = inch(16))
    assertEquals(FlatteningTool.DRUM_SANDER, longGrainFlattening(inch(20), profile).tool)
  }

  @Test
  fun drumSanderAloneIsUsed() {
    val profile = ShopProfile(planerMaxWidth = null, drumSanderMaxWidth = inch(16))
    assertEquals(FlatteningTool.DRUM_SANDER, longGrainFlattening(inch(10), profile).tool)
  }

  @Test
  fun noMachinesUseEndGrainMethod() {
    val profile = ShopProfile(planerMaxWidth = null)
    val f = longGrainFlattening(inch(12, 1, 4), profile)
    assertEquals(FlatteningTool.ROUTER_SLED, f.tool)
    assertNull(f.widthLimit)
    assertEquals(inch(0, 1, 8), f.allowance)
  }

  @Test
  fun endGrainBeltAndOrbitalAndDrumWithoutWidth() {
    val belt =
      ShopProfile(planerMaxWidth = null, endGrainFlattening = FlatteningMethod.BELT_AND_ORBITAL)
    assertEquals(FlatteningTool.BELT_AND_ORBITAL, longGrainFlattening(inch(10), belt).tool)
    val drum = ShopProfile(planerMaxWidth = null, endGrainFlattening = FlatteningMethod.DRUM_SANDER)
    val f = longGrainFlattening(inch(10), drum)
    assertEquals(FlatteningTool.DRUM_SANDER, f.tool)
    assertNull(f.widthLimit)
    assertEquals(inch(0, 1, 16), f.allowance)
  }

  @Test
  fun splitLimitForEndGrainIsSmallerOfMachineAndCrosscut() {
    val profile = ShopProfile()
    val f = longGrainFlattening(inch(10), profile)
    assertEquals(inch(12), splitLimit(f, true, profile))
    assertEquals(inch(13), splitLimit(f, false, profile))
  }

  @Test
  fun splitLimitWithoutMachinesIsCrosscutForEndGrainOnly() {
    val profile = ShopProfile(planerMaxWidth = null)
    val f = longGrainFlattening(inch(10), profile)
    assertEquals(inch(12), splitLimit(f, true, profile))
    assertNull(splitLimit(f, false, profile))
  }

  @Test
  fun splitLimitUsesNarrowerMachineThanCrosscut() {
    val profile = ShopProfile(planerMaxWidth = inch(8))
    val f = longGrainFlattening(inch(6), profile)
    assertEquals(inch(8), splitLimit(f, true, profile))
  }
}
