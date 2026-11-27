package com.jaygindin.graincraft.app

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.jaygindin.graincraft.app.data.FileWorkspaceStore
import com.jaygindin.graincraft.app.data.WindowPlacement
import com.jaygindin.graincraft.app.data.WorkspaceStore
import com.jaygindin.graincraft.app.data.xdgDataRoot
import com.jaygindin.graincraft.app.platform.DesktopExternalBrowser
import com.jaygindin.graincraft.app.platform.DesktopFileDialogs
import com.jaygindin.graincraft.app.platform.SkiaArgbImageFactory
import java.awt.Dimension
import java.awt.GraphicsEnvironment
import java.awt.Rectangle
import java.nio.file.Path
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.runBlocking

private const val DEFAULT_WIDTH = 1280
private const val DEFAULT_HEIGHT = 800
private const val MIN_WIDTH = 1024
private const val MIN_HEIGHT = 700

fun main() {
  val clock = System::currentTimeMillis
  val root = Path.of(xdgDataRoot(System.getenv(), System.getProperty("user.home")))
  val store = FileWorkspaceStore(root, clock)
  val graph =
    AppGraph(
      store = store,
      dialogs = DesktopFileDialogs(),
      browser = DesktopExternalBrowser(),
      clock = clock,
      newId = { UUID.randomUUID().toString() },
      images = SkiaArgbImageFactory,
    )
  val saved = runBlocking { store.loadSettings().window }
  val placement = restorablePlacement(saved, screenBounds())

  application {
    val state =
      rememberWindowState(
        position =
          placement?.let { WindowPosition(it.x.dp, it.y.dp) } ?: WindowPosition.PlatformDefault,
        size =
          placement?.let { DpSize(it.width.dp, it.height.dp) }
            ?: DpSize(DEFAULT_WIDTH.dp, DEFAULT_HEIGHT.dp),
      )
    Window(
      onCloseRequest = {
        savePlacement(store, state)
        exitApplication()
      },
      state = state,
      title = "GrainCraft Studio",
    ) {
      LaunchedEffect(Unit) { window.minimumSize = Dimension(MIN_WIDTH, MIN_HEIGHT) }
      App(graph)
    }
  }
}

/**
 * [saved] if any part of it is on one of [screens]; a placement left over from a monitor that is
 * gone would open the window where nobody can see it.
 */
internal fun restorablePlacement(
  saved: WindowPlacement?,
  screens: List<Rectangle>,
): WindowPlacement? {
  if (saved == null) return null
  val bounds = Rectangle(saved.x, saved.y, saved.width, saved.height)
  return saved.takeIf { screens.any { it.intersects(bounds) } }
}

private fun screenBounds(): List<Rectangle> =
  GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices.map {
    it.defaultConfiguration.bounds
  }

/** Writes the window's position and size into the settings on disk, keeping everything else. */
private fun savePlacement(store: WorkspaceStore, state: WindowState) {
  val position = state.position as? WindowPosition.Absolute ?: return
  val placement =
    WindowPlacement(
      x = position.x.value.roundToInt(),
      y = position.y.value.roundToInt(),
      width = state.size.width.value.roundToInt(),
      height = state.size.height.value.roundToInt(),
    )
  runBlocking { store.saveSettings(store.loadSettings().copy(window = placement)) }
}
