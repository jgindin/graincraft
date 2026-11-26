package com.jaygindin.graincraft.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class AppSmokeTest {
  @Test
  fun showsAppName() = runComposeUiTest {
    setContent { App() }
    onNodeWithText("GrainCraft Studio").assertExists()
  }
}
