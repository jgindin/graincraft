package com.jaygindin.graincraft.core.fabrication

import com.jaygindin.graincraft.core.Examples
import com.jaygindin.graincraft.core.Examples.inch
import com.jaygindin.graincraft.core.units.Nominal
import com.jaygindin.graincraft.core.wood.Woods
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CutListTableTest {
  private val woods = Woods(emptyList())

  private fun part(stock: Nominal?, laminated: Boolean = false) =
    CutPart(
      speciesId = "black-walnut",
      count = 3,
      width = inch(1, 1, 2),
      height = inch(1, 9, 16),
      length = inch(23),
      stock = stock,
      fromThickness = inch(1, 9, 16),
      ripWidth = inch(1, 1, 2),
      laminated = laminated,
      panelNames = listOf("1A", "1B"),
    )

  @Test
  fun headersNameWhatEachSizeMeans() {
    val table = cutListTable(emptyList(), woods, Examples.fmt)
    assertEquals(listOf("Wood", "Qty", "Size (W × H × L)", "Mill from", "Panels"), table.headers)
    assertTrue("W" in table.legend && "H" in table.legend && "L" in table.legend)
  }

  @Test
  fun aStockedPartSaysWhatToMillItFrom() {
    val row = cutListTable(listOf(part(Nominal.EIGHT_QUARTER)), woods, Examples.fmt).rows.single()
    assertEquals("black-walnut", row.speciesId)
    assertEquals(
      listOf(
        "Black Walnut",
        "3",
        "1-1/2\" × 1-9/16\" × 23\"",
        "8/4 stock, 1-9/16\" thick, rip 1-1/2\"",
        "1A, 1B",
      ),
      row.cells,
    )
  }

  @Test
  fun aLaminatedPartSaysItIsGluedFromTwoBoards() {
    val row =
      cutListTable(listOf(part(Nominal.EIGHT_QUARTER, laminated = true)), woods, Examples.fmt)
        .rows
        .single()
    assertEquals("1-1/2\" × 1-9/16\" × 23\", glued from two boards", row.cells[2])
  }

  @Test
  fun aPartNoStockFitsSaysToLaminateOrBuyThicker() {
    val row = cutListTable(listOf(part(null)), woods, Examples.fmt).rows.single()
    assertEquals("No stock is thick enough: laminate or buy thicker stock", row.cells[3])
  }
}
