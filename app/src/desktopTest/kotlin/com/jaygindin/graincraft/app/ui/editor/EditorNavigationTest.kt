package com.jaygindin.graincraft.app.ui.editor

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.jaygindin.graincraft.app.App
import com.jaygindin.graincraft.app.testutil.FakePlatform
import com.jaygindin.graincraft.app.ui.preview.PreviewExamples
import com.jaygindin.graincraft.app.ui.preview.PreviewExamples.inch
import kotlin.test.Test
import kotlin.test.assertEquals

private const val WIDE = 1400
private const val TALL = 900

/** The editor inside the app: its layout beside the rail, and leaving it without losing edits. */
@OptIn(ExperimentalTestApi::class)
class EditorNavigationTest {
  @Test
  fun narrowWindowCollapsesToTabs() {
    // In the app, beside the rail: the threshold is the window's width, not the editor's.
    // 1250 leaves the editor only 1170 dp beside the rail, yet the window is wide enough.
    runDesktopComposeUiTest(1250, 800) {
      openInApp(FakePlatform())
      onNodeWithTag("tab-preview").assertDoesNotExist()
      onNodeWithTag("preview-canvas").assertExists()
      onNodeWithTag("tab-issues").assertIsSelected()
    }
    runDesktopComposeUiTest(1100, 800) {
      openInApp(FakePlatform())
      onNodeWithTag("tab-preview").assertIsSelected()
      onNodeWithTag("preview-canvas").assertExists()
      onNodeWithTag("tab-lumber").performClick()
      awaitTag("lumber-total")
      onNodeWithTag("preview-canvas").assertDoesNotExist()
    }
  }

  @Test
  fun openingADesignShowsTheEditorAndBackSavesFirst() =
    runDesktopComposeUiTest(WIDE, TALL) {
      val platform = FakePlatform()
      openInApp(platform)

      onNodeWithTag("dim-width").performTextReplacement("14")
      // Back must write the change at once rather than lose it to the autosave delay.
      onNodeWithTag("editor-back").performClick()
      awaitTag("screen-designs")
      assertEquals(inch(14), platform.store.designs.getValue("a").finished.width)
      onNodeWithTag("screen-editor").assertDoesNotExist()
    }

  @Test
  fun aNameTypedJustBeforeBackIsSaved() =
    runDesktopComposeUiTest(WIDE, TALL) {
      val platform = FakePlatform()
      openInApp(platform)

      onNodeWithTag("editor-name").performTextReplacement("Walnut checker")
      onNodeWithTag("editor-back").performClick()
      awaitTag("screen-designs")
      assertEquals("Walnut checker", platform.store.designs.getValue("a").name)
    }

  @Test
  fun leavingThroughTheRailSavesTypedEdits() =
    runDesktopComposeUiTest(WIDE, TALL) {
      val platform = FakePlatform()
      openInApp(platform)

      onNodeWithTag("editor-name").performTextReplacement("Gift board")
      onNodeWithTag("dim-length").performTextReplacement("20")
      // The length field still has focus: its valid text is already in the design.
      onNodeWithTag("rail-woods").performClick()
      awaitTag("screen-woods")
      val saved = platform.store.designs.getValue("a")
      assertEquals("Gift board", saved.name)
      assertEquals(inch(20), saved.finished.length)
    }
}

/** Shows the app with example A stored as design "a", and opens it from the library. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.openInApp(platform: FakePlatform) {
  platform.store.designs["a"] = PreviewExamples.a.copy(id = "a")
  setContent { App(platform.graph) }
  awaitTag("design-card-a")
  onNodeWithTag("design-card-a").performClick()
  awaitTag("dim-width")
}
