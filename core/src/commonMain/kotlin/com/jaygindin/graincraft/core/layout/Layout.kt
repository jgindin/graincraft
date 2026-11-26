package com.jaygindin.graincraft.core.layout

import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.units.Length

/** Which way the wood fibres run in a piece: `X` along the board's length, `Z` up out of it. */
enum class Fibre {
  X,
  Z,
}

/** A point on the finished board: `x` along its length, `y` across its width. */
data class Point(val x: Length, val y: Length)

/**
 * One visible piece of wood on the finished board's face.
 *
 * `sourceStripIndex` is the strip's position in panel 1 before it was split into sub-panels, so
 * pieces cut from the same strip share it; a mortar-row piece has `-1 - subPanelIndex`. A wide
 * edge-grain strip is laminated from two boards, numbered by `laminationIndex`. `sliceIndex` counts
 * the rows of the same kind (null for edge grain, which is not sliced), and `flipped` and `shiftY`
 * are the row's rotation and shift.
 */
data class Piece(
  val polygon: List<Point>,
  val speciesId: String,
  val fibre: Fibre,
  val sourceStripIndex: Int,
  val laminationIndex: Int,
  val sliceIndex: Int?,
  val flipped: Boolean,
  val shiftY: Length,
  val panelName: String,
)

/** The finished board: its size and the pieces that tile `[0, L] × [0, W]`. */
data class BoardLayout(val size: Dimensions, val pieces: List<Piece>)
