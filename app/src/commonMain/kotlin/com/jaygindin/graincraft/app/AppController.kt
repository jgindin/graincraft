package com.jaygindin.graincraft.app

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaygindin.graincraft.app.data.WindowPlacement
import com.jaygindin.graincraft.app.ui.editor.EditorScreen
import com.jaygindin.graincraft.app.ui.editor.EditorViewModel
import com.jaygindin.graincraft.app.ui.editor.rememberPacketExport
import com.jaygindin.graincraft.app.ui.settings.SettingsViewModel
import com.jaygindin.graincraft.core.compile.CompileResult
import com.jaygindin.graincraft.core.design.BoardDesign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withTimeoutOrNull

/** How long closing the window waits for each write before it exits anyway. */
const val CLOSE_TIMEOUT_MILLIS = 5_000L

/** What a keyboard shortcut asks the app to do. */
enum class AppAction {
  NEW_DESIGN,
  UNDO,
  REDO,
  EXPORT_PACKET,
  FIND_WOOD,
}

/**
 * What the window needs from the running app: its shortcuts, and the writes to finish before it
 * closes. The screens on show register what each shortcut does there.
 */
class AppController {
  internal var openEditor: EditorViewModel? = null

  /** The settings the Settings screen edits; the window placement is written through them too. */
  internal var settings: SettingsViewModel? = null

  // Later registrations win, so the editor's undo is used while it is open.
  private val handlers = mutableListOf<Map<AppAction, () -> Unit>>()

  /** Does [action] if a screen on show handles it; false leaves the key to whatever has focus. */
  fun perform(action: AppAction): Boolean {
    val handler = handlers.lastOrNull { action in it }?.getValue(action) ?: return false
    handler()
    return true
  }

  /** Makes [actions] available until the returned function is called. */
  internal fun register(actions: Map<AppAction, () -> Unit>): () -> Unit {
    handlers += actions
    return { handlers.removeAll { it === actions } }
  }

  /** Writes any pending change of the open design; does nothing when none is open. */
  suspend fun flushOpenEditor() {
    openEditor?.flush()
  }

  /**
   * Writes the open design and then [placement] before the window closes. Each write gets
   * [CLOSE_TIMEOUT_MILLIS], so a disk that has stopped answering cannot keep the app open forever.
   */
  suspend fun prepareToClose(placement: WindowPlacement?) {
    // The writes run apart from the caller, since a write that has started cannot be cancelled
    // and the caller must be able to stop waiting for it.
    val detached = CoroutineScope(currentCoroutineContext().minusKey(Job))
    awaitAtMost(detached.async { flushOpenEditor() })
    val settings = settings
    if (placement != null && settings != null) {
      awaitAtMost(detached.async { settings.saveWindow(placement) })
    }
  }

  private suspend fun awaitAtMost(write: Deferred<Unit>) {
    withTimeoutOrNull(CLOSE_TIMEOUT_MILLIS) { write.await() }
  }
}

/**
 * Gives [controller] the app's [settings] while this is in the composition: closing the window
 * writes its placement through them, so that write never races a change made in Settings.
 */
@Composable
internal fun RegisterSettings(controller: AppController, settings: SettingsViewModel) {
  DisposableEffect(controller, settings) {
    controller.settings = settings
    onDispose { if (controller.settings === settings) controller.settings = null }
  }
}

/** Registers [actions] with [controller] while this is in the composition. */
@Composable
internal fun RegisterActions(controller: AppController, actions: Map<AppAction, () -> Unit>) {
  val current by rememberUpdatedState(actions)
  DisposableEffect(controller) {
    val stable = current.keys.associateWith { key -> { current[key]?.invoke() ?: Unit } }
    val unregister = controller.register(stable)
    onDispose { unregister() }
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
  graph: AppGraph,
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
    val focus = LocalFocusManager.current
    val snackbars = remember { SnackbarHostState() }
    val export = rememberPacketExport(editor, graph.dialogs, graph.browser, snackbars)
    RegisterActions(
      controller,
      mapOf(
        // As the buttons do: a field being typed in would keep showing its text otherwise.
        AppAction.UNDO to
          {
            focus.clearFocus()
            editor.undo()
          },
        AppAction.REDO to
          {
            focus.clearFocus()
            editor.redo()
          },
        AppAction.EXPORT_PACKET to { if (editor.state.value.compile is CompileResult.Ok) export() },
      ),
    )
    EditorScreen(editor, onBack = onBack, onExport = export, snackbars = snackbars)
  }
}

private class Loaded(val design: BoardDesign?)

private class EditorStoreOwner : ViewModelStoreOwner {
  override val viewModelStore = ViewModelStore()
}
