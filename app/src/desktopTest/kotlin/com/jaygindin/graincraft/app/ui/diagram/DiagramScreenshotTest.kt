package com.jaygindin.graincraft.app.ui.diagram

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.jaygindin.graincraft.app.testutil.screenshotOptions
import com.jaygindin.graincraft.app.ui.preview.PreviewExamples
import com.jaygindin.graincraft.app.ui.theme.GrainCraftTheme
import com.jaygindin.graincraft.app.ui.theme.Spacing
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

/** Each stage diagram of example A on its own, wide enough to draw at full label size. */
@OptIn(ExperimentalTestApi::class)
class DiagramScreenshotTest {
  @Test
  fun stageDiagramsOfA() {
    val compiled = PreviewExamples.compile(PreviewExamples.a)
    for (stage in compiled.stages) {
      val diagram = stage.diagram ?: continue
      runDesktopComposeUiTest(900, 700) {
        setContent {
          GrainCraftTheme(dark = false) {
            Surface { DiagramCanvas(diagram, Modifier.padding(Spacing.lg).width(860.dp)) }
          }
        }
        waitForIdle()
        onAllNodes(isRoot())[0].captureRoboImage(
          "screenshots/diagram_a_stage_${stage.number}.png",
          screenshotOptions,
        )
      }
    }
  }
}
