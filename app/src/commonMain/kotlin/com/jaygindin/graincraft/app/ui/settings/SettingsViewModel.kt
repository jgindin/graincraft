package com.jaygindin.graincraft.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaygindin.graincraft.app.data.AppSettings
import com.jaygindin.graincraft.app.data.WindowPlacement
import com.jaygindin.graincraft.app.data.WorkspaceStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The app-wide settings: loaded once from the store, saved on every change. */
class SettingsViewModel(private val store: WorkspaceStore) : ViewModel() {
  private val state = MutableStateFlow(AppSettings())
  val settings: StateFlow<AppSettings> = state.asStateFlow()

  // Saves queue in order, so a slow save never lands after a newer one.
  private val saving = Mutex()
  private var changed = false
  private val loading: Job

  init {
    loading = viewModelScope.launch {
      val loaded = store.loadSettings()
      // A change made while loading is newer than what is on disk.
      if (!changed) state.value = loaded
    }
  }

  fun update(s: AppSettings) {
    changed = true
    state.value = s
    viewModelScope.launch { saving.withLock { store.saveSettings(state.value) } }
  }

  /**
   * Records where the window is and writes it, returning once it is on disk. It queues behind any
   * other settings write, and waits for the first load so the other settings are not lost.
   */
  suspend fun saveWindow(placement: WindowPlacement) {
    loading.join()
    changed = true
    state.value = state.value.copy(window = placement)
    saving.withLock { store.saveSettings(state.value) }
  }
}
