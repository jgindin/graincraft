package com.jaygindin.graincraft.app.ui.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.jaygindin.graincraft.app.App
import com.jaygindin.graincraft.app.data.AppSettings
import com.jaygindin.graincraft.app.testutil.FakePlatform
import com.jaygindin.graincraft.app.testutil.FakeWorkspaceStore
import com.jaygindin.graincraft.app.ui.theme.GrainCraftDarkColorScheme
import com.jaygindin.graincraft.app.ui.theme.GrainCraftLightColorScheme
import com.jaygindin.graincraft.app.ui.theme.ThemeChoice
import com.jaygindin.graincraft.core.units.FractionPrecision
import com.jaygindin.graincraft.core.units.LengthUnit
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SettingsScreenTest {
  private fun ComposeUiTest.openSettings(platform: FakePlatform) {
    setContent { App(platform.graph) }
    onNodeWithTag("rail-settings").performClick()
  }

  /** The colour in the window's bottom-right corner, which is always plain background. */
  private fun ComposeUiTest.cornerColour(): Color {
    val pixels = onRoot().captureToImage().toPixelMap()
    return pixels[pixels.width - 4, pixels.height - 4]
  }

  @Test
  fun unitsSettingIsSavedAndApplied() = runComposeUiTest {
    val platform = FakePlatform()
    openSettings(platform)
    // 1-17/32" rounds to 1-9/16" at the default precision.
    onNodeWithText("Example: 1-9/16\"").assertExists()

    onNodeWithTag("settings-units-metric").performClick()

    waitUntil { platform.store.savedSettings.lastOrNull()?.units == LengthUnit.MILLIMETRE }
    onNodeWithText("Example: 38.9 mm").assertExists()
    // Fractions mean nothing in millimetres.
    onNodeWithTag("settings-precision-32").assertIsNotEnabled()
  }

  @Test
  fun precisionSettingIsSavedAndApplied() = runComposeUiTest {
    val platform = FakePlatform()
    openSettings(platform)

    onNodeWithTag("settings-precision-32").performClick()

    waitUntil {
      platform.store.savedSettings.lastOrNull()?.precision == FractionPrecision.THIRTY_SECOND
    }
    onNodeWithText("Example: 1-17/32\"").assertExists()
  }

  @Test
  fun savedSettingsAreShownOnStart() = runComposeUiTest {
    val platform = FakePlatform(FakeWorkspaceStore(AppSettings(units = LengthUnit.MILLIMETRE)))
    openSettings(platform)
    waitUntil { onAllNodesWithText("Example: 38.9 mm").fetchSemanticsNodes().isNotEmpty() }
  }

  @Test
  fun themeOverride() = runComposeUiTest {
    val platform = FakePlatform(FakeWorkspaceStore(AppSettings(theme = ThemeChoice.LIGHT)))
    openSettings(platform)
    waitForIdle()
    assertEquals(GrainCraftLightColorScheme.background, cornerColour())

    onNodeWithTag("settings-theme-dark").performClick()

    waitUntil { platform.store.savedSettings.lastOrNull()?.theme == ThemeChoice.DARK }
    waitForIdle()
    assertEquals(GrainCraftDarkColorScheme.background, cornerColour())
  }

  @Test
  fun openFolderUsesBrowser() = runComposeUiTest {
    val platform = FakePlatform()
    openSettings(platform)
    onNodeWithText(platform.store.rootDescription).assertExists()

    onNodeWithTag("settings-open-folder").performClick()

    assertEquals(listOf(platform.store.rootDescription), platform.browser.openedFolders)
  }
}
