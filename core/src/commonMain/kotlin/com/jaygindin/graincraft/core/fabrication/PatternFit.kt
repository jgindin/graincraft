package com.jaygindin.graincraft.core.fabrication

import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.design.PatternSpec
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.UnitFormatter
import com.jaygindin.graincraft.core.units.sum
import kotlin.math.floor

enum class StripRole {
  PLAIN,
  BRICK,
  MORTAR,
}

/** One strip across the width; `finishedWidth` is the visible width, before any trim allowance. */
data class FitStrip(val speciesId: String, val finishedWidth: Length, val role: StripRole)

enum class RowKind {
  PLAIN,
  BRICK,
  MORTAR,
}

/** One row along the length. */
data class FitRow(val kind: RowKind, val height: Length)

data class Fit(
  val columns: List<FitStrip>,
  val rows: List<FitRow>,
  val actual: Dimensions,
  val notes: List<String>,
)

sealed interface FitResult {
  data class Ok(val fit: Fit) : FitResult

  data class Failed(val error: CompileError) : FitResult
}

/** Adjusts the pattern's elements to the target size and reports the size actually reached. */
fun fitPattern(
  mode: GrainMode,
  target: Dimensions,
  pattern: PatternSpec,
  formatter: UnitFormatter,
): FitResult {
  validate(mode, target, pattern)?.let {
    return FitResult.Failed(it)
  }
  return when (pattern) {
    is PatternSpec.Stripes -> fitStripes(mode, target, pattern)
    is PatternSpec.Checkerboard -> FitResult.Ok(fitCheckerboard(target, pattern, formatter))
    is PatternSpec.Brick -> FitResult.Ok(fitBrick(target, pattern))
  }
}

/**
 * Splits `total` into `n` parts that sum to it exactly; the first parts take the extra nanometres.
 */
private fun splitEvenly(total: Length, n: Int): List<Length> {
  val base = total.nanometres.floorDiv(n.toLong())
  val extra = (total.nanometres - base * n).toInt()
  return List(n) { i -> Length(if (i < extra) base + 1 else base) }
}

private fun fitStripes(
  mode: GrainMode,
  target: Dimensions,
  pattern: PatternSpec.Stripes,
): FitResult {
  val widths = scaleToWidth(pattern.strips.map { it.width }, target.width)
  val columns =
    pattern.strips.mapIndexed { i, s -> FitStrip(s.speciesId, widths[i], StripRole.PLAIN) }
  val rows =
    if (mode == GrainMode.EDGE_GRAIN) {
      listOf(FitRow(RowKind.PLAIN, target.length))
    } else {
      val n = roundedCount(target.length, pattern.rowHeight)
      splitEvenly(target.length, n).map { FitRow(RowKind.PLAIN, it) }
    }
  return FitResult.Ok(Fit(columns, rows, target, emptyList()))
}

/**
 * Scales widths proportionally so they sum to `total`, rounds each to 1/64" and gives the rounding
 * remainder to the widest strip (the first of equals).
 */
private fun scaleToWidth(widths: List<Length>, total: Length): List<Length> {
  val sum = widths.sum().nanometres
  val scaled = widths.map { w ->
    Length((2 * w.nanometres * total.nanometres + sum).floorDiv(2 * sum)).roundToSixtyFourth()
  }
  val remainder = total - scaled.sum()
  if (remainder == Length.ZERO) return scaled
  val widest = scaled.indices.maxByOrNull { scaled[it].nanometres }!!
  // maxByOrNull keeps the first of equal maxima, so ties go to the earlier strip.
  return scaled.mapIndexed { i, w -> if (i == widest) w + remainder else w }
}

private fun fitCheckerboard(
  target: Dimensions,
  pattern: PatternSpec.Checkerboard,
  formatter: UnitFormatter,
): Fit {
  val cell = pattern.targetCell.nanometres
  val m = maxOf(2, 2 * roundHalfUp(target.width.nanometres.toDouble() / (2.0 * cell)))
  val s = target.width / m
  val n = roundedCount(target.length, s)
  // Column widths sum to the width exactly; they differ from `s` by at most a nanometre.
  val columns =
    splitEvenly(target.width, m).mapIndexed { i, w ->
      FitStrip(if (i % 2 == 0) pattern.speciesA else pattern.speciesB, w, StripRole.PLAIN)
    }
  val rows = List(n) { FitRow(RowKind.PLAIN, s) }
  val actualLength = s * n
  val notes = mutableListOf<String>()
  // Nanometre drift from rounding `s` is not a difference worth reporting.
  if (actualLength.roundToSixtyFourth() != target.length) {
    val diff =
      if (actualLength > target.length) actualLength - target.length
      else target.length - actualLength
    val direction = if (actualLength > target.length) "over" else "under"
    notes +=
      "Length is ${formatter.format(actualLength)} (target ${formatter.format(target.length)}), " +
        "${formatter.format(diff)} $direction target, to keep squares square"
  }
  return Fit(columns, rows, target.copy(length = actualLength), notes)
}

private fun fitBrick(target: Dimensions, pattern: PatternSpec.Brick): Fit {
  val m = pattern.mortarWidth
  val period = (target.length + m).nanometres.toDouble()
  val nb = maxOf(1, roundHalfUp(period / (pattern.targetBrickHeight + m).nanometres))
  val heights = splitEvenly(target.length - m * (nb - 1), nb)
  val columns =
    listOf(
      FitStrip(pattern.brickSpecies, pattern.targetBrickWidth, StripRole.BRICK),
      FitStrip(pattern.mortarSpecies, m, StripRole.MORTAR),
    )
  val rows =
    List(2 * nb - 1) { i ->
      if (i % 2 == 0) FitRow(RowKind.BRICK, heights[i / 2]) else FitRow(RowKind.MORTAR, m)
    }
  // Brick width is the target as given; the width is filled by trimming later.
  return Fit(columns, rows, target, emptyList())
}

private fun roundedCount(length: Length, element: Length): Int =
  maxOf(1, roundHalfUp(length.nanometres.toDouble() / element.nanometres))

private const val HALF = 0.5

private fun roundHalfUp(x: Double): Int = floor(x + HALF).toInt()
