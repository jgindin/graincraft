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
import com.jaygindin.graincraft.app.ui.theme.GrainCraftTheme
import com.jaygindin.graincraft.app.ui.theme.ThemeChoice
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.UnitFormatter
import com.jaygindin.graincraft.core.wood.Woods

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
  val libraryViewModel = viewModel {
    LibraryViewModel(graph, { Woods(emptyList()) }, { ShopProfile() }, { formatterNow })
  }
  // A design opened from the library, shown instead of the rail's screen.
  var openDesignId by rememberSaveable { mutableStateOf<String?>(null) }
  LaunchedEffect(formatter) { libraryViewModel.refresh() }
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
          else -> RailScreen(destination, settingsViewModel, graph, version)
        }
      }
    }
  }
}

@Composable
private fun RailScreen(
  destination: Destination,
  settingsViewModel: SettingsViewModel,
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
    else -> PlaceholderScreen(destination)
  }
}
