package com.jaygindin.graincraft.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.focus.FocusRequester
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
  RegisterSettings(controller, settingsViewModel)
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
  // Thumbnails are compiled from these, so they are rebuilt when any of them changes.
  LaunchedEffect(formatter, woods, profile) { libraryViewModel.refresh() }
  LaunchedEffect(Unit) { graph.store.purgeTrash(graph.clock()) }
  val nav = rememberShellNavigation(controller, onLeftEditor = libraryViewModel::refresh)
  val woodSearch = rememberWoodSearchFocus(nav)

  GrainCraftTheme(dark) {
    CompositionLocalProvider(*appLocals(formatter, graph)) {
      AppScaffold(
        current = nav.destination,
        onSelect = nav::select,
      ) {
        when (nav.destination) {
          Destination.DESIGNS ->
            DesignsDestination(
              nav,
              libraryViewModel,
              controller,
              graph,
              editorFactory(graph, woodsViewModel, shopViewModel, formatterFlow),
            )
          else ->
            RailScreen(
              nav.destination,
              settingsViewModel,
              woodsViewModel,
              shopViewModel,
              graph,
              version,
              woodSearch,
            )
        }
      }
    }
  }
}

/** The open design's editor, or the library when none is open. */
@Composable
private fun DesignsDestination(
  nav: ShellNavigation,
  library: LibraryViewModel,
  controller: AppController,
  graph: AppGraph,
  newEditor: (BoardDesign) -> EditorViewModel,
) {
  val openId = nav.openDesignId
  if (openId != null) {
    EditorRoute(openId, library::load, controller, graph, onBack = nav::back, newEditor)
  } else {
    LibraryScreen(
      library,
      onOpen = { nav.openDesignId = it },
      creating = nav.creatingDesign,
      onCreatingChange = { nav.creatingDesign = it },
    )
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
  woodSearch: FocusRequester,
) {
  when (destination) {
    Destination.SETTINGS ->
      SettingsScreen(
        viewModel = settingsViewModel,
        dataFolder = graph.store.rootDescription,
        version = version,
        onOpenFolder = { graph.browser.openFolder(graph.store.rootDescription) },
      )
    Destination.WOODS -> WoodsScreen(woodsViewModel, woodSearch)
    Destination.SHOP -> ShopScreen(shopViewModel)
    else -> PlaceholderScreen(destination)
  }
}
