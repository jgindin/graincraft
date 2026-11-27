package com.jaygindin.graincraft.app.ui.editor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.jaygindin.graincraft.app.platform.SaveResult
import com.jaygindin.graincraft.app.testutil.FakeFileDialogs
import com.jaygindin.graincraft.app.testutil.FakePlatform
import com.jaygindin.graincraft.app.ui.preview.PreviewExamples
import com.jaygindin.graincraft.app.ui.preview.PreviewExamples.inch
import com.jaygindin.graincraft.core.packet.ShopPacketExporter
import com.jaygindin.graincraft.core.shop.ShopProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

private const val WIDE = 1400
private const val TALL = 900

@OptIn(ExperimentalTestApi::class)
class ExportPacketTest {
  private val compiled = PreviewExamples.compile(PreviewExamples.a.copy(name = "Walnut checker"))

  @Test
  fun exportUsesDefaultName() = runTest {
    val dialogs = FakeFileDialogs()
    val saved = exportPacket(compiled, PreviewExamples.woods, ShopProfile(), inchFormatter, dialogs)

    val save = dialogs.saves.single()
    // The dialog adds the extension itself, so the name and extension together are the file name.
    assertEquals("Walnut checker - shop packet.html", "${save.suggestedName}.${save.extension}")
    val html =
      ShopPacketExporter.html(compiled, PreviewExamples.woods, ShopProfile(), inchFormatter)
    assertEquals(html, save.contents)
    assertEquals(dialogs.saveResult, saved)
  }

  @Test
  fun cancelledExportDoesNothing() {
    runTest {
      val dialogs = FakeFileDialogs().apply { saveResult = SaveResult.Cancelled }
      assertEquals(
        SaveResult.Cancelled,
        exportPacket(compiled, PreviewExamples.woods, ShopProfile(), inchFormatter, dialogs),
      )
    }
    runDesktopComposeUiTest(WIDE, TALL) {
      val platform = FakePlatform()
      platform.dialogs.saveResult = SaveResult.Cancelled
      openInApp(platform)

      onNodeWithTag("editor-export").performClick()
      awaitUntil { platform.dialogs.saves.isNotEmpty() }
      waitForIdle()
      onNodeWithTag("snackbar-open-packet").assertDoesNotExist()
      assertTrue(platform.browser.openedFiles.isEmpty())
    }
  }

  @Test
  fun openInBrowserOpensSavedFile() =
    runDesktopComposeUiTest(WIDE, TALL) {
      val platform = FakePlatform()
      platform.dialogs.saveResult =
        SaveResult.Saved("/home/maker/Documents/Example - shop packet.html")
      openInApp(platform)

      onNodeWithTag("editor-export").performClick()
      awaitTag("snackbar-open-packet")
      onNodeWithText("Packet saved").assertExists()
      assertTrue(platform.dialogs.saves.single().contents.startsWith("<!DOCTYPE html>"))
      assertTrue(platform.browser.openedFiles.isEmpty())

      onNodeWithTag("snackbar-open-packet").performClick()
      awaitUntil { platform.browser.openedFiles.isNotEmpty() }
      assertEquals(
        listOf("/home/maker/Documents/Example - shop packet.html"),
        platform.browser.openedFiles,
      )
    }

  @Test
  fun aFailedSaveSaysWhyAndTheEditorKeepsSaving() =
    runDesktopComposeUiTest(WIDE, TALL) {
      val platform = FakePlatform()
      platform.dialogs.saveResult = SaveResult.Failed("No space left on device")
      openInApp(platform)
      // An edit still waiting for autosave when the export fails.
      onNodeWithTag("dim-width").performTextReplacement("14")

      onNodeWithTag("editor-export").performClick()
      awaitUntil { allText().contains("Couldn't save: No space left on device") }
      onNodeWithTag("snackbar-open-packet").assertDoesNotExist()
      assertTrue(platform.browser.openedFiles.isEmpty())

      awaitUntil { platform.store.designs.getValue("a").finished.width == inch(14) }
      onNodeWithTag("dim-length").performTextReplacement("20")
      awaitUntil { platform.store.designs.getValue("a").finished.length == inch(20) }
    }

  @Test
  fun exportIsDisabledWhileTheDesignCannotBeBuilt() =
    runDesktopComposeUiTest(WIDE, TALL) {
      openInApp(FakePlatform())
      // A 40" board is over the 36" limit.
      onNodeWithTag("dim-length").performTextReplacement("40")
      awaitTag("compile-error")
      onNodeWithTag("editor-export").assertIsNotEnabled()
    }
}
