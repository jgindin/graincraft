package com.jaygindin.graincraft.app.screenshots

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.jaygindin.graincraft.app.AppShell
import com.jaygindin.graincraft.app.data.AppSettings
import com.jaygindin.graincraft.app.testutil.FakePlatform
import com.jaygindin.graincraft.app.testutil.FakeWorkspaceStore
import com.jaygindin.graincraft.app.testutil.SCREENSHOT_HEIGHT
import com.jaygindin.graincraft.app.testutil.SCREENSHOT_VERSION
import com.jaygindin.graincraft.app.testutil.SCREENSHOT_WIDTH
import com.jaygindin.graincraft.app.testutil.screenshotOptions
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
}
