package com.jaygindin.graincraft.core.fabrication

import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.design.PatternSpec
import com.jaygindin.graincraft.core.units.Length

private val LENGTH_RANGE = Length.inches(6)..Length.inches(36)
private val WIDTH_RANGE = Length.inches(4)..Length.inches(24)
private val THICKNESS_RANGE = Length.inches(0, 3, 4)..Length.inches(3)

/**
 * Bounds on every pattern element. The lower bounds keep the piece count (and the memory a layout
 * needs) small; the upper bound keeps every product in the fitting arithmetic inside a `Long`.
 * Strips and mortar may go down to 1/32", so the auditor's "under 1/8"" warning still applies.
 */
private val MAX_ELEMENT = Length.inches(36)
internal val STRIP_RANGE = Length.inches(0, 1, 32)..MAX_ELEMENT
private val BLOCK_RANGE = Length.inches(0, 1, 4)..MAX_ELEMENT

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
    pattern is PatternSpec.Checkerboard -> element("targetCell", pattern.targetCell, BLOCK_RANGE)
    pattern is PatternSpec.Brick -> brickError(pattern)
    else -> null
  }

/** Zero or less is reported as such; anything else must lie in `range`. */
private fun element(field: String, value: Length, range: ClosedRange<Length>): CompileError? =
  if (value > Length.ZERO) rangeError(field, value, range)
  else CompileError.NonPositiveDimension(field)

private fun brickError(pattern: PatternSpec.Brick): CompileError? =
  element("targetBrickWidth", pattern.targetBrickWidth, BLOCK_RANGE)
    ?: element("targetBrickHeight", pattern.targetBrickHeight, BLOCK_RANGE)
    ?: element("mortarWidth", pattern.mortarWidth, STRIP_RANGE)

private fun stripesError(mode: GrainMode, pattern: PatternSpec.Stripes): CompileError? {
  if (pattern.strips.isEmpty()) return CompileError.NoStrips
  val bad = pattern.strips.indexOfFirst { it.width <= Length.ZERO }
  val outside = pattern.strips.indexOfFirst { it.width !in STRIP_RANGE }
  return when {
    bad >= 0 -> CompileError.NonPositiveWidth(bad)
    outside >= 0 -> stripOutOfRange(outside)
    mode == GrainMode.END_GRAIN -> element("rowHeight", pattern.rowHeight, BLOCK_RANGE)
    else -> null
  }
}

internal fun stripOutOfRange(index: Int): CompileError =
  CompileError.StripOutOfRange(index, STRIP_RANGE.start, STRIP_RANGE.endInclusive)

private fun rangeError(field: String, value: Length, range: ClosedRange<Length>): CompileError? =
  if (value in range) null else CompileError.OutOfRange(field, range.start, range.endInclusive)
