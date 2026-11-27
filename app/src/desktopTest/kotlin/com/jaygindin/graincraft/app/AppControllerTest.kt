package com.jaygindin.graincraft.app

import com.jaygindin.graincraft.app.data.AppSettings
import com.jaygindin.graincraft.app.data.WindowPlacement
import com.jaygindin.graincraft.app.testutil.FakeWorkspaceStore
import com.jaygindin.graincraft.app.ui.editor.EditorInputs
import com.jaygindin.graincraft.app.ui.editor.GatedStore
import com.jaygindin.graincraft.app.ui.editor.editor
import com.jaygindin.graincraft.app.ui.preview.PreviewExamples
import com.jaygindin.graincraft.app.ui.preview.PreviewExamples.inch
import com.jaygindin.graincraft.app.ui.settings.SettingsViewModel
import com.jaygindin.graincraft.app.ui.theme.ThemeChoice
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/** Closing the window: the open design and the window placement are written before exit. */
@OptIn(ExperimentalCoroutinesApi::class)
class AppControllerTest {
  private val placement = WindowPlacement(40, 60, 1300, 820)

  @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

  @AfterTest fun tearDown() = Dispatchers.resetMain()

  @Test
  fun closeWritesTheOpenDesignAndThePlacement() = runTest {
    val store = FakeWorkspaceStore(settings = AppSettings(theme = ThemeChoice.DARK))
    val editor = editor(PreviewExamples.a, EditorInputs(store = store))
    val controller = AppController()
    controller.openEditor = editor
    controller.settings = SettingsViewModel(store)
    editor.edit { it.copy(finished = it.finished.copy(width = inch(14))) }

    controller.prepareToClose(placement)

    assertEquals(inch(14), store.designs.getValue(PreviewExamples.a.id).finished.width)
    // The placement joins the settings already there rather than replacing them.
    assertEquals(AppSettings(theme = ThemeChoice.DARK, window = placement), store.settings)
  }

  @Test
  fun closeKeepsAThemeChosenJustBefore() = runTest {
    val store = FakeWorkspaceStore()
    val settings = SettingsViewModel(store)
    val controller = AppController()
    controller.settings = settings

    settings.update(settings.settings.value.copy(theme = ThemeChoice.LIGHT))
    controller.prepareToClose(placement)

    assertEquals(AppSettings(theme = ThemeChoice.LIGHT, window = placement), store.settings)
  }

  @Test
  fun aStuckDiskDoesNotHoldTheCloseForever() = runTest {
    val gated = GatedStore()
    val editor = editor(PreviewExamples.a, EditorInputs(store = gated))
    val controller = AppController()
    controller.openEditor = editor
    controller.settings = SettingsViewModel(gated.inner)
    editor.edit { it.copy(finished = it.finished.copy(width = inch(14))) }

    controller.prepareToClose(placement)

    assertTrue(currentTime >= CLOSE_TIMEOUT_MILLIS, "gave up after $currentTime ms")
    assertTrue(gated.inner.designs.isEmpty())
    // The placement is still written, so the window opens where it was.
    assertEquals(placement, gated.inner.settings.window)
  }

  @Test
  fun closeWithNothingOpenStillSavesThePlacement() = runTest {
    val store = FakeWorkspaceStore()
    val controller = AppController()
    controller.settings = SettingsViewModel(store)

    controller.prepareToClose(placement)

    assertEquals(placement, store.settings.window)
  }
}
