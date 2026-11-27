package com.jaygindin.graincraft.app.ui.woods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaygindin.graincraft.app.data.WorkspaceStore
import com.jaygindin.graincraft.core.wood.Rating
import com.jaygindin.graincraft.core.wood.WoodSpecies
import com.jaygindin.graincraft.core.wood.Woods
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The filter chips above the woods list; several can be on at once. */
enum class WoodFilter {
  /** Only woods rated Recommended for both edge and end grain. */
  RECOMMENDED_ONLY,

  /** Hides woods rated Not suitable for either grain. */
  HIDE_NOT_SUITABLE,

  /** Only the user's own woods. */
  CUSTOM,
}

/**
 * The species the app knows: built-ins plus the user's custom woods, which are loaded once and
 * saved on every change. The editor and the library read [woods] too.
 */
class WoodsViewModel(private val store: WorkspaceStore) : ViewModel() {
  private val state = MutableStateFlow(Woods(emptyList()))
  val woods: StateFlow<Woods> = state.asStateFlow()

  private val queryState = MutableStateFlow("")
  val query: StateFlow<String> = queryState.asStateFlow()

  private val filterState = MutableStateFlow(emptySet<WoodFilter>())
  val filters: StateFlow<Set<WoodFilter>> = filterState.asStateFlow()

  /** [woods] narrowed by the search text and filters. */
  val visible: StateFlow<List<WoodSpecies>> =
    combine(state, queryState, filterState) { woods, query, filters ->
        woods.all.filter { matches(it, query, filters) }
      }
      .stateIn(viewModelScope, SharingStarted.Eagerly, state.value.all)

  // Saves queue in order, so a slow save never lands after a newer one.
  private val saving = Mutex()
  private var changed = false

  init {
    viewModelScope.launch {
      val loaded = store.loadCustomWoods()
      // A change made while loading is newer than what is on disk.
      if (!changed) state.value = Woods(loaded)
    }
  }

  fun setQuery(q: String) {
    queryState.value = q
  }

  fun setFilter(f: WoodFilter, on: Boolean) {
    filterState.value = if (on) filterState.value + f else filterState.value - f
  }

  /** Adds [w], or replaces the custom wood with the same id. */
  fun saveCustom(w: WoodSpecies) {
    val kept = state.value.customs
    val replaced = kept.any { it.id == w.id }
    change(if (replaced) kept.map { if (it.id == w.id) w else it } else kept + w)
  }

  fun deleteCustom(id: String) {
    change(state.value.customs.filterNot { it.id == id })
  }

  private fun change(customs: List<WoodSpecies>) {
    changed = true
    state.value = Woods(customs)
    viewModelScope.launch { saving.withLock { store.saveCustomWoods(state.value.customs) } }
  }
}

private fun matches(wood: WoodSpecies, query: String, filters: Set<WoodFilter>): Boolean {
  val needle = query.trim()
  val ratings = listOf(wood.suitability.edgeGrain.rating, wood.suitability.endGrain.rating)
  return (needle.isEmpty() ||
    wood.name.contains(needle, ignoreCase = true) ||
    wood.botanicalName.contains(needle, ignoreCase = true)) &&
    (WoodFilter.CUSTOM !in filters || wood.custom) &&
    (WoodFilter.RECOMMENDED_ONLY !in filters || ratings.all { it == Rating.RECOMMENDED }) &&
    (WoodFilter.HIDE_NOT_SUITABLE !in filters || ratings.none { it == Rating.NOT_SUITABLE })
}
