package com.jaygindin.graincraft.app.ui.diagram

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.jaygindin.graincraft.app.ui.preview.PreviewExamples
import com.jaygindin.graincraft.app.ui.theme.GrainCraftTheme
import com.jaygindin.graincraft.core.guide.Diagram
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DiagramCanvasTest {
  private val diagrams: List<Pair<String, Diagram>> =
    listOf("A" to PreviewExamples.a, "B" to PreviewExamples.b, "C" to PreviewExamples.c).flatMap {
      (name, design) ->
      PreviewExamples.compile(design).stages.mapNotNull { stage ->
        stage.diagram?.let { "$name stage ${stage.number}" to it }
      }
    }

  @Test
  fun everyExampleHasDiagrams() {
    // Glue-up, crosscut and arrangement for each end-grain board; at least the glue-up for B.
    assertTrue(
      diagrams.count { it.first.startsWith("A") } >= 3,
      diagrams.map { it.first }.toString(),
    )
    assertTrue(diagrams.count { it.first.startsWith("B") } >= 1)
    assertTrue(diagrams.count { it.first.startsWith("C") } >= 3)
  }

  @Test
  fun drawsEveryStageDiagramScaledToItsWidthOrAReadableMinimum() {
    for ((name, diagram) in diagrams) {
      runDesktopComposeUiTest(800, 900) {
        setContent {
          GrainCraftTheme(dark = false) { DiagramCanvas(diagram, Modifier.width(600.dp)) }
        }
        waitForIdle()
        val bounds = onNodeWithTag("diagram-canvas").fetchSemanticsNode().size
        // Fitted to 600 dp, unless that is under 20 dp an inch: then wider, and it scrolls.
        val width = maxOf(600.0, 20 * diagram.width)
        assertTrue(abs(bounds.width - width) <= 1.0, "$name width ${bounds.width} vs $width")
        val height = width * diagram.height / diagram.width
        assertTrue(abs(bounds.height - height) <= 1.0, "$name height ${bounds.height} vs $height")
      }
    }
  }
}
