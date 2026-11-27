package com.jaygindin.graincraft.app.ui.library

import com.jaygindin.graincraft.core.design.PatternSpec
import com.jaygindin.graincraft.core.design.Strip
import com.jaygindin.graincraft.core.units.Length

private const val WALNUT = "black-walnut"
private const val MAPLE = "hard-maple"
private const val CHERRY = "american-cherry"
private const val DEFAULT_STRIPS = 7
private const val BRICK_WIDTH_INCHES = 3
private val MORTAR = Length.inches(0, 1, 4)

/** A sensible starting pattern for [kind]: two contrasting woods in common sizes. */
fun defaultPattern(kind: PatternKind): PatternSpec =
  when (kind) {
    PatternKind.STRIPES ->
      PatternSpec.Stripes(
        List(DEFAULT_STRIPS) { i ->
          Strip(if (i % 2 == 0) WALNUT else MAPLE, Length.inches(1, 1, 2))
        },
      )
    PatternKind.CHECKERBOARD -> PatternSpec.Checkerboard(WALNUT, MAPLE, Length.inches(1, 1, 2))
    PatternKind.BRICK ->
      PatternSpec.Brick(
        CHERRY,
        MAPLE,
        targetBrickWidth = Length.inches(BRICK_WIDTH_INCHES),
        targetBrickHeight = Length.inches(1, 1, 2),
        mortarWidth = MORTAR,
      )
  }
