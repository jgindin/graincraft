package com.jaygindin.graincraft.app.ui.shop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaygindin.graincraft.app.data.WorkspaceStore
import com.jaygindin.graincraft.core.shop.ShopProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The shop profile: loaded once from the store, saved on every change. */
class ShopViewModel(private val store: WorkspaceStore) : ViewModel() {
  private val state = MutableStateFlow(ShopProfile())
  val profile: StateFlow<ShopProfile> = state.asStateFlow()

  // Saves queue in order, so a slow save never lands after a newer one.
  private val saving = Mutex()
  private var changed = false

  init {
    viewModelScope.launch {
      val loaded = store.loadShopProfile()
      // A change made while loading is newer than what is on disk.
      if (!changed) state.value = loaded
    }
  }

  fun update(p: ShopProfile) {
    changed = true
    state.value = p
    viewModelScope.launch { saving.withLock { store.saveShopProfile(state.value) } }
  }

  fun resetToDefaults() = update(ShopProfile())
}
