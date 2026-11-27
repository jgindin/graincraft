package com.jaygindin.graincraft.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import com.jaygindin.graincraft.app.testutil.FakePlatform
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AppSmokeTest {
  @Test
  fun showsTheRail() = runComposeUiTest {
    setContent { App(FakePlatform().graph) }
    onNodeWithTag("rail-designs").assertExists()
  }
}
