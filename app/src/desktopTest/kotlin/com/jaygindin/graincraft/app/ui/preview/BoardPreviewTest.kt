package com.jaygindin.graincraft.app.ui.preview

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.jaygindin.graincraft.app.ui.editor.Finish
import com.jaygindin.graincraft.app.ui.preview.PreviewExamples.inch
import com.jaygindin.graincraft.core.design.Dimensions
import kotlin.test.Test

/**
 * The window is 1000 × 772; the bottom 72 px are the controls band (a 48 px pill and 12 px padding
 * above and below), which the fitted board stays above. Example A fits the 1000 × 700 left over at
 * 50 px per inch with its corner at (50, 50), so the screen point (87.5, 87.5) is 3/4" into the
 * board both ways.
 */
@OptIn(ExperimentalTestApi::class)
class BoardPreviewTest {
  private val a = PreviewExamples.compile(PreviewExamples.a)
  private val corner = Offset(87.5f, 87.5f)

  private fun preview(
    target: Dimensions = a.design.finished,
    showDimensions: Boolean = false,
    check: ComposeUiTest.() -> Unit,
  ) =
    runDesktopComposeUiTest(1000, 772) {
      setContent {
        PreviewHost {
          BoardPreview(
            layout = a.layout,
            woods = PreviewExamples.woods,
            finish = Finish.RAW,
            edges = a.design.edges,
            target = target,
            showDimensions = showDimensions,
            modifier = Modifier.fillMaxSize(),
          )
        }
      }
      waitForIdle()
      check()
    }

  private fun ComposeUiTest.hover(at: Offset) {
    onNodeWithTag("preview-canvas").performMouseInput { moveTo(at) }
    waitForIdle()
  }

  private fun ComposeUiTest.assertTooltip(text: String) =
    onNodeWithTag("preview-tooltip", useUnmergedTree = true).assertTextEquals(text)

  @Test
  fun hoverNamesTheSpeciesStripAndSlice() = preview {
    hover(corner)
    assertTooltip("Black Walnut · strip 1 · slice 1")
    // The second row is turned end for end, so the last strip comes first.
    hover(Offset(87.5f + 75f, 87.5f))
    assertTooltip("Hard Maple · strip 8 · slice 2")
  }

  @Test
  fun theFittedBoardStaysAboveTheControlsBand() = preview {
    // Fitted to the whole window instead, the board would start 36 px lower and miss this point.
    hover(Offset(87.5f, 52f))
    assertTooltip("Black Walnut · strip 1 · slice 1")
  }

  @Test
  fun zoomButtonsZoomAboutTheCentreAndFitRestores() = preview {
    onNodeWithTag("preview-zoom-in").performClick()
    waitForIdle()
    // 1.5 times about (500, 350) puts the corner point at (3-1/2", 2-1/2").
    hover(corner)
    assertTooltip("Hard Maple · strip 2 · slice 3")
    onNodeWithTag("preview-fit").performClick()
    hover(corner)
    assertTooltip("Black Walnut · strip 1 · slice 1")
    onNodeWithTag("preview-zoom-out").performClick()
    waitForIdle()
    // 1/1.5 about the centre puts the board's corner at (200, 150), off to the left of the point.
    hover(corner)
    onNodeWithTag("preview-tooltip", useUnmergedTree = true).assertDoesNotExist()
  }

  @Test
  fun draggingPansTheBoard() = preview {
    onNodeWithTag("preview-canvas").performMouseInput {
      moveTo(Offset(400f, 400f))
      press()
      moveBy(Offset(-25f, 0f))
      moveBy(Offset(-25f, 0f))
      moveBy(Offset(-25f, 0f))
      release()
    }
    waitForIdle()
    // Panned 1-1/2" to the left, the point is over the second row.
    hover(corner)
    assertTooltip("Hard Maple · strip 8 · slice 2")
  }

  @Test
  fun scrollingZoomsAboutThePointer() = preview {
    onNodeWithTag("preview-canvas").performMouseInput {
      moveTo(corner)
      scroll(-5f)
    }
    waitForIdle()
    // The point under the pointer stays put, so it still shows the same cell.
    hover(corner)
    assertTooltip("Black Walnut · strip 1 · slice 1")
    hover(Offset(87.5f + 75f, 87.5f))
    assertTooltip("Black Walnut · strip 1 · slice 1")
  }

  @Test
  fun dimensionsShowTheActualSize() =
    preview(showDimensions = true) {
      onNodeWithTag("preview-dimensions").assertTextEquals("18\" × 12\" × 1-1/2\"")
    }

  @Test
  fun dimensionsAddTheTargetWhenItDiffers() =
    preview(target = Dimensions(inch(17, 7, 8), inch(12), inch(1, 1, 2)), showDimensions = true) {
      onNodeWithTag("preview-dimensions")
        .assertTextEquals("18\" × 12\" × 1-1/2\" (target 17-7/8\" × 12\" × 1-1/2\")")
    }

  @Test
  fun dimensionsAreHiddenWhenOff() = preview {
    onNodeWithTag("preview-dimensions").assertDoesNotExist()
  }
}
