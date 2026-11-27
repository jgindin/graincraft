package com.jaygindin.graincraft.app.ui.library

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.jaygindin.graincraft.app.App
import com.jaygindin.graincraft.app.data.StoredDesign
import com.jaygindin.graincraft.app.testutil.FakePlatform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class LibraryScreenTest {
  @Test
  fun unreadableDesignShowsCantOpen() = runComposeUiTest {
    val platform = FakePlatform()
    platform.store.unreadable += StoredDesign.Unreadable("broken.graincraft.json", "Unexpected end")
    setContent { App(platform.graph) }
    waitForIdle()

    onNodeWithTag("unreadable-broken.graincraft.json").assertExists()
    onNodeWithText("Can't open").assertExists()
    onNodeWithText("Unexpected end", substring = true).assertExists()
  }

  @Test
  fun edgeGrainDisablesCheckerAndBrick() = runComposeUiTest {
    setContent { App(FakePlatform().graph) }
    onNodeWithTag("library-new").performClick()

    onNodeWithTag("new-pattern-checkerboard").assertIsEnabled()
    onNodeWithTag("new-grain-edge").performClick()
    onNodeWithTag("new-pattern-checkerboard").assertIsNotEnabled()
    onNodeWithTag("new-pattern-brick").assertIsNotEnabled()
    onNodeWithTag("new-pattern-stripes").assertIsEnabled()
  }

  @Test
  fun newDesignDialogCreatesAndOpens() = runComposeUiTest {
    val platform = FakePlatform()
    val opened = mutableListOf<String>()
    setContent {
      com.jaygindin.graincraft.app.ui.theme.GrainCraftTheme(false) {
        LibraryScreen(
          viewModel =
            androidx.lifecycle.viewmodel.compose.viewModel {
              LibraryViewModel(
                platform.graph,
                { com.jaygindin.graincraft.core.wood.Woods(emptyList()) },
                { com.jaygindin.graincraft.core.shop.ShopProfile() },
              )
            },
          onOpen = { opened += it },
        )
      }
    }
    onNodeWithTag("library-new").performClick()
    onNodeWithTag("new-name").performTextInput("Serving board")
    onNodeWithTag("new-pattern-brick").performClick()
    onNodeWithTag("new-size-16x10").performClick()
    onNodeWithTag("new-create").performClick()
    waitForIdle()

    val design = platform.store.designs.values.single()
    assertEquals("Serving board", design.name)
    assertTrue(design.pattern is com.jaygindin.graincraft.core.design.PatternSpec.Brick)
    assertEquals(listOf(design.id), opened)
  }

  @Test
  fun deleteShowsUndoSnackbar() = runComposeUiTest {
    val platform = FakePlatform()
    platform.store.designs["a"] = sampleDesign("a", "Walnut board")
    setContent { App(platform.graph) }
    waitForIdle()

    onNodeWithTag("design-menu-a").performClick()
    onNodeWithTag("menu-delete").performClick()
    waitForIdle()
    onNodeWithText("Deleted Walnut board").assertExists()
    assertTrue("a" in platform.store.trash)

    onNodeWithTag("snackbar-undo").performClick()
    waitForIdle()
    assertTrue("a" in platform.store.designs)
  }
}
