package com.jaygindin.graincraft.app.ui.editor

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.jaygindin.graincraft.app.ui.preview.PreviewExamples
import com.jaygindin.graincraft.app.ui.preview.PreviewHost
import com.jaygindin.graincraft.core.fabrication.cutListTable
import com.jaygindin.graincraft.core.packet.ShopPacketExporter
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.FractionPrecision
import com.jaygindin.graincraft.core.units.LengthUnit
import com.jaygindin.graincraft.core.units.UnitFormatter
import kotlin.test.Test
import kotlin.test.assertTrue

/** The Lumber tab and the printed packet show the same cut list, row for row. */
@OptIn(ExperimentalTestApi::class)
class LumberTabTest {
  private val fmt = UnitFormatter(LengthUnit.INCH, FractionPrecision.SIXTEENTH)

  private fun escape(s: String) = s.replace("&", "&amp;").replace("\"", "&quot;")

  @Test
  fun theTabAndThePacketShowTheSameRowsForExamplesAToC() {
    for (design in listOf(PreviewExamples.a, PreviewExamples.b, PreviewExamples.c)) {
      val compiled = PreviewExamples.compile(design)
      val table = cutListTable(compiled.cutList, PreviewExamples.woods, fmt)
      val packet = ShopPacketExporter.html(compiled, PreviewExamples.woods, ShopProfile(), fmt)
      runDesktopComposeUiTest(1600, 1200) {
        setContent {
          PreviewHost { LumberTab(compiled, PreviewExamples.woods, Modifier.fillMaxSize()) }
        }
        onNodeWithTag("cut-header").assertTextEquals(*table.headers.toTypedArray())
        table.rows.forEachIndexed { i, row ->
          onNodeWithTag("cut-$i").assertTextEquals(*row.cells.toTypedArray())
          for (cell in row.cells.drop(1)) {
            assertTrue(packet.contains("<td>${escape(cell)}</td>"), "packet lacks $cell")
          }
        }
        onNodeWithTag("cut-${table.rows.size}").assertDoesNotExist()
      }
    }
  }
}
