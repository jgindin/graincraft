package com.jaygindin.graincraft.core.guide

import com.jaygindin.graincraft.core.Examples
import com.jaygindin.graincraft.core.Examples.inch
import com.jaygindin.graincraft.core.design.BoardDesign
import com.jaygindin.graincraft.core.design.EdgeProfile
import com.jaygindin.graincraft.core.design.EdgeTreatments
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.design.JuiceGroove
import com.jaygindin.graincraft.core.design.PatternSpec
import com.jaygindin.graincraft.core.design.Strip
import com.jaygindin.graincraft.core.fabrication.FabricationResult
import com.jaygindin.graincraft.core.fabrication.FitResult
import com.jaygindin.graincraft.core.fabrication.compileFabrication
import com.jaygindin.graincraft.core.fabrication.cutList
import com.jaygindin.graincraft.core.fabrication.fitPattern
import com.jaygindin.graincraft.core.fabrication.lumber
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.wood.Woods
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class StagePlannerTest {
  private val woods = Woods(emptyList())

  private fun stages(design: BoardDesign, profile: ShopProfile = ShopProfile()): List<Stage> {
    val fit = fitPattern(design.grainMode, design.finished, design.pattern, Examples.fmt)
    assertTrue(fit is FitResult.Ok, "expected a fit but was $fit")
    val result = compileFabrication(design.grainMode, design.finished, fit.fit, profile)
    assertTrue(result is FabricationResult.Ok, "expected Ok but was $result")
    val parts = cutList(result.fabrication, profile)
    return planStages(
      design,
      result.fabrication,
      parts,
      lumber(parts, profile),
      woods,
      profile,
      Examples.fmt,
    )
  }

  private fun List<Stage>.of(kind: StageKind): Stage = single { it.kind == kind }

  private fun Stage.text(): String = (steps + callouts.map { it.text }).joinToString("\n")

  @Test
  fun endGrainAlwaysWarnsAgainstThePlaner() {
    val flatten =
      stages(Examples.a, ShopProfile(planerMaxWidth = inch(24)))
        .of(
          StageKind.FLATTEN_BOARD,
        )
    assertTrue(
      flatten.callouts.any { it.kind == CalloutKind.WARNING && "planer" in it.text },
      "callouts were ${flatten.callouts}",
    )
  }

  @Test
  fun acetoneOnlyWithOilyWoods() {
    val teak =
      Examples.design(
        GrainMode.EDGE_GRAIN,
        Examples.b.finished,
        PatternSpec.Stripes(
          listOf(
            Strip("teak", inch(2)),
            Strip("hard-maple", inch(2)),
            Strip("teak", inch(2)),
            Strip("hard-maple", inch(2, 1, 4)),
          ),
        ),
      )
    assertTrue("acetone" in stages(teak).of(StageKind.PREPARE).text())
    assertFalse("acetone" in stages(Examples.a).of(StageKind.PREPARE).text())
  }

  @Test
  fun bleedingWoodsAreWipedPromptly() {
    val padauk =
      Examples.design(
        GrainMode.EDGE_GRAIN,
        Examples.b.finished,
        PatternSpec.Stripes(
          listOf(Strip("african-padauk", inch(4)), Strip("hard-maple", inch(4, 1, 4))),
        ),
      )
    assertTrue("squeeze-out" in stages(padauk).of(StageKind.PREPARE).text())
    assertFalse("squeeze-out" in stages(Examples.a).of(StageKind.PREPARE).text())
  }

  @Test
  fun edgeGrainStageList() {
    val stages = stages(Examples.b)
    assertEquals(
      listOf(
        StageKind.BUY_AND_MILL,
        StageKind.PREPARE,
        StageKind.GLUE_PANELS,
        StageKind.FLATTEN_PANELS,
        StageKind.TRIM,
        StageKind.FINISH,
      ),
      stages.map { it.kind },
    )
    assertEquals((1..6).toList(), stages.map { it.number })
  }

  @Test
  fun endGrainStageListWithEdgeDetails() {
    val design = Examples.a.copy(edges = EdgeTreatments(fingerGrips = true))
    val stages = stages(design)
    assertEquals(StageKind.entries.toList(), stages.map { it.kind })
    assertEquals((1..10).toList(), stages.map { it.number })
  }

  @Test
  fun buyAndMillListsLumberAndParts() {
    val text = stages(Examples.a).of(StageKind.BUY_AND_MILL).text()
    assertTrue("2.7 BF of Black Walnut" in text, text)
    assertTrue("2.7 BF of Hard Maple" in text, text)
    assertTrue("5.5 BF" in text, text)
    assertTrue("1-1/2\" × 1-9/16\" × 23\"" in text, text)
    assertTrue("8/4" in text, text)
  }

  @Test
  fun crosscutStepsNameCountAndThickness() {
    val steps = stages(Examples.a).of(StageKind.CROSSCUT).steps
    for (panel in listOf("1A", "1B")) {
      assertTrue(
        steps.any { panel in it && "12 slices" in it && "1-5/8\"" in it },
        "no crosscut step for $panel in $steps",
      )
    }
  }

  @Test
  fun crosscutIncludesMortarPanels() {
    val steps = stages(Examples.c).of(StageKind.CROSSCUT).steps
    for (panel in listOf("MA", "MB")) {
      assertTrue(steps.any { panel in it && "9 slices" in it }, "no step for $panel in $steps")
    }
  }

  @Test
  fun arrangeMentionsRotationAndMortar() {
    val a = stages(Examples.a).of(StageKind.ARRANGE_AND_GLUE).text()
    assertTrue("every other row" in a && "180°" in a, a)
    val c = stages(Examples.c).of(StageKind.ARRANGE_AND_GLUE).text()
    assertTrue("mortar row" in c, c)
    assertTrue("1-5/8\"" in c && "shift" in c.lowercase(), c)
  }

  @Test
  fun trimEndGrainIsSidesOnly() {
    val text = stages(Examples.c).of(StageKind.TRIM).text()
    assertTrue("12\" wide" in text, text)
    assertTrue("2\" from each side" in text, text)
  }

  @Test
  fun edgeDetailsOrder() {
    val design =
      Examples.b.copy(
        edges =
          EdgeTreatments(
            cornerRadius = inch(0, 1, 2),
            edgeProfile = EdgeProfile.ROUNDOVER,
            edgeProfileSize = inch(0, 1, 4),
            juiceGroove = JuiceGroove(inch(0, 1, 2), inch(0, 1, 4), inch(0, 3, 4)),
            fingerGrips = true,
            feet = true,
          ),
      )
    val stages = stages(design)
    val details = stages.of(StageKind.EDGE_DETAILS).steps
    val order =
      listOf("juice groove", "corner", "roundover", "finger grips").map { word ->
        details.indexOfFirst { word in it.lowercase() }
      }
    assertTrue(order.all { it >= 0 } && order == order.sorted(), "steps were $details")
    assertTrue("feet" in stages.of(StageKind.FINISH).steps.last())
    assertTrue(details.none { "feet" in it })
  }

  @Test
  fun diagramsForGlueCrosscutAndArrange() {
    val stages = stages(Examples.a)
    for (kind in listOf(StageKind.GLUE_PANELS, StageKind.CROSSCUT, StageKind.ARRANGE_AND_GLUE)) {
      assertNotNull(stages.of(kind).diagram, "no diagram for $kind")
    }
  }

  @Test
  fun glueDiagramHasOneBoxPerStrip() {
    val diagram = assertNotNull(stages(Examples.c).of(StageKind.GLUE_PANELS).diagram)
    val strips = diagram.elements.filterIsInstance<DiagramElement.Box>().filter { it.fill != null }
    // Panel 1 has 9 strips, the mortar panel 2 boards, and the legend one swatch per species.
    assertEquals(9 + 2 + 2, strips.size)
  }

  @Test
  fun crosscutDiagramDrawsOneDashedLinePerSlice() {
    val diagram = assertNotNull(stages(Examples.a).of(StageKind.CROSSCUT).diagram)
    val dashed = diagram.elements.filterIsInstance<DiagramElement.Line>().count { it.dashed }
    assertEquals(12, dashed)
  }

  @Test
  fun arrangeDiagramHatchesRotatedRows() {
    val diagram = assertNotNull(stages(Examples.a).of(StageKind.ARRANGE_AND_GLUE).diagram)
    val boxes = diagram.elements.filterIsInstance<DiagramElement.Box>()
    // 12 rows of 8 cells; the six rotated rows are hatched.
    assertEquals(96, boxes.size)
    assertEquals(48, boxes.count { it.hatched })
  }
}
