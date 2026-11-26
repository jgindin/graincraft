package com.jaygindin.graincraft.core.layout

import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.fabrication.Fabrication
import com.jaygindin.graincraft.core.fabrication.Panel
import com.jaygindin.graincraft.core.fabrication.PanelKind
import com.jaygindin.graincraft.core.fabrication.Row
import com.jaygindin.graincraft.core.fabrication.RowKind
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.max
import com.jaygindin.graincraft.core.units.min

/**
 * Lays out the finished board's face from the build, so the preview shows which strip each piece
 * came from and how each row was turned or shifted.
 *
 * Each row's strips are laid across the glue-up width centred on the board, then clipped to the
 * finished `L × W`. An edge-grain strip milled thicker than the thickest stock in `profile` is
 * laminated from two boards, so it shows as two pieces with the glue line where it really is: the
 * milled strip is halved before the side trim is clipped away.
 */
fun buildLayout(fabrication: Fabrication, finished: Dimensions, profile: ShopProfile): BoardLayout {
  val origins = stripOrigins(fabrication.panels)
  val panels = fabrication.panels.associateBy { it.name }
  val y0 = Length.ZERO - (fabrication.glueUpWidth - finished.width) / 2
  val pieces =
    when (fabrication.mode) {
      GrainMode.EDGE_GRAIN -> {
        val thickest = profile.surfacedThickness.values.maxOrNull() ?: finished.width
        val row = fabrication.rows.single()
        layRow(row, panels, origins, y0, Length.ZERO, finished.length, sliceIndex = null)
          .flatMap { it.laminated(thickest) }
          .clip(finished)
          .map { it.toPiece(Fibre.X) }
      }
      GrainMode.END_GRAIN -> {
        val seen = mutableMapOf<RowKind, Int>()
        var x = Length.ZERO
        fabrication.rows
          .flatMap { row ->
            val slice = seen.getOrElse(row.kind) { 0 }.also { seen[row.kind] = it + 1 }
            layRow(row, panels, origins, y0 + row.shift, x, x + row.height, slice).also {
              x += row.height
            }
          }
          .clip(finished)
          .map { it.toPiece(Fibre.Z) }
      }
    }
  return BoardLayout(finished, pieces)
}

/** A piece as an axis-aligned rectangle before it becomes a polygon. */
private data class Rect(
  val x0: Length,
  val x1: Length,
  val y0: Length,
  val y1: Length,
  val speciesId: String,
  val sourceStripIndex: Int,
  val laminationIndex: Int,
  val sliceIndex: Int?,
  val row: Row,
  val panelName: String,
) {
  fun toPiece(fibre: Fibre) =
    Piece(
      polygon = listOf(Point(x0, y0), Point(x1, y0), Point(x1, y1), Point(x0, y1)),
      speciesId = speciesId,
      fibre = fibre,
      sourceStripIndex = sourceStripIndex,
      laminationIndex = laminationIndex,
      sliceIndex = sliceIndex,
      flipped = row.rotated,
      shiftY = row.shift,
      panelName = panelName,
    )

  /** Splits a strip too thick for one board into two halves; the first takes the odd nanometre. */
  fun laminated(thickest: Length): List<Rect> {
    val width = y1 - y0
    if (width <= thickest) return listOf(this)
    val mid = y0 + width / 2
    return listOf(copy(y1 = mid), copy(y0 = mid, laminationIndex = 1))
  }
}

/**
 * Each long-grain panel strip's position in panel 1 before splitting; a mortar sub-panel's single
 * board has `-1 - subPanelIndex`.
 */
private fun stripOrigins(panels: List<Panel>): Map<String, List<Int>> {
  var next = 0
  var mortar = 0
  return panels.associate { p ->
    p.name to
      if (p.kind == PanelKind.MORTAR) List(p.strips.size) { -1 - mortar++ }
      else List(p.strips.size) { next++ }
  }
}

/**
 * The strips of one row from `y` upward. A rotated row already lists its sub-panels in reverse, and
 * each slice in it is turned end for end, so its strips are reversed too.
 */
private fun layRow(
  row: Row,
  panels: Map<String, Panel>,
  origins: Map<String, List<Int>>,
  y: Length,
  x0: Length,
  x1: Length,
  sliceIndex: Int?,
): List<Rect> {
  var at = y
  return row.panels.flatMap { name ->
    val panel = panels.getValue(name)
    val strips = panel.strips.zip(origins.getValue(name))
    (if (row.rotated) strips.reversed() else strips).map { (strip, origin) ->
      val start = at
      at += strip.millWidth
      Rect(x0, x1, start, at, strip.speciesId, origin, 0, sliceIndex, row, name)
    }
  }
}

/** Trims every rectangle to the finished board and drops those trimmed away entirely. */
private fun List<Rect>.clip(board: Dimensions): List<Rect> = mapNotNull { r ->
  val clipped =
    r.copy(
      x0 = max(r.x0, Length.ZERO),
      x1 = min(r.x1, board.length),
      y0 = max(r.y0, Length.ZERO),
      y1 = min(r.y1, board.width),
    )
  clipped.takeIf { it.x1 > it.x0 && it.y1 > it.y0 }
}
