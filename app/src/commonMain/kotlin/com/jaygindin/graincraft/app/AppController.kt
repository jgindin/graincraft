package com.jaygindin.graincraft.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaygindin.graincraft.app.ui.editor.EditorScreen
import com.jaygindin.graincraft.app.ui.editor.EditorViewModel
import com.jaygindin.graincraft.core.design.BoardDesign

/**
 * What the window needs from the running app. Closing the window awaits [flushOpenEditor], so the
 * last edit to an open design is written rather than lost to the autosave delay.
 */
class AppController {
  internal var openEditor: EditorViewModel? = null

  /** Writes any pending change of the open design; does nothing when none is open. */
  suspend fun flushOpenEditor() {
    openEditor?.flush()
  }
}

/**
 * The editor for design [id], once it is read. Its view model lives in a store of its own, cleared
 * when the editor leaves the screen, so reopening a design starts from the file.
 */
@Composable
internal fun EditorRoute(
  id: String,
  load: suspend (String) -> BoardDesign?,
  controller: AppController,
  onBack: () -> Unit,
  newEditor: (BoardDesign) -> EditorViewModel,
) {
  val loaded by produceState<Loaded?>(null, id) { value = Loaded(load(id)) }
  val design = loaded?.design
  if (loaded != null && design == null) {
    // The file went away (or cannot be read) between the library listing and the click.
    LaunchedEffect(id) { onBack() }
  }
  if (design == null) return
  val owner = remember(id) { EditorStoreOwner() }
  DisposableEffect(owner) { onDispose { owner.viewModelStore.clear() } }
  CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
    val editor = viewModel(key = "editor-$id") { newEditor(design) }
    DisposableEffect(editor) {
      controller.openEditor = editor
      onDispose { if (controller.openEditor === editor) controller.openEditor = null }
    }
    // The shop packet export is wired in by the window, which also owns the file dialogs.
    EditorScreen(editor, onBack = onBack, onExport = {})
  }
}

private class Loaded(val design: BoardDesign?)

private class EditorStoreOwner : ViewModelStoreOwner {
  override val viewModelStore = ViewModelStore()
}
