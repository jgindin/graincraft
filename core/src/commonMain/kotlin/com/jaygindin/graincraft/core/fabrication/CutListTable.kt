package com.jaygindin.graincraft.core.fabrication

import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.UnitFormatter
import com.jaygindin.graincraft.core.wood.Woods

/** One cut-list line, as text cells under [CutListTable.headers]; [speciesId] is for a swatch. */
data class CutListRow(val speciesId: String, val cells: List<String>)

/** The cut list as the user reads it, with [legend] saying what W, H and L are. */
data class CutListTable(val headers: List<String>, val rows: List<CutListRow>, val legend: String)

private val HEADERS = listOf("Wood", "Qty", "Size (W × H × L)", "Mill from", "Panels")
private const val LEGEND =
  "W is across the board, H is up through its thickness before flattening, and L is along " +
    "the strip."

/**
 * The one way the cut list is written out: the editor's Lumber tab and the shop packet both show
 * these rows, so the screen and the printout cannot disagree about which face is which. A part
 * glued from two boards says so, and so does a part no stock is thick enough for.
 */
fun cutListTable(parts: List<CutPart>, woods: Woods, formatter: UnitFormatter): CutListTable {
  fun f(length: Length) = formatter.format(length)
  val rows = parts.map { p ->
    val size = "${f(p.width)} × ${f(p.height)} × ${f(p.length)}"
    val millFrom =
      p.stock?.let { "${it.label} stock, ${f(p.fromThickness)} thick, rip ${f(p.ripWidth)}" }
        ?: "No stock is thick enough: laminate or buy thicker stock"
    CutListRow(
      p.speciesId,
      listOf(
        woods.byId(p.speciesId)?.name ?: p.speciesId,
        p.count.toString(),
        if (p.laminated) "$size, glued from two boards" else size,
        millFrom,
        p.panelNames.joinToString(", "),
      ),
    )
  }
  return CutListTable(HEADERS, rows, LEGEND)
}
