package com.jaygindin.graincraft.core.fabrication

import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.design.PatternSpec
import com.jaygindin.graincraft.core.units.Length

private val LENGTH_RANGE = Length.inches(6)..Length.inches(36)
private val WIDTH_RANGE = Length.inches(4)..Length.inches(24)
private val THICKNESS_RANGE = Length.inches(0, 3, 4)..Length.inches(3)

internal fun validate(mode: GrainMode, target: Dimensions, pattern: PatternSpec): CompileError? =
  rangeError("length", target.length, LENGTH_RANGE)
    ?: rangeError("width", target.width, WIDTH_RANGE)
    ?: rangeError("thickness", target.thickness, THICKNESS_RANGE)
    ?: patternError(mode, pattern)

private fun patternError(mode: GrainMode, pattern: PatternSpec): CompileError? =
  when {
    mode == GrainMode.EDGE_GRAIN && pattern !is PatternSpec.Stripes ->
      CompileError.PatternNotAllowedInEdgeGrain
    pattern is PatternSpec.Stripes -> stripesError(mode, pattern)
    pattern is PatternSpec.Checkerboard -> positive("targetCell", pattern.targetCell)
    pattern is PatternSpec.Brick -> brickError(pattern)
    else -> null
  }

private fun positive(field: String, value: Length): CompileError? =
  if (value > Length.ZERO) null else CompileError.NonPositiveDimension(field)

private fun brickError(pattern: PatternSpec.Brick): CompileError? =
  positive("targetBrickWidth", pattern.targetBrickWidth)
    ?: positive("targetBrickHeight", pattern.targetBrickHeight)
    ?: positive("mortarWidth", pattern.mortarWidth)

private fun stripesError(mode: GrainMode, pattern: PatternSpec.Stripes): CompileError? {
  if (pattern.strips.isEmpty()) return CompileError.NoStrips
  val bad = pattern.strips.indexOfFirst { it.width <= Length.ZERO }
  return when {
    bad >= 0 -> CompileError.NonPositiveWidth(bad)
    mode == GrainMode.END_GRAIN -> positive("rowHeight", pattern.rowHeight)
    else -> null
  }
}

private fun rangeError(field: String, value: Length, range: ClosedRange<Length>): CompileError? =
  if (value in range) null else CompileError.OutOfRange(field, range.start, range.endInclusive)
