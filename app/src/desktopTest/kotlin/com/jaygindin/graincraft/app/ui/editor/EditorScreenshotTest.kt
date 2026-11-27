package com.jaygindin.graincraft.app.ui.editor

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.jaygindin.graincraft.app.testutil.screenshotOptions
import com.jaygindin.graincraft.app.ui.preview.PreviewExamples
import com.jaygindin.graincraft.app.ui.preview.PreviewExamples.inch
import com.jaygindin.graincraft.core.design.BoardDesign
import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.design.PatternSpec
import com.jaygindin.graincraft.core.design.Strip
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class EditorScreenshotTest {
  private fun shot(
    name: String,
    design: BoardDesign,
    dark: Boolean = false,
    width: Int = 1400,
    height: Int = 900,
    setUp: ComposeUiTest.() -> Unit = {},
  ) =
    runDesktopComposeUiTest(width, height) {
      withEditor(design, dark = dark) {
        setUp()
        waitForIdle()
        onAllNodes(isRoot())[0].captureRoboImage("screenshots/$name.png", screenshotOptions)
      }
    }

  @Test fun checkerboardLight() = shot("editor_a_light", PreviewExamples.a)

  @Test fun checkerboardDark() = shot("editor_a_dark", PreviewExamples.a, dark = true)

  @Test fun stripesLight() = shot("editor_b_light", PreviewExamples.b)

  @Test fun stripesDark() = shot("editor_b_dark", PreviewExamples.b, dark = true)

  @Test fun brickLight() = shot("editor_c_light", PreviewExamples.c)

  @Test fun brickDark() = shot("editor_c_dark", PreviewExamples.c, dark = true)

  @Test
  fun collapsedLayout() = shot("editor_a_collapsed", PreviewExamples.a, width = 1100, height = 800)

  @Test
  fun buildTab() =
    shot("editor_a_build", PreviewExamples.a) {
      onNodeWithTag("tab-build").performClick()
      // The arrangement stage, so the golden shows a diagram in its card.
      onNodeWithTag("stage-6").performScrollTo()
    }

  @Test
  fun lumberTab() =
    shot("editor_a_lumber", PreviewExamples.a) { onNodeWithTag("tab-lumber").performClick() }

  /** Stripes with a red oak strip: an end-grain board with issues to show. */
  @Test
  fun issuesTab() =
    shot(
      "editor_issues",
      PreviewExamples.a.copy(
        finished = Dimensions(inch(16), inch(10, 1, 2), inch(1, 1, 4)),
        pattern =
          PatternSpec.Stripes(
            List(7) { i ->
              Strip(
                if (i == 3) "red-oak" else if (i % 2 == 0) "black-walnut" else "hard-maple",
                inch(1, 1, 2),
              )
            },
          ),
      ),
    )
}
