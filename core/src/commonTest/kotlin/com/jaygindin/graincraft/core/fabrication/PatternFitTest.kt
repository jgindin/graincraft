package com.jaygindin.graincraft.core.fabrication

import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.design.PatternSpec
import com.jaygindin.graincraft.core.design.Strip
import com.jaygindin.graincraft.core.units.FractionPrecision
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.LengthUnit
import com.jaygindin.graincraft.core.units.UnitFormatter
import com.jaygindin.graincraft.core.units.sum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PatternFitTest {
  private val fmt = UnitFormatter(LengthUnit.INCH, FractionPrecision.SIXTEENTH)

  private fun inch(w: Int, n: Int = 0, d: Int = 1) = Length.inches(w, n, d)

  private fun dims(l: Length, w: Length, t: Length = inch(1, 1, 2)) = Dimensions(l, w, t)

  private fun stripes(widths: List<Length>, rowHeight: Length = inch(1, 1, 2)) =
    PatternSpec.Stripes(widths.mapIndexed { i, w -> Strip("s$i", w) }, rowHeight)

  private fun fit(mode: GrainMode, target: Dimensions, pattern: PatternSpec): Fit {
    val result = fitPattern(mode, target, pattern, fmt)
    assertTrue(result is FitResult.Ok, "expected Ok but was $result")
    return result.fit
  }

  private fun fail(mode: GrainMode, target: Dimensions, pattern: PatternSpec): CompileError {
    val result = fitPattern(mode, target, pattern, fmt)
    assertTrue(result is FitResult.Failed, "expected Failed but was $result")
    return result.error
  }

  @Test
  fun stripesScaleToWidth() {
    val f =
      fit(
        GrainMode.EDGE_GRAIN,
        dims(inch(16), inch(10)),
        stripes(listOf(inch(2), inch(1), inch(2))),
      )
    assertEquals(listOf(inch(4), inch(2), inch(4)), f.columns.map { it.finishedWidth })
    assertEquals(listOf("s0", "s1", "s2"), f.columns.map { it.speciesId })
    assertTrue(!f.rotateAlternateRows)
  }

  @Test
  fun stripesRemainderGoesToWidest() {
    val f =
      fit(
        GrainMode.EDGE_GRAIN,
        dims(inch(16), inch(10)),
        stripes(listOf(inch(1), inch(1), inch(1))),
      )
    val widths = f.columns.map { it.finishedWidth }
    assertEquals(listOf(inch(3, 11, 32), inch(3, 21, 64), inch(3, 21, 64)), widths)
    assertEquals(inch(10), widths.sum())
  }

  @Test
  fun stripesAlreadyFittingAreUnchanged() {
    val widths =
      listOf(
        inch(1, 1, 2),
        inch(0, 3, 4),
        inch(1, 1, 2),
        inch(0, 3, 4),
        inch(1, 1, 2),
        inch(0, 3, 4),
        inch(1, 1, 2),
      )
    val f =
      fit(
        GrainMode.EDGE_GRAIN,
        dims(inch(16), inch(8, 1, 4), inch(1, 1, 4)),
        stripes(widths),
      )
    assertEquals(widths, f.columns.map { it.finishedWidth })
    assertTrue(f.columns.all { it.role == StripRole.PLAIN })
    assertEquals(dims(inch(16), inch(8, 1, 4), inch(1, 1, 4)), f.actual)
    assertTrue(f.notes.isEmpty())
  }

  @Test
  fun edgeGrainIsOnePlainRowOfFullLength() {
    val f = fit(GrainMode.EDGE_GRAIN, dims(inch(16), inch(10)), stripes(listOf(inch(2), inch(1))))
    assertEquals(listOf(FitRow(RowKind.PLAIN, inch(16))), f.rows)
  }

  @Test
  fun endGrainStripeRows() {
    val f = fit(GrainMode.END_GRAIN, dims(inch(16), inch(10)), stripes(listOf(inch(2), inch(1))))
    assertEquals(11, f.rows.size)
    val share = inch(16) / 11
    assertTrue(f.rows.all { it.kind == RowKind.PLAIN })
    assertTrue(f.rows.all { it.height.nanometres - share.nanometres in -1L..1L })
  }

  @Test
  fun checkerboardExampleA() {
    val pattern = PatternSpec.Checkerboard("walnut", "maple", inch(1, 1, 2))
    val f = fit(GrainMode.END_GRAIN, dims(inch(18), inch(12)), pattern)
    assertEquals(8, f.columns.size)
    assertTrue(f.columns.all { it.finishedWidth == inch(1, 1, 2) })
    assertEquals(
      listOf("walnut", "maple", "walnut", "maple", "walnut", "maple", "walnut", "maple"),
      f.columns.map { it.speciesId },
    )
    assertEquals(12, f.rows.size)
    assertTrue(f.rows.all { it.height == inch(1, 1, 2) })
    assertEquals(dims(inch(18), inch(12)), f.actual)
    assertTrue(f.notes.isEmpty())
    assertTrue(f.rotateAlternateRows)
  }

  @Test
  fun checkerboardRoundsToEvenColumns() {
    val pattern = PatternSpec.Checkerboard("a", "b", inch(1, 3, 4))
    val f = fit(GrainMode.END_GRAIN, dims(inch(18), inch(12)), pattern)
    assertEquals(6, f.columns.size)
    assertTrue(f.columns.all { it.finishedWidth == inch(2) })
    assertEquals(9, f.rows.size)
    assertEquals(inch(18), f.actual.length)
  }

  @Test
  fun checkerboardReportsActualLength() {
    val pattern = PatternSpec.Checkerboard("a", "b", inch(1, 1, 2))
    val f = fit(GrainMode.END_GRAIN, dims(inch(17), inch(12)), pattern)
    assertEquals(8, f.columns.size)
    assertEquals(11, f.rows.size)
    assertEquals(inch(16, 1, 2), f.actual.length)
    assertEquals(1, f.notes.size)
    assertTrue("16-1/2\"" in f.notes[0] && "17\"" in f.notes[0], f.notes[0])
    assertTrue("1/2\" under" in f.notes[0], f.notes[0])
  }

  @Test
  fun checkerboardCellLargerThanBoardClampsToTwoColumns() {
    val pattern = PatternSpec.Checkerboard("a", "b", inch(20))
    val f = fit(GrainMode.END_GRAIN, dims(inch(18), inch(12)), pattern)
    assertEquals(2, f.columns.size)
    assertTrue(f.columns.all { it.finishedWidth == inch(6) })
  }

  @Test
  fun brickExampleC() {
    val pattern = PatternSpec.Brick("cherry", "maple", inch(3), inch(1, 1, 2), inch(0, 1, 4))
    val f = fit(GrainMode.END_GRAIN, dims(inch(17, 1, 4), inch(12)), pattern)
    assertEquals(19, f.rows.size)
    f.rows.forEachIndexed { i, row ->
      if (i % 2 == 0) {
        assertEquals(FitRow(RowKind.BRICK, inch(1, 1, 2)), row)
      } else {
        assertEquals(FitRow(RowKind.MORTAR, inch(0, 1, 4)), row)
      }
    }
    assertEquals(inch(17, 1, 4), f.rows.map { it.height }.sum())
    assertEquals(inch(17, 1, 4), f.actual.length)
    assertEquals(
      listOf(
        FitStrip("cherry", inch(3), StripRole.BRICK),
        FitStrip("maple", inch(0, 1, 4), StripRole.MORTAR),
      ),
      f.columns,
    )
    assertTrue(f.notes.isEmpty())
  }

  @Test
  fun brickWiderThanBoardIsAllowed() {
    val pattern = PatternSpec.Brick("c", "m", inch(30), inch(1, 1, 2), inch(0, 1, 4))
    assertTrue(
      fitPattern(GrainMode.END_GRAIN, dims(inch(18), inch(12)), pattern, fmt) is FitResult.Ok,
    )
  }

  @Test
  fun emptyStripsFail() {
    assertEquals(
      CompileError.NoStrips,
      fail(GrainMode.EDGE_GRAIN, dims(inch(16), inch(8)), PatternSpec.Stripes(emptyList())),
    )
  }

  @Test
  fun zeroWidthStripFails() {
    assertEquals(
      CompileError.NonPositiveWidth(1),
      fail(GrainMode.EDGE_GRAIN, dims(inch(16), inch(8)), stripes(listOf(inch(1), Length.ZERO))),
    )
  }

  @Test
  fun checkerboardInEdgeGrainFails() {
    val pattern = PatternSpec.Checkerboard("a", "b", inch(1))
    assertEquals(
      CompileError.PatternNotAllowedInEdgeGrain,
      fail(GrainMode.EDGE_GRAIN, dims(inch(16), inch(8)), pattern),
    )
  }

  @Test
  fun brickInEdgeGrainFails() {
    val pattern = PatternSpec.Brick("c", "m", inch(3), inch(1), inch(0, 1, 4))
    assertEquals(
      CompileError.PatternNotAllowedInEdgeGrain,
      fail(GrainMode.EDGE_GRAIN, dims(inch(16), inch(8)), pattern),
    )
  }

  @Test
  fun outOfRangeLengthWidthThickness() {
    val p = stripes(listOf(inch(1)))
    assertEquals(
      CompileError.OutOfRange("length", inch(6), inch(36)),
      fail(GrainMode.END_GRAIN, dims(inch(40), inch(8)), p),
    )
    assertEquals(
      CompileError.OutOfRange("width", inch(4), inch(24)),
      fail(GrainMode.END_GRAIN, dims(inch(16), inch(3)), p),
    )
    assertEquals(
      CompileError.OutOfRange("thickness", inch(0, 3, 4), inch(3)),
      fail(GrainMode.END_GRAIN, dims(inch(16), inch(8), inch(0, 1, 2)), p),
    )
  }

  @Test
  fun rangeErrorsReportLengthFirst() {
    assertEquals(
      CompileError.OutOfRange("length", inch(6), inch(36)),
      fail(GrainMode.END_GRAIN, dims(inch(2), inch(30), inch(5)), stripes(listOf(inch(1)))),
    )
  }

  @Test
  fun endGrainRowsSumToLengthExactly() {
    val f = fit(GrainMode.END_GRAIN, dims(inch(16), inch(10)), stripes(listOf(inch(2), inch(1))))
    assertEquals(inch(16), f.rows.map { it.height }.sum())
    assertEquals(inch(16), f.actual.length)
  }

  @Test
  fun checkerboardColumnsSumToWidthExactly() {
    val pattern = PatternSpec.Checkerboard("a", "b", inch(1, 3, 4))
    val f = fit(GrainMode.END_GRAIN, dims(inch(10), inch(10)), pattern)
    assertEquals(6, f.columns.size)
    assertEquals(inch(10), f.columns.map { it.finishedWidth }.sum())
    assertEquals(f.rows.map { it.height }.sum(), f.actual.length)
    assertEquals(inch(10), f.actual.width)
  }

  @Test
  fun brickRowsSumToLengthWhenHeightDoesNotDivideEvenly() {
    val pattern = PatternSpec.Brick("c", "m", inch(3), inch(1, 1, 2), inch(0, 1, 4))
    val f = fit(GrainMode.END_GRAIN, dims(inch(16), inch(12)), pattern)
    assertEquals(inch(16), f.rows.map { it.height }.sum())
    assertEquals(inch(16), f.actual.length)
  }

  @Test
  fun nonPositiveDimensionsFail() {
    val d = dims(inch(16), inch(12))
    val z = Length.ZERO
    fun e(field: String) = CompileError.NonPositiveDimension(field)
    assertEquals(
      e("targetCell"),
      fail(GrainMode.END_GRAIN, d, PatternSpec.Checkerboard("a", "b", z)),
    )
    assertEquals(
      e("targetBrickWidth"),
      fail(GrainMode.END_GRAIN, d, PatternSpec.Brick("c", "m", z, inch(1), inch(0, 1, 4))),
    )
    assertEquals(
      e("targetBrickHeight"),
      fail(GrainMode.END_GRAIN, d, PatternSpec.Brick("c", "m", inch(3), z, inch(0, 1, 4))),
    )
    assertEquals(
      e("mortarWidth"),
      fail(GrainMode.END_GRAIN, d, PatternSpec.Brick("c", "m", inch(3), inch(1), z)),
    )
    assertEquals(
      e("rowHeight"),
      fail(GrainMode.END_GRAIN, d, stripes(listOf(inch(1)), rowHeight = z)),
    )
  }

  private val elementRange = inch(0, 1, 4)..inch(36)

  @Test
  fun oneMillimetreCellFailsWithoutBuildingTheBoard() {
    val d = dims(inch(18), inch(12))
    val pattern = PatternSpec.Checkerboard("a", "b", Length.millimetres(1.0))
    assertEquals(
      CompileError.OutOfRange("targetCell", elementRange.start, elementRange.endInclusive),
      fail(GrainMode.END_GRAIN, d, pattern),
    )
  }

  @Test
  fun sixtyFourthInchCellFails() {
    val pattern = PatternSpec.Checkerboard("a", "b", inch(0, 1, 64))
    assertTrue(
      fail(GrainMode.END_GRAIN, dims(inch(36), inch(24)), pattern) is CompileError.OutOfRange,
    )
  }

  @Test
  fun quarterInchCellIsAllowed() {
    fit(
      GrainMode.END_GRAIN,
      dims(inch(18), inch(12)),
      PatternSpec.Checkerboard("a", "b", inch(0, 1, 4)),
    )
  }

  @Test
  fun cellOverThirtySixInchesFails() {
    val pattern = PatternSpec.Checkerboard("a", "b", inch(37))
    assertEquals(
      CompileError.OutOfRange("targetCell", elementRange.start, elementRange.endInclusive),
      fail(GrainMode.END_GRAIN, dims(inch(18), inch(12)), pattern),
    )
  }

  @Test
  fun microscopicStripFails() {
    val tiny = Length.inchesApprox(1e-6)
    assertEquals(
      CompileError.StripOutOfRange(0, inch(0, 1, 32), inch(36)),
      fail(GrainMode.EDGE_GRAIN, dims(inch(16), inch(8)), stripes(listOf(tiny))),
    )
  }

  @Test
  fun threeHundredInchStripFailsInsteadOfOverflowing() {
    assertEquals(
      CompileError.StripOutOfRange(1, inch(0, 1, 32), inch(36)),
      fail(GrainMode.EDGE_GRAIN, dims(inch(16), inch(24)), stripes(listOf(inch(1), inch(300)))),
    )
  }

  @Test
  fun thirtySecondInchStripIsAllowed() {
    val f =
      fit(
        GrainMode.EDGE_GRAIN,
        dims(inch(16), inch(8)),
        stripes(listOf(inch(3), inch(0, 1, 32), inch(3))),
      )
    assertTrue(f.columns.all { it.finishedWidth >= inch(0, 1, 32) })
  }

  @Test
  fun stripThatScalesBelowTheMinimumFails() {
    // 1/32" next to two 36" strips, scaled down to 4" wide, would round to nothing.
    assertEquals(
      CompileError.StripOutOfRange(1, inch(0, 1, 32), inch(36)),
      fail(
        GrainMode.EDGE_GRAIN,
        dims(inch(16), inch(4)),
        stripes(listOf(inch(36), inch(0, 1, 32), inch(36))),
      ),
    )
  }

  @Test
  fun brickAndRowSizesAreBounded() {
    val d = dims(inch(16), inch(12))
    val q = inch(0, 1, 8)
    fun e(field: String) =
      CompileError.OutOfRange(field, elementRange.start, elementRange.endInclusive)
    assertEquals(
      e("targetBrickWidth"),
      fail(GrainMode.END_GRAIN, d, PatternSpec.Brick("c", "m", q, inch(1), inch(0, 1, 4))),
    )
    assertEquals(
      e("targetBrickHeight"),
      fail(GrainMode.END_GRAIN, d, PatternSpec.Brick("c", "m", inch(3), q, inch(0, 1, 4))),
    )
    assertEquals(
      CompileError.OutOfRange("mortarWidth", inch(0, 1, 32), inch(36)),
      fail(GrainMode.END_GRAIN, d, PatternSpec.Brick("c", "m", inch(3), inch(1), inch(0, 1, 64))),
    )
    assertEquals(
      CompileError.OutOfRange("mortarWidth", inch(0, 1, 32), inch(36)),
      fail(GrainMode.END_GRAIN, d, PatternSpec.Brick("c", "m", inch(3), inch(1), inch(40))),
    )
    assertEquals(
      e("rowHeight"),
      fail(GrainMode.END_GRAIN, d, stripes(listOf(inch(1)), rowHeight = q)),
    )
  }
}
