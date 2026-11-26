package com.jaygindin.graincraft.core.layout

import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.design.PatternSpec
import com.jaygindin.graincraft.core.design.Strip
import com.jaygindin.graincraft.core.fabrication.FabricationResult
import com.jaygindin.graincraft.core.fabrication.FitResult
import com.jaygindin.graincraft.core.fabrication.compileFabrication
import com.jaygindin.graincraft.core.fabrication.fitPattern
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.FractionPrecision
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.LengthUnit
import com.jaygindin.graincraft.core.units.UnitFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LayoutBuilderTest {
  private val fmt = UnitFormatter(LengthUnit.INCH, FractionPrecision.SIXTEENTH)
  private val profile = ShopProfile()

  private fun inch(w: Int, n: Int = 0, d: Int = 1) = Length.inches(w, n, d)

  private val targetA = Dimensions(inch(18), inch(12), inch(1, 1, 2))
  private val targetB = Dimensions(inch(16), inch(8, 1, 4), inch(1, 1, 4))
  private val targetC = Dimensions(inch(17, 1, 4), inch(12), inch(1, 1, 2))

  private val patternA = PatternSpec.Checkerboard("walnut", "hard-maple", inch(1, 1, 2))
  private val patternB =
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
    )
  private val patternC =
    PatternSpec.Brick("cherry", "hard-maple", inch(3), inch(1, 1, 2), inch(0, 1, 4))

  private fun layout(mode: GrainMode, target: Dimensions, pattern: PatternSpec): BoardLayout {
    val fit = fitPattern(mode, target, pattern, fmt)
    assertTrue(fit is FitResult.Ok, "expected a fit but was $fit")
    val fabrication = compileFabrication(mode, target, fit.fit, profile)
    assertTrue(fabrication is FabricationResult.Ok, "expected Ok but was $fabrication")
    return buildLayout(fabrication.fabrication, target, profile)
  }

  private val layoutA by lazy { layout(GrainMode.END_GRAIN, targetA, patternA) }
  private val layoutB by lazy { layout(GrainMode.EDGE_GRAIN, targetB, patternB) }
  private val layoutC by lazy { layout(GrainMode.END_GRAIN, targetC, patternC) }

  private val Piece.minX
    get() = polygon.minOf { it.x }

  private val Piece.maxX
    get() = polygon.maxOf { it.x }

  private val Piece.minY
    get() = polygon.minOf { it.y }

  private val Piece.maxY
    get() = polygon.maxOf { it.y }

  private val Piece.width
    get() = maxY - minY

  /** The row a piece of a slice-by-slice board belongs to, by where it starts along the length. */
  private fun BoardLayout.row(x: Length): List<Piece> =
    pieces.filter { it.minX == x }.sortedBy { it.minY }

  private fun assertTiles(layout: BoardLayout) {
    val pieces = layout.pieces
    for (p in pieces) {
      assertEquals(4, p.polygon.size, "pieces are rectangles")
      assertTrue(p.minX >= Length.ZERO && p.maxX <= layout.size.length, "x in board: $p")
      assertTrue(p.minY >= Length.ZERO && p.maxY <= layout.size.width, "y in board: $p")
      assertTrue(p.maxX > p.minX && p.maxY > p.minY, "non-empty: $p")
    }
    val area = pieces.sumOf { (it.maxX - it.minX).nanometres * (it.maxY - it.minY).nanometres }
    assertEquals(layout.size.length.nanometres * layout.size.width.nanometres, area)
    for (i in pieces.indices) {
      for (j in i + 1 until pieces.size) {
        val a = pieces[i]
        val b = pieces[j]
        val overlap = a.minX < b.maxX && b.minX < a.maxX && a.minY < b.maxY && b.minY < a.maxY
        assertTrue(!overlap, "pieces overlap: $a and $b")
      }
    }
  }

  @Test
  fun piecesTileTheBoard() {
    assertEquals(targetA, layoutA.size)
    assertTiles(layoutA)
    assertEquals(targetB, layoutB.size)
    assertTiles(layoutB)
    assertEquals(targetC, layoutC.size)
    assertTiles(layoutC)
  }

  @Test
  fun checkerboardAlternates() {
    val row0 = layoutA.row(Length.ZERO)
    val row1 = layoutA.row(inch(1, 1, 2))
    assertEquals(8, row0.size)
    assertEquals("walnut", row0[0].speciesId)
    assertEquals("hard-maple", row0[1].speciesId)
    assertEquals("hard-maple", row1[0].speciesId)
    for (p in layoutA.pieces) {
      val odd = p.sliceIndex?.let { it % 2 == 1 }
      assertEquals(odd, p.flipped, "piece $p")
    }
    assertTrue(row1.all { it.flipped && it.sliceIndex == 1 })
    assertEquals(listOf("1B", "1B", "1B", "1B", "1A", "1A", "1A", "1A"), row1.map { it.panelName })
  }

  @Test
  fun sameStripSameSource() {
    val row0 = layoutA.row(Length.ZERO)
    val row1 = layoutA.row(inch(1, 1, 2))
    val row2 = layoutA.row(inch(3))
    assertEquals(row0[0].sourceStripIndex, row2[0].sourceStripIndex)
    assertEquals(0, row0[0].sourceStripIndex)
    assertEquals(7, row1[0].sourceStripIndex)
    assertEquals((0..7).toList(), row0.map { it.sourceStripIndex })
    assertEquals((7 downTo 0).toList(), row1.map { it.sourceStripIndex })
  }

  @Test
  fun outerStripsAreTrimmedToFinishedWidth() {
    for (x in listOf(Length.ZERO, inch(1, 1, 2))) {
      val row = layoutA.row(x)
      assertEquals(inch(1, 1, 2), row.first().width)
      assertEquals(inch(1, 1, 2), row.last().width)
      assertEquals(Length.ZERO, row.first().minY)
      assertEquals(inch(12), row.last().maxY)
    }
  }

  @Test
  fun brickRowsShift() {
    val row0 = layoutC.row(Length.ZERO)
    val row1 = layoutC.row(inch(1, 1, 2))
    val row2 = layoutC.row(inch(1, 3, 4))
    fun mortarStarts(row: List<Piece>) = row.filter { it.speciesId == "hard-maple" }.map { it.minY }
    assertEquals(listOf(inch(1), inch(4, 1, 4), inch(7, 1, 2), inch(10, 3, 4)), mortarStarts(row0))
    assertEquals(listOf(inch(2, 5, 8), inch(5, 7, 8), inch(9, 1, 8)), mortarStarts(row2))
    assertTrue(row0.all { it.shiftY == Length.ZERO && it.sliceIndex == 0 })
    assertTrue(row2.all { it.shiftY == inch(1, 5, 8) && it.sliceIndex == 1 })
    assertTrue(row0.none { it.flipped } && row2.none { it.flipped })
    // Panel 1 starts at y = -2, so the first and last bricks of row 0 are trimmed to 1.
    assertEquals(inch(1), row0.first().width)
    assertEquals(inch(1), row0.last().width)

    // The mortar row is maple across the full width, one piece per mortar sub-panel slice.
    assertTrue(row1.all { it.speciesId == "hard-maple" && it.sliceIndex == 0 })
    assertTrue(row1.all { it.maxX - it.minX == inch(0, 1, 4) })
    assertEquals(Length.ZERO, row1.first().minY)
    assertEquals(inch(12), row1.last().maxY)
    assertEquals(row1.size, row1.zipWithNext().count { (a, b) -> a.maxY == b.minY } + 1)
    assertEquals(listOf("MA", "MB"), row1.map { it.panelName })
    assertEquals(listOf(-1, -2), row1.map { it.sourceStripIndex })
  }

  @Test
  fun brickSourceIsThePanelOnePosition() {
    val row0 = layoutC.row(Length.ZERO)
    // Panel 1 is B M B M B M B M B; the trimmed board shows strips 0..8 in order.
    assertEquals((0..8).toList(), row0.map { it.sourceStripIndex })
    assertEquals(
      listOf("1A", "1A", "1A", "1A", "1B", "1B", "1B", "1B", "1B"),
      row0.map { it.panelName },
    )
  }

  @Test
  fun fibreMatchesGrainMode() {
    assertTrue(layoutB.pieces.all { it.fibre == Fibre.X })
    assertTrue(layoutA.pieces.all { it.fibre == Fibre.Z })
    assertTrue(layoutC.pieces.all { it.fibre == Fibre.Z })
  }

  @Test
  fun edgeGrainRunsTheFullLength() {
    assertEquals(7, layoutB.pieces.size)
    assertTrue(layoutB.pieces.all { it.minX == Length.ZERO && it.maxX == inch(16) })
    assertTrue(layoutB.pieces.all { it.sliceIndex == null && !it.flipped })
    assertEquals((0..6).toList(), layoutB.pieces.sortedBy { it.minY }.map { it.sourceStripIndex })
    assertTrue(layoutB.pieces.all { it.laminationIndex == 0 })
  }

  @Test
  fun wideEdgeGrainStripIsLaminated() {
    val target = Dimensions(inch(16), inch(5, 1, 2), inch(1, 1, 4))
    val pattern =
      PatternSpec.Stripes(
        listOf(
          Strip("walnut", inch(1, 1, 2)),
          Strip("hard-maple", inch(2, 1, 2)),
          Strip("walnut", inch(1, 1, 2)),
        ),
      )
    val layout = layout(GrainMode.EDGE_GRAIN, target, pattern)
    assertTiles(layout)
    val wide = layout.pieces.filter { it.sourceStripIndex == 1 }.sortedBy { it.minY }
    assertEquals(listOf(inch(1, 1, 4), inch(1, 1, 4)), wide.map { it.width })
    assertEquals(listOf(0, 1), wide.map { it.laminationIndex })
    assertEquals(inch(1, 1, 2), wide[0].minY)
    assertEquals(4, layout.pieces.size)
  }

  @Test
  fun outerStripIsLaminatedOnItsMilledWidth() {
    // Finished 1-3/4 is milled 1-7/8 with the side trim, too thick for one 8/4 board.
    val target = Dimensions(inch(16), inch(5), inch(1, 1, 4))
    val pattern =
      PatternSpec.Stripes(
        listOf(
          Strip("walnut", inch(1, 3, 4)),
          Strip("hard-maple", inch(1, 1, 2)),
          Strip("walnut", inch(1, 3, 4)),
        ),
      )
    val layout = layout(GrainMode.EDGE_GRAIN, target, pattern)
    assertTiles(layout)
    val outer = layout.pieces.filter { it.sourceStripIndex == 0 }.sortedBy { it.minY }
    assertEquals(listOf(0, 1), outer.map { it.laminationIndex })
    // The glue line is at half the milled width from the untrimmed edge: 15/16 - 1/8.
    assertEquals(inch(0, 13, 16), outer[0].maxY)
    assertEquals(inch(0, 13, 16), outer[1].minY)
    assertEquals(inch(1, 3, 4), outer[1].maxY)
    val last = layout.pieces.filter { it.sourceStripIndex == 2 }.sortedBy { it.minY }
    assertEquals(listOf(0, 1), last.map { it.laminationIndex })
    assertEquals(inch(4, 3, 16), last[0].maxY)
    assertEquals(1, layout.pieces.count { it.sourceStripIndex == 1 })
  }

  @Test
  fun oddLaminationGivesTheExtraNanometreToTheFirstHalf() {
    // The cherry strip is milled 2-9/64 (2-1/64 plus the side trim) and starts at y = 2; that is
    // an odd number of nanometres, since 1/64" is 396,875 nm.
    val target = Dimensions(inch(16), inch(4, 1, 64), inch(1, 1, 4))
    val pattern =
      PatternSpec.Stripes(listOf(Strip("walnut", inch(2)), Strip("cherry", inch(2, 1, 64))))
    val layout = layout(GrainMode.EDGE_GRAIN, target, pattern)
    assertTiles(layout)
    val milled = inch(2, 9, 64).nanometres
    assertEquals(1L, milled % 2)
    val cherry = layout.pieces.filter { it.sourceStripIndex == 1 }.sortedBy { it.minY }
    assertEquals(listOf(0, 1), cherry.map { it.laminationIndex })
    assertEquals(inch(2), cherry[0].minY)
    assertEquals(Length((milled + 1) / 2), cherry[0].width)
    assertEquals(inch(2) + Length((milled + 1) / 2), cherry[1].minY)
  }
}
