package com.jaygindin.graincraft.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaygindin.graincraft.app.ui.LocalUnitFormatter
import com.jaygindin.graincraft.app.ui.editor.EditorViewModel
import com.jaygindin.graincraft.app.ui.library.LibraryScreen
import com.jaygindin.graincraft.app.ui.library.LibraryViewModel
import com.jaygindin.graincraft.app.ui.preview.LocalArgbImageFactory
import com.jaygindin.graincraft.app.ui.settings.SettingsScreen
import com.jaygindin.graincraft.app.ui.settings.SettingsViewModel
import com.jaygindin.graincraft.app.ui.shop.ShopScreen
import com.jaygindin.graincraft.app.ui.shop.ShopViewModel
import com.jaygindin.graincraft.app.ui.theme.GrainCraftTheme
import com.jaygindin.graincraft.app.ui.theme.ThemeChoice
import com.jaygindin.graincraft.app.ui.woods.WoodsScreen
import com.jaygindin.graincraft.app.ui.woods.WoodsViewModel
import com.jaygindin.graincraft.core.design.BoardDesign
import com.jaygindin.graincraft.core.units.UnitFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** The whole app: theme and units from the settings, the rail, and the current screen. */
@Composable
fun App(graph: AppGraph, controller: AppController = remember { AppController() }) =
  AppShell(graph, AppInfo.VERSION, controller)

/** [App] with the version passed in, so screenshots do not change with every release. */
@Composable
internal fun AppShell(
  graph: AppGraph,
  version: String,
  controller: AppController = remember { AppController() },
) {
  val settingsViewModel = viewModel { SettingsViewModel(graph.store) }
  val settings by settingsViewModel.settings.collectAsState()
  val dark = settings.theme.isDark()
  val formatter =
    remember(settings.units, settings.precision) {
      UnitFormatter(settings.units, settings.precision)
    }
  val formatterNow by rememberUpdatedState(formatter)
  val formatterFlow = rememberFlowOf(formatter)
  // One of each for the whole app, so the library and the editor see the same woods and profile.
  val woodsViewModel = viewModel { WoodsViewModel(graph.store) }
  val shopViewModel = viewModel { ShopViewModel(graph.store) }
  val woods by woodsViewModel.woods.collectAsState()
  val profile by shopViewModel.profile.collectAsState()
  val libraryViewModel = viewModel {
    LibraryViewModel(
      graph,
      { woodsViewModel.woods.value },
      { shopViewModel.profile.value },
      { formatterNow },
    )
  }
  // A design opened from the library, shown instead of the rail's screen.
  var openDesignId by rememberSaveable { mutableStateOf<String?>(null) }
  // Thumbnails are compiled from these, so they are rebuilt when any of them changes.
  LaunchedEffect(formatter, woods, profile) { libraryViewModel.refresh() }
  LaunchedEffect(Unit) { graph.store.purgeTrash(graph.clock()) }
  var destination by rememberSaveable { mutableStateOf(Destination.DESIGNS) }
  val scope = rememberCoroutineScope()
  var leaving by remember { mutableStateOf(false) }
  // Leaving the editor writes its last change first, wherever the user goes.
  fun leaveEditor(then: () -> Unit) {
    if (openDesignId == null) return then()
    // A second click while the first leave is saving would otherwise save and leave twice.
    if (leaving) return
    leaving = true
    scope.launch {
      try {
        controller.flushOpenEditor()
      } finally {
        // A failed flush must not block every later leave.
        leaving = false
      }
      openDesignId = null
      // The cards show the design as it now is.
      libraryViewModel.refresh()
      then()
    }
  }

  GrainCraftTheme(dark) {
    CompositionLocalProvider(*appLocals(formatter, graph)) {
      AppScaffold(
        current = destination,
        onSelect = { leaveEditor { destination = it } },
      ) {
        when {
          destination == Destination.DESIGNS && openDesignId != null ->
            EditorRoute(
              checkNotNull(openDesignId),
              libraryViewModel::load,
              controller,
              onBack = { leaveEditor {} },
              newEditor = editorFactory(graph, woodsViewModel, shopViewModel, formatterFlow),
            )
          destination == Destination.DESIGNS ->
            LibraryScreen(libraryViewModel, onOpen = { openDesignId = it })
          else ->
            RailScreen(
              destination,
              settingsViewModel,
              woodsViewModel,
              shopViewModel,
              graph,
              version,
            )
        }
      }
    }
  }
}

/** Opens a design in an editor that follows the app's woods, shop profile and units. */
private fun editorFactory(
  graph: AppGraph,
  woods: WoodsViewModel,
  shop: ShopViewModel,
  formatter: StateFlow<UnitFormatter>,
): (BoardDesign) -> EditorViewModel = { design ->
  EditorViewModel(design, graph.store, woods.woods, shop.profile, formatter, graph.clock)
}

@Composable
private fun ThemeChoice.isDark(): Boolean =
  when (this) {
    ThemeChoice.SYSTEM -> isSystemInDarkTheme()
    ThemeChoice.LIGHT -> false
    ThemeChoice.DARK -> true
  }

/** [value] as a flow that follows it, for a view model that reacts to it changing. */
@Composable
private fun <T> rememberFlowOf(value: T): StateFlow<T> {
  val flow = remember { MutableStateFlow(value) }
  SideEffect { flow.value = value }
  return flow
}

/** What every screen reads from the composition: the units and the platform's bitmap factory. */
private fun appLocals(formatter: UnitFormatter, graph: AppGraph) =
  arrayOf(LocalUnitFormatter provides formatter, LocalArgbImageFactory provides graph.images)

@Composable
private fun RailScreen(
  destination: Destination,
  settingsViewModel: SettingsViewModel,
  woodsViewModel: WoodsViewModel,
  shopViewModel: ShopViewModel,
  graph: AppGraph,
  version: String,
) {
  when (destination) {
    Destination.SETTINGS ->
      SettingsScreen(
        viewModel = settingsViewModel,
        dataFolder = graph.store.rootDescription,
        version = version,
        onOpenFolder = { graph.browser.openFolder(graph.store.rootDescription) },
      )
    Destination.WOODS -> WoodsScreen(woodsViewModel)
    Destination.SHOP -> ShopScreen(shopViewModel)
    else -> PlaceholderScreen(destination)
  }
}
