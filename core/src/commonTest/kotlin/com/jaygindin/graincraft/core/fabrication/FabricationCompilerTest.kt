package com.jaygindin.graincraft.core.fabrication

import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.design.FlatteningMethod
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.design.PatternSpec
import com.jaygindin.graincraft.core.design.Strip
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.FractionPrecision
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.LengthUnit
import com.jaygindin.graincraft.core.units.UnitFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FabricationCompilerTest {
  private val fmt = UnitFormatter(LengthUnit.INCH, FractionPrecision.SIXTEENTH)
  private val profile = ShopProfile()

  private fun inch(w: Int, n: Int = 0, d: Int = 1) = Length.inches(w, n, d)

  private fun compile(
    mode: GrainMode,
    target: Dimensions,
    pattern: PatternSpec,
  ): FabricationResult {
    val fitResult = fitPattern(mode, target, pattern, fmt)
    assertTrue(fitResult is FitResult.Ok, "expected a fit but was $fitResult")
    return compileFabrication(mode, target, fitResult.fit, profile)
  }

  private fun ok(mode: GrainMode, target: Dimensions, pattern: PatternSpec): Fabrication {
    val result = compile(mode, target, pattern)
    assertTrue(result is FabricationResult.Ok, "expected Ok but was $result")
    return result.fabrication
  }

  private fun Fabrication.panel(name: String): Panel = panels.single { it.name == name }

  @Test
  fun exampleA_endGrainCheckerboard() {
    val f =
      ok(
        GrainMode.END_GRAIN,
        Dimensions(inch(18), inch(12), inch(1, 1, 2)),
        PatternSpec.Checkerboard("walnut", "hard-maple", inch(1, 1, 2)),
      )
    assertEquals(GrainMode.END_GRAIN, f.mode)
    assertEquals(listOf("1A", "1B"), f.panels.map { it.name })
    val a = f.panel("1A")
    val b = f.panel("1B")
    assertEquals(listOf(inch(1, 5, 8)) + List(3) { inch(1, 1, 2) }, a.strips.map { it.millWidth })
    assertEquals(List(3) { inch(1, 1, 2) } + listOf(inch(1, 5, 8)), b.strips.map { it.millWidth })
    val alternating = List(8) { if (it % 2 == 0) "walnut" else "hard-maple" }
    assertEquals(alternating, (a.strips + b.strips).map { it.speciesId })
    assertEquals((0..7).toList(), (a.strips + b.strips).map { it.sourceStripIndex })
    for (p in listOf(a, b)) {
      assertEquals(PanelKind.PANEL_1, p.kind)
      assertEquals(inch(6, 1, 8), p.width)
      assertEquals(inch(1, 9, 16), p.millHeight)
      assertEquals(inch(1, 1, 2), p.flattenTo)
      assertEquals(inch(23), p.length)
      assertEquals(FlatteningTool.PLANER, p.flattening.tool)
      assertEquals(inch(0, 1, 16), p.flattening.allowance)
    }
    assertEquals(inch(1, 5, 8), f.sliceThickness)
    assertEquals(mapOf("1A" to 12, "1B" to 12), f.slicesPerPanel)
    assertEquals(12, f.rows.size)
    f.rows.forEachIndexed { i, row ->
      assertEquals(i, row.index)
      assertEquals(RowKind.PLAIN, row.kind)
      assertEquals(inch(1, 1, 2), row.height)
      assertEquals(i % 2 == 1, row.rotated)
      assertEquals(Length.ZERO, row.shift)
      assertEquals(if (i % 2 == 1) listOf("1B", "1A") else listOf("1A", "1B"), row.panels)
    }
    assertEquals(inch(18), f.glueUpLength)
    assertEquals(inch(12, 1, 4), f.glueUpWidth)
    assertEquals(FlatteningMethod.ROUTER_SLED, f.finalFlatten)
    assertEquals(inch(0, 1, 8), f.finalAllowance)
  }

  @Test
  fun exampleB_edgeGrainStripes() {
    val widths =
      listOf(
        "walnut" to inch(1, 1, 2),
        "hard-maple" to inch(0, 3, 4),
        "cherry" to inch(1, 1, 2),
        "hard-maple" to inch(0, 3, 4),
        "cherry" to inch(1, 1, 2),
        "hard-maple" to inch(0, 3, 4),
        "walnut" to inch(1, 1, 2),
      )
    val f =
      ok(
        GrainMode.EDGE_GRAIN,
        Dimensions(inch(16), inch(8, 1, 4), inch(1, 1, 4)),
        PatternSpec.Stripes(widths.map { (s, w) -> Strip(s, w) }),
      )
    assertEquals(GrainMode.EDGE_GRAIN, f.mode)
    val p = f.panels.single()
    assertEquals("1", p.name)
    assertEquals(PanelKind.EDGE_GRAIN_BOARD, p.kind)
    assertEquals(
      listOf(
        inch(1, 5, 8),
        inch(0, 3, 4),
        inch(1, 1, 2),
        inch(0, 3, 4),
        inch(1, 1, 2),
        inch(0, 3, 4),
        inch(1, 5, 8),
      ),
      p.strips.map { it.millWidth },
    )
    assertEquals(widths.map { it.first }, p.strips.map { it.speciesId })
    assertTrue(p.strips.all { it.role == StripRole.PLAIN })
    assertEquals(inch(8, 1, 2), p.width)
    assertEquals(inch(1, 5, 16), p.millHeight)
    assertEquals(inch(1, 1, 4), p.flattenTo)
    assertEquals(inch(18), p.length)
    assertEquals(FlatteningTool.PLANER, p.flattening.tool)
    assertNull(f.sliceThickness)
    assertEquals(emptyMap(), f.slicesPerPanel)
    val row = f.rows.single()
    assertEquals(RowKind.PLAIN, row.kind)
    assertEquals(inch(16), row.height)
    assertEquals(listOf("1"), row.panels)
    assertEquals(inch(8, 1, 2), f.glueUpWidth)
    assertEquals(inch(18), f.glueUpLength)
    assertNull(f.finalFlatten)
    assertEquals(inch(0, 1, 16), f.finalAllowance)
  }

  @Test
  fun exampleC_endGrainBrick() {
    val f =
      ok(
        GrainMode.END_GRAIN,
        Dimensions(inch(17, 1, 4), inch(12), inch(1, 1, 2)),
        PatternSpec.Brick("cherry", "hard-maple", inch(3), inch(1, 1, 2), inch(0, 1, 4)),
      )
    assertEquals(listOf("1A", "1B", "MA", "MB"), f.panels.map { it.name })
    val brick = inch(3)
    val mortar = inch(0, 1, 4)
    val a = f.panel("1A")
    val b = f.panel("1B")
    assertEquals(listOf(brick, mortar, brick, mortar), a.strips.map { it.millWidth })
    assertEquals(listOf(brick, mortar, brick, mortar, brick), b.strips.map { it.millWidth })
    val roles = (a.strips + b.strips).map { it.role }
    assertEquals(List(9) { if (it % 2 == 0) StripRole.BRICK else StripRole.MORTAR }, roles)
    val species = (a.strips + b.strips).map { it.speciesId }
    assertEquals(List(9) { if (it % 2 == 0) "cherry" else "hard-maple" }, species)
    assertEquals(inch(6, 1, 2), a.width)
    assertEquals(inch(9, 1, 2), b.width)
    for (p in listOf(a, b)) {
      assertEquals(PanelKind.PANEL_1, p.kind)
      assertEquals(inch(1, 9, 16), p.millHeight)
      assertEquals(inch(1, 1, 2), p.flattenTo)
      assertEquals(inch(19, 1, 2), p.length)
    }
    for ((i, name) in listOf("MA", "MB").withIndex()) {
      val m = f.panel(name)
      assertEquals(PanelKind.MORTAR, m.kind)
      assertEquals(inch(8), m.width)
      assertEquals(listOf(PanelStrip("hard-maple", inch(8), -1 - i, StripRole.MORTAR)), m.strips)
      assertEquals(inch(0, 5, 16), m.millHeight)
      assertEquals(inch(0, 1, 4), m.flattenTo)
      assertEquals(inch(17, 3, 4), m.length)
    }
    assertEquals(inch(1, 5, 8), f.sliceThickness)
    assertEquals(mapOf("1A" to 10, "1B" to 10, "MA" to 9, "MB" to 9), f.slicesPerPanel)
    assertEquals(19, f.rows.size)
    val shifted = setOf(2, 6, 10, 14, 18)
    f.rows.forEachIndexed { i, row ->
      assertEquals(i, row.index)
      assertEquals(false, row.rotated)
      if (i % 2 == 0) {
        assertEquals(RowKind.BRICK, row.kind)
        assertEquals(inch(1, 1, 2), row.height)
        assertEquals(listOf("1A", "1B"), row.panels)
      } else {
        assertEquals(RowKind.MORTAR, row.kind)
        assertEquals(mortar, row.height)
        assertEquals(listOf("MA", "MB"), row.panels)
      }
      assertEquals(if (i in shifted) inch(1, 5, 8) else Length.ZERO, row.shift, "row $i shift")
    }
    assertEquals(inch(17, 1, 4), f.glueUpLength)
    assertEquals(inch(16), f.glueUpWidth)
    assertEquals(FlatteningMethod.ROUTER_SLED, f.finalFlatten)
    assertEquals(inch(0, 1, 8), f.finalAllowance)
  }

  @Test
  fun edgeGrainSplitBoardFlattensProud() {
    val f =
      ok(
        GrainMode.EDGE_GRAIN,
        Dimensions(inch(18), inch(15), inch(1, 1, 4)),
        PatternSpec.Stripes(
          List(20) { Strip(if (it % 2 == 0) "walnut" else "cherry", inch(0, 3, 4)) },
        ),
      )
    assertEquals(listOf("1A", "1B"), f.panels.map { it.name })
    assertTrue(f.panels.all { it.kind == PanelKind.EDGE_GRAIN_BOARD })
    assertTrue(f.panels.all { it.width <= inch(13) })
    assertEquals(20, f.panels.sumOf { it.strips.size })
    assertTrue(f.panels.all { it.flattenTo == inch(1, 9, 32) })
    assertTrue(f.panels.all { it.millHeight == inch(1, 5, 16) })
    assertEquals(listOf("1A", "1B"), f.rows.single().panels)
    assertEquals(inch(15, 1, 4), f.glueUpWidth)
  }

  @Test
  fun endGrainStripeRowsAreAllTheSameWayRound() {
    val f =
      ok(
        GrainMode.END_GRAIN,
        Dimensions(inch(16), inch(12), inch(1, 1, 2)),
        PatternSpec.Stripes(
          List(8) { Strip(if (it % 2 == 0) "walnut" else "cherry", inch(1, 1, 2)) },
        ),
      )
    assertEquals(11, f.rows.size)
    assertTrue(f.rows.none { it.rotated })
    assertTrue(f.rows.all { it.panels == listOf("1A", "1B") })
    assertEquals(mapOf("1A" to 11, "1B" to 11), f.slicesPerPanel)
  }

  @Test
  fun singleBrickRowHasNoMortarPanel() {
    val f =
      ok(
        GrainMode.END_GRAIN,
        Dimensions(inch(6), inch(12), inch(1, 1, 2)),
        PatternSpec.Brick("cherry", "hard-maple", inch(3), inch(4), inch(0, 1, 4)),
      )
    assertTrue(f.panels.none { it.kind == PanelKind.MORTAR })
    assertEquals(listOf("1A", "1B"), f.panels.map { it.name })
    assertEquals(mapOf("1A" to 1, "1B" to 1), f.slicesPerPanel)
    assertEquals(listOf(RowKind.BRICK), f.rows.map { it.kind })
  }

  @Test
  fun noValidSplitIsAnError() {
    val result =
      compile(
        GrainMode.END_GRAIN,
        Dimensions(inch(18), inch(14), inch(1, 1, 2)),
        PatternSpec.Stripes(listOf(Strip("walnut", inch(14)))),
      )
    assertEquals(FabricationResult.Failed(CompileError.NoValidSplit("1", inch(12))), result)
  }
}
