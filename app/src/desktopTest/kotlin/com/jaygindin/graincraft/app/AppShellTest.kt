package com.jaygindin.graincraft.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.jaygindin.graincraft.app.testutil.FakePlatform
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AppShellTest {
  @Test
  fun railSwitchesDestinations() = runComposeUiTest {
    setContent { App(FakePlatform().graph) }

    onNodeWithTag("rail-designs").assertIsSelected()
    onNodeWithTag("screen-designs").assertExists()

    for (name in listOf("woods", "shop", "settings", "designs")) {
      onNodeWithTag("rail-$name").performClick()
      onNodeWithTag("rail-$name").assertIsSelected()
      onNodeWithTag("screen-$name").assertExists()
      for (other in listOf("designs", "woods", "shop", "settings") - name) {
        onNodeWithTag("rail-$other").assertIsNotSelected()
        onNodeWithTag("screen-$other").assertDoesNotExist()
      }
    }
  }
}
