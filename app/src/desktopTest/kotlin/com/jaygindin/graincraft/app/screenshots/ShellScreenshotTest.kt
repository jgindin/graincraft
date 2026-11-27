package com.jaygindin.graincraft.app.screenshots

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.jaygindin.graincraft.app.AppShell
import com.jaygindin.graincraft.app.data.AppSettings
import com.jaygindin.graincraft.app.testutil.FakePlatform
import com.jaygindin.graincraft.app.testutil.FakeWorkspaceStore
import com.jaygindin.graincraft.app.testutil.SCREENSHOT_HEIGHT
import com.jaygindin.graincraft.app.testutil.SCREENSHOT_VERSION
import com.jaygindin.graincraft.app.testutil.SCREENSHOT_WIDTH
import com.jaygindin.graincraft.app.testutil.screenshotOptions
import com.jaygindin.graincraft.app.ui.library.PatternKind
import com.jaygindin.graincraft.app.ui.library.sampleDesign
import com.jaygindin.graincraft.app.ui.theme.ThemeChoice
import io.github.takahirom.roborazzi.captureRoboImage
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class ShellScreenshotTest {
  private fun settings(theme: ThemeChoice, name: String) =
    runDesktopComposeUiTest(SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT) {
      val platform = FakePlatform(FakeWorkspaceStore(AppSettings(theme = theme)))
      setContent { AppShell(platform.graph, version = SCREENSHOT_VERSION) }
      onNodeWithTag("rail-settings").performClick()
      waitForIdle()
      onRoot().captureRoboImage("screenshots/$name.png", screenshotOptions)
    }

  @Test fun settingsLight() = settings(ThemeChoice.LIGHT, "settings_light")

  @Test fun settingsDark() = settings(ThemeChoice.DARK, "settings_dark")

  private fun library(theme: ThemeChoice, name: String, designs: Boolean, dialog: Boolean = false) =
    runDesktopComposeUiTest(SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT) {
      val platform = FakePlatform(FakeWorkspaceStore(AppSettings(theme = theme)))
      if (designs) {
        listOf(
            sampleDesign(
              "a",
              "Walnut & Maple Checker",
              PatternKind.CHECKERBOARD,
              1_700_300_000_000,
            ),
            sampleDesign("b", "Stripes Serving Board", PatternKind.STRIPES, 1_700_200_000_000),
            sampleDesign("c", "Cherry Brick", PatternKind.BRICK, 1_700_100_000_000),
          )
          .forEach { platform.store.designs[it.id] = it }
      }
      setContent { AppShell(platform.graph, version = SCREENSHOT_VERSION) }
      waitForIdle()
      if (dialog) {
        onNodeWithTag("library-new").performClick()
        onNodeWithTag("new-name").performTextInput("Serving board")
        waitForIdle()
      }
      // A dialog is its own root; its layer is drawn over the whole window.
      onAllNodes(isRoot())[if (dialog) 1 else 0].captureRoboImage(
        "screenshots/$name.png",
        screenshotOptions,
      )
    }

  @Test fun libraryLight() = library(ThemeChoice.LIGHT, "library_light", designs = true)

  @Test fun libraryDark() = library(ThemeChoice.DARK, "library_dark", designs = true)

  @Test fun libraryEmptyLight() = library(ThemeChoice.LIGHT, "library_empty_light", designs = false)

  @Test fun libraryEmptyDark() = library(ThemeChoice.DARK, "library_empty_dark", designs = false)

  @Test
  fun newDesignDialogLight() =
    library(ThemeChoice.LIGHT, "new_design_light", designs = true, dialog = true)

  @Test
  fun newDesignDialogDark() =
    library(ThemeChoice.DARK, "new_design_dark", designs = true, dialog = true)
}
