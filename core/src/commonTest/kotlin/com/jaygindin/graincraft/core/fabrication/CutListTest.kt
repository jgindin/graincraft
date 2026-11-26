package com.jaygindin.graincraft.core.fabrication

import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.design.PatternSpec
import com.jaygindin.graincraft.core.design.Strip
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.FractionPrecision
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.LengthUnit
import com.jaygindin.graincraft.core.units.Nominal
import com.jaygindin.graincraft.core.units.UnitFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CutListTest {
  private val fmt = UnitFormatter(LengthUnit.INCH, FractionPrecision.SIXTEENTH)
  private val profile = ShopProfile()

  private fun inch(w: Int, n: Int = 0, d: Int = 1) = Length.inches(w, n, d)

  private fun fabrication(mode: GrainMode, target: Dimensions, pattern: PatternSpec): Fabrication {
    val fit = fitPattern(mode, target, pattern, fmt)
    assertTrue(fit is FitResult.Ok, "expected a fit but was $fit")
    val result = compileFabrication(mode, target, fit.fit, profile)
    assertTrue(result is FabricationResult.Ok, "expected Ok but was $result")
    return result.fabrication
  }

  private val partsA by lazy {
    cutList(
      fabrication(
        GrainMode.END_GRAIN,
        Dimensions(inch(18), inch(12), inch(1, 1, 2)),
        PatternSpec.Checkerboard("walnut", "hard-maple", inch(1, 1, 2)),
      ),
      profile,
    )
  }

  private val partsB by lazy {
    cutList(
      fabrication(
        GrainMode.EDGE_GRAIN,
        Dimensions(inch(16), inch(8, 1, 4), inch(1, 1, 4)),
        PatternSpec.Stripes(
          listOf(
            Strip("walnut", inch(1, 1, 2)),
            Strip("hard-maple", inch(0, 3, 4)),
            Strip("cherry", inch(1, 1, 2)),
            Strip("hard-maple", inch(0, 3, 4)),
            Strip("cherry", inch(1, 1, 2)),
            Strip("hard-maple", inch(0, 3, 4)),
            Strip("walnut", inch(1, 1, 2)),
          ),
        ),
      ),
      profile,
    )
  }

  private val partsC by lazy {
    cutList(
      fabrication(
        GrainMode.END_GRAIN,
        Dimensions(inch(17, 1, 4), inch(12), inch(1, 1, 2)),
        PatternSpec.Brick("cherry", "hard-maple", inch(3), inch(1, 1, 2), inch(0, 1, 4)),
      ),
      profile,
    )
  }

  private fun part(
    speciesId: String,
    count: Int,
    width: Length,
    height: Length,
    length: Length,
    stock: Nominal?,
    fromThickness: Length,
    ripWidth: Length,
    panelNames: List<String>,
    laminated: Boolean = false,
  ) =
    CutPart(
      speciesId,
      count,
      width,
      height,
      length,
      stock,
      fromThickness,
      ripWidth,
      laminated,
      panelNames,
    )

  private fun assertLumber(expected: List<Pair<String, Double>>, total: Double, actual: Lumber) {
    assertEquals(expected.map { it.first }, actual.bySpecies.map { it.speciesId })
    expected.zip(actual.bySpecies).forEach { (e, a) ->
      assertEquals(e.second, a.boardFeet, 1e-6, "board feet for ${e.first}")
    }
    assertEquals(total, actual.total, 1e-6, "total board feet")
  }

  @Test
  fun exampleAParts() {
    val h = inch(1, 9, 16)
    val l = inch(23)
    val names = listOf("1A", "1B")
    val e8 = Nominal.EIGHT_QUARTER
    assertEquals(
      listOf(
        part("walnut", 1, inch(1, 5, 8), h, l, e8, inch(1, 5, 8), h, listOf("1A")),
        part("hard-maple", 3, inch(1, 1, 2), h, l, e8, h, inch(1, 1, 2), names),
        part("walnut", 3, inch(1, 1, 2), h, l, e8, h, inch(1, 1, 2), names),
        part("hard-maple", 1, inch(1, 5, 8), h, l, e8, inch(1, 5, 8), h, listOf("1B")),
      ),
      partsA,
    )
  }

  @Test
  fun exampleALumber() {
    val lumber = lumber(partsA, profile)
    assertLumber(listOf("walnut" to 2.725260, "hard-maple" to 2.725260), 5.450521, lumber)
    assertEquals(listOf("2.7 BF", "2.7 BF"), lumber.bySpecies.map { formatBoardFeet(it.boardFeet) })
    assertEquals("5.5 BF", formatBoardFeet(lumber.total))
  }

  @Test
  fun exampleBParts() {
    val h = inch(1, 5, 16)
    val l = inch(18)
    val e8 = Nominal.EIGHT_QUARTER
    val walnut = inch(1, 5, 8)
    val cherry = inch(1, 1, 2)
    val maple = inch(0, 3, 4)
    val names = listOf("1")
    assertEquals(
      listOf(
        part("walnut", 2, walnut, h, l, e8, walnut, h, names),
        part("hard-maple", 3, maple, h, l, Nominal.FOUR_QUARTER, maple, h, names),
        part("cherry", 2, cherry, h, l, e8, cherry, h, names),
      ),
      partsB,
    )
  }

  @Test
  fun exampleBLumber() {
    val lumber = lumber(partsB, profile)
    assertLumber(
      listOf("walnut" to 0.934375, "hard-maple" to 0.700781, "cherry" to 0.934375),
      2.569531,
      lumber,
    )
    assertEquals(
      listOf("0.9 BF", "0.7 BF", "0.9 BF"),
      lumber.bySpecies.map { formatBoardFeet(it.boardFeet) },
    )
    assertEquals("2.6 BF", formatBoardFeet(lumber.total))
  }

  @Test
  fun exampleCParts() {
    val h = inch(1, 9, 16)
    val l = inch(19, 1, 2)
    val e8 = Nominal.EIGHT_QUARTER
    val mortar = inch(0, 1, 4)
    val names = listOf("1A", "1B")
    val boardH = inch(0, 5, 16)
    val boardL = inch(17, 3, 4)
    assertEquals(
      listOf(
        part("cherry", 5, inch(3), h, l, e8, h, inch(3), names),
        part("hard-maple", 4, mortar, h, l, e8, h, mortar, names),
        part(
          "hard-maple",
          2,
          inch(8),
          boardH,
          boardL,
          Nominal.FOUR_QUARTER,
          boardH,
          inch(8),
          listOf("MA", "MB"),
        ),
      ),
      partsC,
    )
  }

  @Test
  fun exampleCLumber() {
    val lumber = lumber(partsC, profile)
    assertLumber(listOf("cherry" to 5.501302, "hard-maple" to 3.132075), 8.633377, lumber)
    assertEquals(listOf("5.5 BF", "3.1 BF"), lumber.bySpecies.map { formatBoardFeet(it.boardFeet) })
    assertEquals("8.6 BF", formatBoardFeet(lumber.total))
  }

  private fun singlePanel(kind: PanelKind, width: Length, height: Length): Fabrication {
    val panel =
      Panel(
        name = "1",
        kind = kind,
        strips = listOf(PanelStrip("cherry", width, 0, StripRole.PLAIN)),
        width = width,
        millHeight = height,
        flattenTo = height,
        length = inch(20),
        flattening = LongGrainFlattening(FlatteningTool.PLANER, inch(13), inch(0, 1, 16)),
      )
    return Fabrication(
      mode = if (kind == PanelKind.EDGE_GRAIN_BOARD) GrainMode.EDGE_GRAIN else GrainMode.END_GRAIN,
      panels = listOf(panel),
      sliceThickness = null,
      slicesPerPanel = emptyMap(),
      rows = emptyList(),
      glueUpWidth = width,
      glueUpLength = inch(20),
      finalFlatten = null,
      finalAllowance = inch(0, 1, 16),
    )
  }

  @Test
  fun thinStripsAreRippedFromThickStock() {
    val parts = cutList(singlePanel(PanelKind.PANEL_1, inch(0, 1, 4), inch(1, 9, 16)), profile)
    val p = parts.single()
    assertEquals(Nominal.EIGHT_QUARTER, p.stock)
    assertEquals(inch(1, 9, 16), p.fromThickness)
    assertEquals(inch(0, 1, 4), p.ripWidth)
    assertEquals(false, p.laminated)
  }

  @Test
  fun tooThickBothWays() {
    val parts = cutList(singlePanel(PanelKind.PANEL_1, inch(2), inch(2, 1, 2)), profile)
    val p = parts.single()
    assertNull(p.stock)
    assertEquals(inch(2), p.fromThickness)
    assertEquals(inch(2, 1, 2), p.ripWidth)
    // Priced as 2" stock: 2 x (2-1/2 + 1/8) x 20 / 144 x 1.3.
    assertEquals(2 * 2.625 * 20 / 144 * 1.3, lumber(parts, profile).total, 1e-9)
  }

  @Test
  fun tooThickPricesTheNextQuarterInch() {
    val parts = cutList(singlePanel(PanelKind.PANEL_1, inch(2, 1, 8), inch(3)), profile)
    assertNull(parts.single().stock)
    // 2-1/8 rounds up to 2-1/4: 2.25 x (3 + 1/8) x 20 / 144 x 1.3.
    assertEquals(2.25 * 3.125 * 20 / 144 * 1.3, lumber(parts, profile).total, 1e-9)
  }

  @Test
  fun wideEdgeGrainStripIsTwoLaminations() {
    val height = inch(1, 5, 16)
    val parts = cutList(singlePanel(PanelKind.EDGE_GRAIN_BOARD, inch(2, 1, 2), height), profile)
    val half = inch(1, 1, 4)
    assertEquals(
      listOf(
        part(
          "cherry",
          2,
          half,
          height,
          inch(20),
          Nominal.SIX_QUARTER,
          half,
          height,
          listOf("1"),
          laminated = true,
        ),
      ),
      parts,
    )
  }

  @Test
  fun edgeGrainStripAtTheThickestStockIsNotLaminated() {
    val parts =
      cutList(singlePanel(PanelKind.EDGE_GRAIN_BOARD, inch(1, 3, 4), inch(1, 5, 16)), profile)
    val p = parts.single()
    assertEquals(1, p.count)
    assertEquals(false, p.laminated)
    assertEquals(Nominal.EIGHT_QUARTER, p.stock)
  }

  @Test
  fun formatRoundsHalfUp() {
    assertEquals("0.3 BF", formatBoardFeet(0.25))
    assertEquals("0.2 BF", formatBoardFeet(0.249999))
    assertEquals("12.0 BF", formatBoardFeet(11.96))
    assertEquals("0.4 BF", formatBoardFeet(0.35))
    assertEquals("0.0 BF", formatBoardFeet(0.0))
  }
}
