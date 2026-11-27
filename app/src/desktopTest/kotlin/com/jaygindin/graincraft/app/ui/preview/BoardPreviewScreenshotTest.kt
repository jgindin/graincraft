package com.jaygindin.graincraft.app.ui.preview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.jaygindin.graincraft.app.testutil.screenshotOptions
import com.jaygindin.graincraft.app.ui.editor.Finish
import com.jaygindin.graincraft.app.ui.preview.PreviewExamples.inch
import com.jaygindin.graincraft.core.compile.CompiledDesign
import com.jaygindin.graincraft.core.design.EdgeTreatments
import com.jaygindin.graincraft.core.design.JuiceGroove
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

/** The board preview in the light theme only: the board itself is coloured by its woods. */
@OptIn(ExperimentalTestApi::class)
class BoardPreviewScreenshotTest {
  private fun shot(
    name: String,
    compiled: CompiledDesign,
    finish: Finish,
    edges: EdgeTreatments = compiled.design.edges,
    showDimensions: Boolean = false,
  ) =
    runDesktopComposeUiTest(1000, 700) {
      setContent {
        PreviewHost {
          BoardPreview(
            layout = compiled.layout,
            woods = PreviewExamples.woods,
            finish = finish,
            edges = edges,
            target = compiled.design.finished,
            showDimensions = showDimensions,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
      waitForIdle()
      onAllNodes(isRoot())[0].captureRoboImage("screenshots/$name.png", screenshotOptions)
    }

  private val a = PreviewExamples.compile(PreviewExamples.a)
  private val b = PreviewExamples.compile(PreviewExamples.b)
  private val c = PreviewExamples.compile(PreviewExamples.c)

  @Test fun checkerboardRaw() = shot("preview_a_raw", a, Finish.RAW)

  @Test fun checkerboardOiled() = shot("preview_a_oiled", a, Finish.OILED)

  @Test fun stripesRaw() = shot("preview_b_raw", b, Finish.RAW)

  @Test fun stripesOiled() = shot("preview_b_oiled", b, Finish.OILED)

  @Test fun brickRaw() = shot("preview_c_raw", c, Finish.RAW)

  @Test fun brickOiled() = shot("preview_c_oiled", c, Finish.OILED)

  @Test
  fun stripesWithRoundedCornersAndAJuiceGroove() =
    shot(
      "preview_b_groove",
      b,
      Finish.OILED,
      edges =
        EdgeTreatments(
          cornerRadius = inch(1),
          juiceGroove = JuiceGroove(width = inch(0, 3, 8), depth = inch(0, 1, 4), inch(0, 1, 2)),
        ),
    )

  @Test
  fun checkerboardWithDimensions() =
    shot("preview_a_dimensions", a, Finish.OILED, showDimensions = true)
}
