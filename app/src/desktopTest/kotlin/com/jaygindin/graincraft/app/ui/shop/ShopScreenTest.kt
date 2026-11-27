package com.jaygindin.graincraft.app.ui.shop

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.jaygindin.graincraft.app.App
import com.jaygindin.graincraft.app.testutil.FakePlatform
import com.jaygindin.graincraft.app.testutil.FakeWorkspaceStore
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.Length
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ShopScreenTest {
  private fun ComposeUiTest.openShop(platform: FakePlatform) {
    setContent { App(platform.graph) }
    onNodeWithTag("rail-shop").performClick()
    waitForIdle()
  }

  @Test
  fun kerfChangeIsSaved() = runComposeUiTest {
    val platform = FakePlatform()
    openShop(platform)

    onNodeWithTag("shop-kerf").performTextReplacement("3/32")

    waitUntil { platform.store.savedShopProfiles.isNotEmpty() }
    assertEquals(Length.inches(0, 3, 32), platform.store.shopProfile.kerf)
  }

  @Test
  fun millingWasteIsAPercentage() = runComposeUiTest {
    val platform = FakePlatform()
    openShop(platform)

    onNodeWithTag("shop-millingWaste").performScrollTo().performTextReplacement("25")
    waitUntil { platform.store.savedShopProfiles.isNotEmpty() }
    assertEquals(0.25, platform.store.shopProfile.millingWaste)

    // Out of range is not saved.
    val saves = platform.store.savedShopProfiles.size
    onNodeWithTag("shop-millingWaste").performTextReplacement("120")
    waitForIdle()
    assertEquals(saves, platform.store.savedShopProfiles.size)
  }

  @Test
  fun machineSwitchesAddAndRemoveTheWidth() = runComposeUiTest {
    val platform = FakePlatform()
    openShop(platform)
    onNodeWithTag("shop-drumSanderMaxWidth").assertDoesNotExist()

    onNodeWithTag("shop-drumSanderMaxWidth-switch").performScrollTo().performClick()
    waitUntil { platform.store.savedShopProfiles.isNotEmpty() }
    onNodeWithTag("shop-drumSanderMaxWidth").assertExists()
    onNodeWithTag("shop-planerMaxWidth-switch").performScrollTo().performClick()

    waitUntil { platform.store.shopProfile.planerMaxWidth == null }
    onNodeWithTag("shop-planerMaxWidth").assertDoesNotExist()
    assertEquals(ShopProfile().crosscutMaxWidth, platform.store.shopProfile.crosscutMaxWidth)
  }

  @Test
  fun resetAsksFirst() = runComposeUiTest {
    val platform =
      FakePlatform(
        FakeWorkspaceStore(shopProfile = ShopProfile(glue = "Hide glue", planerMaxWidth = null)),
      )
    openShop(platform)

    onNodeWithTag("shop-reset").performClick()
    waitForIdle()
    assertEquals("Hide glue", platform.store.shopProfile.glue)

    onNodeWithTag("shop-reset-cancel").performClick()
    waitForIdle()
    assertEquals("Hide glue", platform.store.shopProfile.glue)

    onNodeWithTag("shop-reset").performClick()
    onNodeWithTag("shop-reset-confirm").performClick()
    waitUntil { platform.store.shopProfile == ShopProfile() }
  }
}
