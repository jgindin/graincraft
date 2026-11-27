package com.jaygindin.graincraft.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaygindin.graincraft.app.data.AppSettings
import com.jaygindin.graincraft.app.data.WorkspaceStore
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

  init {
    viewModelScope.launch {
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
}
