package com.jaygindin.graincraft.core.shop

import com.jaygindin.graincraft.core.design.FlatteningMethod
import com.jaygindin.graincraft.core.design.WorkspaceJson
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.Nominal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShopProfileTest {
  @Test
  fun defaultsMatchTheDesign() {
    val p = ShopProfile()
    assertEquals(Length.inches(0, 1, 8), p.kerf)
    assertEquals(Length.inches(13), p.planerMaxWidth)
    assertNull(p.drumSanderMaxWidth)
    assertEquals(Length.inches(12), p.crosscutMaxWidth)
    assertEquals(FlatteningMethod.ROUTER_SLED, p.endGrainFlattening)
    assertEquals(Length.inches(0, 1, 16), p.panelFlattenAllowance)
    assertEquals(
      mapOf(
        FlatteningMethod.DRUM_SANDER to Length.inches(0, 1, 16),
        FlatteningMethod.ROUTER_SLED to Length.inches(0, 1, 8),
        FlatteningMethod.BELT_AND_ORBITAL to Length.inches(0, 1, 16),
      ),
      p.endGrainFlattenAllowance,
    )
    assertEquals(Length.inches(0, 1, 16), p.edgeGrainFlattenAllowance)
    assertEquals(Length.inches(2), p.endTrim)
    assertEquals(Length.inches(0, 1, 8), p.edgeTrimPerSide)
    assertEquals(0.30, p.millingWaste)
    assertEquals(
      mapOf(
        Nominal.FOUR_QUARTER to Length.inches(0, 13, 16),
        Nominal.FIVE_QUARTER to Length.inches(1, 1, 16),
        Nominal.SIX_QUARTER to Length.inches(1, 5, 16),
        Nominal.EIGHT_QUARTER to Length.inches(1, 3, 4),
      ),
      p.surfacedThickness,
    )
    assertEquals("Titebond III", p.glue)
  }

  @Test
  fun mapsSerialiseKeyedByEnumName() {
    val text = WorkspaceJson.json.encodeToString(ShopProfile.serializer(), ShopProfile())
    assertTrue("\"ROUTER_SLED\"" in text && "\"EIGHT_QUARTER\"" in text, text)
    assertEquals(ShopProfile(), WorkspaceJson.json.decodeFromString(ShopProfile.serializer(), text))
  }
}
