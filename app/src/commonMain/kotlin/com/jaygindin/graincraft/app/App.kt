package com.jaygindin.graincraft.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaygindin.graincraft.app.ui.LocalUnitFormatter
import com.jaygindin.graincraft.app.ui.library.LibraryScreen
import com.jaygindin.graincraft.app.ui.library.LibraryViewModel
import com.jaygindin.graincraft.app.ui.settings.SettingsScreen
import com.jaygindin.graincraft.app.ui.settings.SettingsViewModel
import com.jaygindin.graincraft.app.ui.shop.ShopScreen
import com.jaygindin.graincraft.app.ui.shop.ShopViewModel
import com.jaygindin.graincraft.app.ui.theme.GrainCraftTheme
import com.jaygindin.graincraft.app.ui.theme.ThemeChoice
import com.jaygindin.graincraft.app.ui.woods.WoodsScreen
import com.jaygindin.graincraft.app.ui.woods.WoodsViewModel
import com.jaygindin.graincraft.core.units.UnitFormatter

/** The whole app: theme and units from the settings, the rail, and the current screen. */
@Composable fun App(graph: AppGraph) = AppShell(graph, AppInfo.VERSION)

/** [App] with the version passed in, so screenshots do not change with every release. */
@Composable
internal fun AppShell(graph: AppGraph, version: String) {
  val settingsViewModel = viewModel { SettingsViewModel(graph.store) }
  val settings by settingsViewModel.settings.collectAsState()
  val dark =
    when (settings.theme) {
      ThemeChoice.SYSTEM -> isSystemInDarkTheme()
      ThemeChoice.LIGHT -> false
      ThemeChoice.DARK -> true
    }
  val formatter =
    remember(settings.units, settings.precision) {
      UnitFormatter(settings.units, settings.precision)
    }
  val formatterNow by rememberUpdatedState(formatter)
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

  GrainCraftTheme(dark) {
    CompositionLocalProvider(LocalUnitFormatter provides formatter) {
      AppScaffold(
        current = destination,
        onSelect = {
          destination = it
          openDesignId = null
        },
      ) {
        when {
          destination == Destination.DESIGNS && openDesignId != null ->
            EditorStub(onBack = { openDesignId = null })
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
