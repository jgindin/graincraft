package com.jaygindin.graincraft.app

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.test.withKeysDown
import com.jaygindin.graincraft.app.testutil.FakePlatform
import com.jaygindin.graincraft.app.ui.editor.ShortcutHost
import com.jaygindin.graincraft.app.ui.editor.awaitTag
import com.jaygindin.graincraft.app.ui.editor.awaitUntil
import com.jaygindin.graincraft.app.ui.editor.openInApp
import com.jaygindin.graincraft.app.ui.editor.textOf
import com.jaygindin.graincraft.app.ui.preview.PreviewExamples.inch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

private const val WIDE = 1400
private const val TALL = 900

@OptIn(ExperimentalTestApi::class)
class ShortcutsTest {
  @Test
  fun keysMapToActions() {
    assertEquals(AppAction.NEW_DESIGN, shortcutAction(Key.N, ctrl = true))
    assertEquals(AppAction.UNDO, shortcutAction(Key.Z, ctrl = true))
    assertEquals(AppAction.REDO, shortcutAction(Key.Z, ctrl = true, shift = true))
    assertEquals(AppAction.REDO, shortcutAction(Key.Y, ctrl = true))
    assertEquals(AppAction.EXPORT_PACKET, shortcutAction(Key.P, ctrl = true))
    assertEquals(AppAction.FIND_WOOD, shortcutAction(Key.F, ctrl = true))
  }

  @Test
  fun otherKeysAreLeftAlone() {
    assertNull(shortcutAction(Key.N, ctrl = false))
    assertNull(shortcutAction(Key.Z, ctrl = false, shift = true))
    assertNull(shortcutAction(Key.Q, ctrl = true))
    assertNull(shortcutAction(Key.N, ctrl = true, shift = true))
    assertNull(shortcutAction(Key.Y, ctrl = true, shift = true))
    assertNull(shortcutAction(Key.P, ctrl = true, alt = true))
    assertNull(shortcutAction(Key.F, ctrl = true, meta = true))
  }

  @Test
  fun ctrlNInTheLibraryOpensTheNewDesignDialog() =
    runDesktopComposeUiTest(WIDE, TALL) {
      val controller = AppController()
      setContent { ShortcutHost(FakePlatform(), controller) }
      awaitTag("library-new")
      onNodeWithTag("library-import").ctrl(Key.N)
      awaitTag("new-name")
    }

  @Test
  fun ctrlNInTheEditorSavesAndOpensTheNewDesignDialog() =
    runDesktopComposeUiTest(WIDE, TALL) {
      val platform = FakePlatform()
      openInApp(platform, AppController())
      onNodeWithTag("dim-width").performTextReplacement("14")

      onNodeWithTag("dim-width").ctrl(Key.N)
      awaitTag("new-name")
      onNodeWithTag("rail-designs").assertIsSelected()
      onNodeWithTag("screen-editor").assertDoesNotExist()
      assertEquals(inch(14), platform.store.designs.getValue("a").finished.width)
    }

  @Test
  fun ctrlZAndRedoKeysUndoAndRedoInTheEditor() =
    runDesktopComposeUiTest(WIDE, TALL) {
      val platform = FakePlatform()
      openInApp(platform, AppController())
      val field = onNodeWithTag("dim-width")
      field.performTextReplacement("14")
      awaitUntil { textOf("dim-width").trimEnd('"') == "14" }

      // Focus is still in the field: the shortcut is the editor's undo, not the field's own,
      // which would add an edit rather than make one to redo.
      field.ctrl(Key.Z)
      awaitUntil { textOf("dim-width").trimEnd('"') == "12" }
      onNodeWithTag("editor-redo").assertIsEnabled()
      onNodeWithTag("editor-undo").assertIsNotEnabled()

      field.requestFocus().performKeyInput {
        withKeysDown(listOf(Key.CtrlLeft, Key.ShiftLeft)) { pressKey(Key.Z) }
      }
      awaitUntil { textOf("dim-width").trimEnd('"') == "14" }
      onNodeWithTag("editor-redo").assertIsNotEnabled()

      field.ctrl(Key.Z)
      awaitUntil { textOf("dim-width").trimEnd('"') == "12" }
      field.ctrl(Key.Y)
      awaitUntil { textOf("dim-width").trimEnd('"') == "14" }
      onNodeWithTag("editor-redo").assertIsNotEnabled()
      onNodeWithTag("editor-undo").assertIsEnabled()
    }

  @Test
  fun ctrlPExportsThePacket() =
    runDesktopComposeUiTest(WIDE, TALL) {
      val platform = FakePlatform()
      openInApp(platform, AppController())

      onNodeWithTag("dim-width").ctrl(Key.P)
      awaitTag("snackbar-open-packet")
      assertEquals("Example - shop packet", platform.dialogs.saves.single().suggestedName)
    }

  @Test
  fun ctrlFOpensWoodsWithTheSearchFocused() =
    runDesktopComposeUiTest(WIDE, TALL) {
      val platform = FakePlatform()
      openInApp(platform, AppController())
      onNodeWithTag("dim-width").performTextReplacement("14")

      onNodeWithTag("dim-width").ctrl(Key.F)
      awaitTag("woods-search")
      onNodeWithTag("rail-woods").assertIsSelected()
      waitForIdle()
      onNodeWithTag("woods-search").assertIsFocused()
      // Leaving the editor this way saves it too.
      assertEquals(inch(14), platform.store.designs.getValue("a").finished.width)
    }

  @Test
  fun editorShortcutsDoNothingOutsideTheEditor() =
    runDesktopComposeUiTest(WIDE, TALL) {
      val controller = AppController()
      setContent { ShortcutHost(FakePlatform(), controller) }
      awaitTag("library-new")
      // Not handled, so a text field elsewhere still gets its own undo.
      runOnIdle {
        assertFalse(controller.perform(AppAction.UNDO))
        assertFalse(controller.perform(AppAction.REDO))
        assertFalse(controller.perform(AppAction.EXPORT_PACKET))
      }
    }
}

@OptIn(ExperimentalTestApi::class)
// Key presses in a test reach only the focused node, unlike the window, which sees them all.
private fun SemanticsNodeInteraction.ctrl(key: Key) =
  requestFocus().performKeyInput { withKeyDown(Key.CtrlLeft) { pressKey(key) } }
