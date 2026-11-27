package com.jaygindin.graincraft.app.ui.preview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.jaygindin.graincraft.app.ui.editor.Finish
import com.jaygindin.graincraft.app.ui.preview.PreviewExamples.inch
import com.jaygindin.graincraft.core.design.EdgeTreatments
import com.jaygindin.graincraft.core.design.JuiceGroove
import kotlin.test.Test

/** Edge details that do not fit the board are left out of the drawing, never thrown. */
@OptIn(ExperimentalTestApi::class)
class BoardPreviewEdgesTest {
  private val a = PreviewExamples.compile(PreviewExamples.a)

  private fun drawn(edges: EdgeTreatments) =
    runDesktopComposeUiTest(1000, 772) {
      setContent {
        PreviewHost {
          BoardPreview(
            layout = a.layout,
            woods = PreviewExamples.woods,
            finish = Finish.RAW,
            edges = edges,
            target = a.design.finished,
            showDimensions = false,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
      waitForIdle()
      // Drawing is done once a hover over the board's corner cell finds it.
      onNodeWithTag("preview-canvas").performMouseInput { moveTo(Offset(87.5f, 87.5f)) }
      waitForIdle()
      onNodeWithTag("preview-tooltip", useUnmergedTree = true)
        .assertTextEquals("Black Walnut · strip 1 · slice 1")
    }

  @Test
  fun aGrooveWiderThanTheBoardIsLeftOutInsteadOfCrashing() =
    // Example A is 12" wide; 1/2" in and 6" wide crosses the middle.
    drawn(EdgeTreatments(juiceGroove = JuiceGroove(inch(6), inch(0, 1, 4), inch(0, 1, 2))))

  @Test
  fun aCornerRadiusLargerThanTheBoardStillDraws() = drawn(EdgeTreatments(cornerRadius = inch(30)))
}
