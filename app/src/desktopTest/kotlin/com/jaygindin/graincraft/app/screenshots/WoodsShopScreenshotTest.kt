package com.jaygindin.graincraft.app.screenshots

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
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
class WoodsShopScreenshotTest {
  private fun shot(
    theme: ThemeChoice,
    name: String,
    setUp: ComposeUiTest.() -> Unit,
    dialog: Boolean = false,
  ) =
    runDesktopComposeUiTest(SCREENSHOT_WIDTH, SCREENSHOT_HEIGHT) {
      val platform = FakePlatform(FakeWorkspaceStore(AppSettings(theme = theme)))
      setContent { AppShell(platform.graph, version = SCREENSHOT_VERSION) }
      setUp()
      waitForIdle()
      // A dialog is its own root; its layer is drawn over the whole window.
      onAllNodes(isRoot())[if (dialog) 1 else 0].captureRoboImage(
        "screenshots/$name.png",
        screenshotOptions,
      )
    }

  private fun woods(theme: ThemeChoice, name: String) =
    shot(
      theme,
      name,
      {
        onNodeWithTag("rail-woods").performClick()
        onNodeWithTag("wood-row-red-oak").performClick()
      },
    )

  private fun customDialog(theme: ThemeChoice, name: String) =
    shot(
      theme,
      name,
      {
        onNodeWithTag("rail-woods").performClick()
        onNodeWithTag("woods-add-custom").performClick()
        onNodeWithTag("custom-name").performTextReplacement("Pear")
        onNodeWithTag("custom-janka").performTextReplacement("550")
      },
      dialog = true,
    )

  private fun shop(theme: ThemeChoice, name: String) =
    shot(theme, name, { onNodeWithTag("rail-shop").performClick() })

  @Test fun woodsLight() = woods(ThemeChoice.LIGHT, "woods_light")

  @Test fun woodsDark() = woods(ThemeChoice.DARK, "woods_dark")

  @Test fun customWoodLight() = customDialog(ThemeChoice.LIGHT, "custom_wood_light")

  @Test fun customWoodDark() = customDialog(ThemeChoice.DARK, "custom_wood_dark")

  @Test fun shopLight() = shop(ThemeChoice.LIGHT, "shop_light")

  @Test fun shopDark() = shop(ThemeChoice.DARK, "shop_dark")
}
