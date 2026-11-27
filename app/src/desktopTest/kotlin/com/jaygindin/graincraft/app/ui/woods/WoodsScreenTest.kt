package com.jaygindin.graincraft.app.ui.woods

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.jaygindin.graincraft.app.App
import com.jaygindin.graincraft.app.testutil.FakePlatform
import com.jaygindin.graincraft.core.wood.Rating
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class WoodsScreenTest {
  private fun ComposeUiTest.openWoods(platform: FakePlatform) {
    setContent { App(platform.graph) }
    onNodeWithTag("rail-woods").performClick()
    waitForIdle()
  }

  @Test
  fun searchNarrowsTheList() = runComposeUiTest {
    openWoods(FakePlatform())
    onNodeWithTag("wood-row-hard-maple").assertExists()

    onNodeWithTag("woods-search").performTextReplacement("oak")
    waitForIdle()

    onNodeWithTag("wood-row-red-oak").assertExists()
    onNodeWithTag("wood-row-white-oak").assertExists()
    onNodeWithTag("wood-row-hard-maple").assertDoesNotExist()
  }

  @Test
  fun expandingARowShowsTheReasons() = runComposeUiTest {
    openWoods(FakePlatform())
    onNodeWithTag("wood-reasons-red-oak").assertDoesNotExist()

    onNodeWithTag("wood-row-red-oak").performClick()
    waitForIdle()

    onNodeWithTag("wood-reasons-red-oak").assertExists()
  }

  @Test
  fun customWoodShowsComputedRating() = runComposeUiTest {
    openWoods(FakePlatform())
    onNodeWithTag("woods-add-custom").performClick()
    onNodeWithTag("custom-janka").performTextReplacement("550")
    waitForIdle()

    onNodeWithTag("custom-rating-edge").assertTextContains("Not suitable", substring = true)
    onNodeWithTag("custom-rating-end").assertTextContains("Not suitable", substring = true)
  }

  @Test
  fun badHexShowsAnErrorAndBlocksSaving() = runComposeUiTest {
    openWoods(FakePlatform())
    onNodeWithTag("woods-add-custom").performClick()
    onNodeWithTag("custom-name").performTextReplacement("Pear")
    waitForIdle()
    onNodeWithTag("custom-save").assertIsEnabled()

    onNodeWithTag("custom-raw-hex").performTextReplacement("#12G")
    waitForIdle()

    onNodeWithText("Use six hex digits", substring = true).assertExists()
    onNodeWithTag("custom-save").assertIsNotEnabled()
  }

  @Test
  fun overrideKeepsReason() = runComposeUiTest {
    val platform = FakePlatform()
    openWoods(platform)
    onNodeWithTag("woods-add-custom").performClick()
    onNodeWithTag("custom-name").performTextReplacement("Pear")
    onNodeWithTag("custom-janka").performTextReplacement("1200")
    onNodeWithTag("custom-override").performClick()
    waitForIdle()
    // A reason is required once the rating is overridden.
    onNodeWithTag("custom-save").assertIsNotEnabled()

    onNodeWithTag("custom-override-rating").performClick()
    onNodeWithTag("custom-override-rating-NOT_SUITABLE").performClick()
    onNodeWithTag("custom-override-reason").performTextReplacement("Cracks when oiled")
    waitForIdle()
    onNodeWithTag("custom-save").assertIsEnabled().performClick()
    waitUntil { platform.store.customWoods.isNotEmpty() }

    val saved = platform.store.customWoods.single()
    assertTrue(saved.id.startsWith("custom-"))
    assertTrue(saved.custom)
    assertEquals(Rating.NOT_SUITABLE, saved.suitability.edgeGrain.rating)
    assertEquals(listOf("Cracks when oiled"), saved.suitability.endGrain.reasons)
    // Custom woods sort after the built-ins, so look at just those.
    onNodeWithTag("woods-filter-custom").performClick()
    onNodeWithTag("wood-row-${saved.id}").assertExists()
  }

  @Test
  fun deletingACustomWoodAsksFirst() = runComposeUiTest {
    val platform = FakePlatform()
    openWoods(platform)
    onNodeWithTag("woods-add-custom").performClick()
    onNodeWithTag("custom-name").performTextReplacement("Pear")
    onNodeWithTag("custom-save").performClick()
    waitUntil { platform.store.customWoods.isNotEmpty() }
    val id = platform.store.customWoods.single().id
    onNodeWithTag("woods-filter-custom").performClick()

    onNodeWithTag("wood-row-$id").performClick()
    onNodeWithTag("wood-delete-$id").performClick()
    assertEquals(1, platform.store.customWoods.size)
    onNodeWithTag("wood-delete-confirm").performClick()

    waitUntil { platform.store.customWoods.isEmpty() }
    onNodeWithTag("wood-row-$id").assertDoesNotExist()
  }
}
