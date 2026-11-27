package com.jaygindin.graincraft.core.packet

import com.jaygindin.graincraft.core.Examples
import com.jaygindin.graincraft.core.compile.CompileResult
import com.jaygindin.graincraft.core.compile.CompiledDesign
import com.jaygindin.graincraft.core.compile.DesignCompiler
import com.jaygindin.graincraft.core.design.BoardDesign
import com.jaygindin.graincraft.core.design.EdgeTreatments
import com.jaygindin.graincraft.core.fabrication.cutListTable
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.FractionPrecision
import com.jaygindin.graincraft.core.units.LengthUnit
import com.jaygindin.graincraft.core.units.UnitFormatter
import com.jaygindin.graincraft.core.wood.Woods
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShopPacketExporterTest {
  private val woods = Woods(emptyList())
  private val profile = ShopProfile()
  private val fmt = Examples.fmt

  private fun compile(design: BoardDesign, formatter: UnitFormatter = fmt): CompiledDesign =
    (DesignCompiler.compile(design, woods, profile, formatter) as CompileResult.Ok).compiled

  private fun html(design: BoardDesign = Examples.a, formatter: UnitFormatter = fmt): String =
    ShopPacketExporter.html(compile(design, formatter), woods, profile, formatter)

  @Test
  fun hasEverySection() {
    val compiled = compile(Examples.a)
    val page = ShopPacketExporter.html(compiled, woods, profile, fmt)
    for (heading in listOf("Issues", "Shopping list", "Cut list", "Shop settings")) {
      assertTrue(page.contains(">$heading<"), heading)
    }
    for (stage in compiled.stages) assertTrue(page.contains(escapeHtml(stage.title)), stage.title)
  }

  @Test
  fun boardDrawingIsInline() {
    val compiled = compile(Examples.a)
    val page = ShopPacketExporter.html(compiled, woods, profile, fmt)
    val firstSvg = page.indexOf("<svg")
    assertTrue(firstSvg >= 0)
    val firstStage = page.indexOf(escapeHtml(compiled.stages.first().title))
    assertTrue(firstSvg < firstStage)
    val drawing = page.substring(firstSvg, page.indexOf("</svg>", firstSvg))
    assertEquals(compiled.layout.pieces.size, drawing.split("<rect").size - 1)
  }

  @Test
  fun roundedCornersClipTheDrawing() {
    val rounded = Examples.a.copy(edges = EdgeTreatments(cornerRadius = Examples.inch(0, 1, 2)))
    assertTrue(html(rounded).contains("clip-path"))
    assertFalse(html().contains("clip-path"))
  }

  @Test
  fun selfContained() {
    val page = html()
    for (banned in listOf("src=", "href=\"http", "<link", "<script")) {
      assertFalse(page.contains(banned), banned)
    }
  }

  @Test
  fun printStyles() {
    assertTrue(html().contains("@media print"))
  }

  @Test
  fun escapesNames() {
    val page = html(Examples.a.copy(name = "<Board & \"Co\">"))
    assertTrue(page.contains("&lt;Board &amp; &quot;Co&quot;&gt;"))
    assertFalse(page.contains("<Board"))
  }

  @Test
  fun metricPacket() {
    val metric = UnitFormatter(LengthUnit.MILLIMETRE, FractionPrecision.SIXTEENTH)
    val page = html(Examples.a, metric)
    assertTrue(page.contains("457.2 mm"))
    val text = page.replace(Regex("<[^>]*>"), "")
    assertFalse(Regex("\\d(\"|&quot;)").containsMatchIn(text))
  }

  @Test
  fun showsBoardFeetAndSettings() {
    val page = html()
    assertTrue(page.contains("2.7 BF"))
    assertTrue(page.contains("5.5 BF"))
    assertTrue(page.contains("Titebond III"))
    assertTrue(page.contains("30%"))
  }

  @Test
  fun defaultFileNameReplacesPathCharacters() {
    assertEquals(
      "Walnut-Maple - shop packet.html",
      ShopPacketExporter.defaultFileName("Walnut/Maple"),
    )
    assertEquals("a-b-c - shop packet.html", ShopPacketExporter.defaultFileName("a\\b:c"))
  }

  @Test
  fun cutListUsesTheSharedTableWithItsNotes() {
    val compiled = compile(Examples.b)
    val first = compiled.cutList.first()
    val parts = listOf(first.copy(laminated = true), first.copy(stock = null))
    val page = ShopPacketExporter.html(compiled.copy(cutList = parts), woods, profile, fmt)
    val table = cutListTable(parts, woods, fmt)
    for (header in table.headers) assertTrue(
      page.contains("<th>${escapeHtml(header)}</th>"),
      header,
    )
    assertFalse(page.contains("thickness × width"))
    assertTrue(page.contains(escapeHtml(table.legend)))
    for (row in table.rows) {
      for (cell in row.cells.drop(1)) assertTrue(
        page.contains("<td>${escapeHtml(cell)}</td>"),
        cell,
      )
    }
    assertTrue(page.contains("glued from two boards"))
    assertTrue(page.contains("laminate or buy thicker stock"))
  }
}
