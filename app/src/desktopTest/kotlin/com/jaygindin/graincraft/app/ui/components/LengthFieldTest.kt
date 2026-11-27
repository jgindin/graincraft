package com.jaygindin.graincraft.app.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import com.jaygindin.graincraft.app.ui.theme.GrainCraftTheme
import com.jaygindin.graincraft.core.units.Length
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class LengthFieldTest {
  @Test
  fun incompleteInputShowsAnErrorAndFinishedInputCommits() = runComposeUiTest {
    val commits = mutableListOf<Length>()
    var value by mutableStateOf(Length.inches(1))
    setContent {
      GrainCraftTheme(false) {
        LengthField(
          "Kerf",
          value,
          {
            commits += it
            value = it
          },
          testTag = "field",
        )
      }
    }

    onNodeWithTag("field").performTextReplacement("1-3/")
    waitForIdle()
    assertEquals(emptyList(), commits)
    onNodeWithText("Not a length", substring = true).assertExists()

    onNodeWithTag("field").performTextReplacement("1-3/4")
    waitForIdle()
    assertEquals(listOf(Length.inches(1, 3, 4)), commits)
    // Typing is never rewritten under the user's fingers.
    onNodeWithTag("field").assertTextEquals("Kerf", "1-3/4", includeEditableText = true)
  }

  @Test
  fun showsTheValueInTheChosenUnits() = runComposeUiTest {
    setContent {
      GrainCraftTheme(false) { LengthField("Kerf", Length.inches(0, 1, 8), {}, testTag = "field") }
    }
    onNodeWithTag("field").assertTextEquals("Kerf", "1/8\"", includeEditableText = true)
  }

  @Test
  fun zeroIsRefusedWhenItIsNotAllowed() = runComposeUiTest {
    val commits = mutableListOf<Length>()
    setContent {
      GrainCraftTheme(false) {
        LengthField(
          "Kerf",
          Length.inches(1),
          { commits += it },
          testTag = "field",
          allowZero = false,
        )
      }
    }
    onNodeWithTag("field").performTextReplacement("0")
    waitForIdle()
    assertEquals(emptyList(), commits)
    onNodeWithText("more than zero", substring = true).assertExists()
  }
}
