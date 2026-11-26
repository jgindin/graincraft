package com.jaygindin.graincraft.core.wood

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WoodCatalogTest {
  private val table: Map<String, Triple<Int, Rating, Rating>> =
    mapOf(
      "hard-maple" to Triple(1450, Rating.RECOMMENDED, Rating.RECOMMENDED),
      "ambrosia-maple" to Triple(950, Rating.CAUTION, Rating.CAUTION),
      "black-walnut" to Triple(1010, Rating.RECOMMENDED, Rating.RECOMMENDED),
      "american-cherry" to Triple(950, Rating.RECOMMENDED, Rating.RECOMMENDED),
      "white-oak" to Triple(1360, Rating.RECOMMENDED, Rating.RECOMMENDED),
      "red-oak" to Triple(1290, Rating.CAUTION, Rating.NOT_SUITABLE),
      "white-ash" to Triple(1320, Rating.CAUTION, Rating.CAUTION),
      "hickory" to Triple(1820, Rating.RECOMMENDED, Rating.RECOMMENDED),
      "european-beech" to Triple(1300, Rating.RECOMMENDED, Rating.RECOMMENDED),
      "yellow-birch" to Triple(1260, Rating.RECOMMENDED, Rating.RECOMMENDED),
      "purpleheart" to Triple(2520, Rating.RECOMMENDED, Rating.RECOMMENDED),
      "african-padauk" to Triple(1970, Rating.RECOMMENDED, Rating.RECOMMENDED),
      "yellowheart" to Triple(1820, Rating.RECOMMENDED, Rating.RECOMMENDED),
      "bloodwood" to Triple(2900, Rating.RECOMMENDED, Rating.RECOMMENDED),
      "zebrawood" to Triple(1575, Rating.CAUTION, Rating.CAUTION),
      "wenge" to Triple(1630, Rating.CAUTION, Rating.CAUTION),
      "sapele" to Triple(1410, Rating.RECOMMENDED, Rating.RECOMMENDED),
      "jatoba" to Triple(2690, Rating.RECOMMENDED, Rating.RECOMMENDED),
      "teak" to Triple(1070, Rating.CAUTION, Rating.CAUTION),
      "bubinga" to Triple(1980, Rating.CAUTION, Rating.CAUTION),
      "osage-orange" to Triple(2040, Rating.RECOMMENDED, Rating.RECOMMENDED),
      "bocote" to Triple(2010, Rating.CAUTION, Rating.CAUTION),
      "eastern-white-pine" to Triple(380, Rating.NOT_SUITABLE, Rating.NOT_SUITABLE),
      "western-red-cedar" to Triple(350, Rating.NOT_SUITABLE, Rating.NOT_SUITABLE),
      "spalted-maple" to Triple(700, Rating.NOT_SUITABLE, Rating.NOT_SUITABLE),
      "cocobolo" to Triple(1136, Rating.NOT_SUITABLE, Rating.NOT_SUITABLE),
    )

  private fun idsWhere(predicate: (WoodSpecies) -> Boolean) =
    WoodCatalog.builtIns.filter(predicate).map { it.id }.toSet()

  @Test
  fun hasTwentySixSpecies() {
    assertEquals(26, WoodCatalog.builtIns.size)
    assertEquals(26, WoodCatalog.builtIns.map { it.id }.toSet().size)
  }

  @Test
  fun ratingsMatchTheDesignTable() {
    assertEquals(table.keys, WoodCatalog.builtIns.map { it.id }.toSet())
    for ((id, expected) in table) {
      val s = assertNotNull(WoodCatalog.byId(id), id)
      assertEquals(
        expected,
        Triple(s.jankaLbf, s.suitability.edgeGrain.rating, s.suitability.endGrain.rating),
        id,
      )
    }
  }

  @Test
  fun flagsMatchTheDesign() {
    assertEquals(setOf("teak", "wenge", "bocote", "cocobolo"), idsWhere { it.oily })
    assertEquals(setOf("african-padauk", "bloodwood"), idsWhere { it.bleedsColor })
    val irritants =
      setOf(
        "purpleheart",
        "african-padauk",
        "yellowheart",
        "zebrawood",
        "wenge",
        "bubinga",
        "osage-orange",
        "bocote",
        "western-red-cedar",
        "cocobolo",
        "jatoba",
      )
    assertTrue(idsWhere { it.irritant }.containsAll(irritants))
  }

  @Test
  fun everyCautionOrWorseHasAReason() {
    for (s in WoodCatalog.builtIns) {
      for (g in listOf(s.suitability.edgeGrain, s.suitability.endGrain)) {
        if (g.rating != Rating.RECOMMENDED) assertTrue(g.reasons.isNotEmpty(), s.id)
      }
    }
  }

  @Test
  fun oiledIsDarkerThanRaw() {
    for (s in WoodCatalog.builtIns) {
      assertTrue(s.oiled.base.luminance < s.raw.base.luminance, s.id)
    }
  }

  @Test
  fun luminanceOfBlackAndWhite() {
    assertEquals(0.0, Rgb.hex("#000000").luminance, 1e-9)
    assertEquals(1.0, Rgb.hex("#FFFFFF").luminance, 1e-9)
  }

  @Test
  fun customIdsNeverShadowBuiltIns() {
    val clash = WoodCatalog.byId("hard-maple")!!.copy(name = "Impostor", custom = true)
    val mine = clash.copy(id = "my-wood", name = "Mine")
    val woods = Woods(listOf(clash, mine))
    assertEquals("Hard Maple", woods.byId("hard-maple")?.name)
    assertEquals("Mine", woods.byId("my-wood")?.name)
    assertEquals(27, woods.all.size)
  }
}
