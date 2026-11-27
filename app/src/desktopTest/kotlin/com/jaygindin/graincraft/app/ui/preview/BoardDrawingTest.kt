package com.jaygindin.graincraft.app.ui.preview

import com.jaygindin.graincraft.app.ui.editor.Finish
import com.jaygindin.graincraft.core.wood.WoodCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoardDrawingTest {
  private val a = PreviewExamples.compile(PreviewExamples.a)
  private val c = PreviewExamples.compile(PreviewExamples.c)

  private fun near(expected: Float, actual: Float) =
    assertEquals(expected, actual, absoluteTolerance = 1e-4f)

  @Test
  fun everyCellOfAStripSharesOneTextureFrameAndSeed() {
    val prepared = prepareBoard(a.layout, PreviewExamples.woods, Finish.RAW)
    val strip1 = prepared.filter { it.piece.sourceStripIndex == 0 }
    assertEquals(12, strip1.size)
    for (p in strip1) {
      near(1.5f, p.frame.width)
      near(1.5f, p.frame.height)
      assertEquals(strip1.first().spec!!.seed, p.spec!!.seed)
      assertEquals(p.piece.flipped, p.spec!!.mirrored)
      assertTrue(p.transposed)
    }
    // Upright rows put strip 1 at the near edge and turned rows at the far edge.
    val (turned, upright) = strip1.partition { it.piece.flipped }
    assertEquals(6, turned.size)
    upright.forEach { near(0f, it.frame.top) }
    turned.forEach { near(10.5f, it.frame.top) }
  }

  @Test
  fun aBrickTrimmedMoreThanItsNeighboursShowsPartOfTheSameFrame() {
    val prepared = prepareBoard(c.layout, PreviewExamples.woods, Finish.OILED)
    val bricks = prepared.filter { it.piece.speciesId == "american-cherry" }
    val cherry = WoodCatalog.byId("american-cherry")!!
    bricks.forEach { assertEquals(cherry.oiled, it.spec!!.tones) }
    for ((_, strip) in bricks.groupBy { it.piece.sourceStripIndex }) {
      strip.forEach {
        near(strip.first().frame.width, it.frame.width)
        near(strip.first().frame.height, it.frame.height)
      }
    }
    // Shifted rows trim the edge strip by different amounts; the more-trimmed brick's frame hangs
    // off the board, so its inner side lines up with the others'.
    val hanging = bricks.filter { it.frame.top < 0f }
    assertTrue(hanging.isNotEmpty())
    hanging.forEach { p -> near(p.polygon.maxOf { it.y }, p.frame.bottom) }
  }

  @Test
  fun longGrainIsNotTransposed() {
    val b = PreviewExamples.compile(PreviewExamples.b)
    val prepared = prepareBoard(b.layout, PreviewExamples.woods, Finish.RAW)
    assertTrue(prepared.none { it.transposed })
    prepared.forEach { near(16f, it.frame.width) }
  }

  @Test
  fun theTooltipLabelNamesMortarRows() {
    val mortar = c.layout.pieces.first { it.sourceStripIndex < 0 }
    assertEquals("Hard Maple · mortar · slice 1", mortar.label(PreviewExamples.woods))
  }
}
