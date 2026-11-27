package com.jaygindin.graincraft.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jaygindin.graincraft.app.ui.LocalUnitFormatter
import com.jaygindin.graincraft.app.ui.settings.SettingsScreen
import com.jaygindin.graincraft.app.ui.settings.SettingsViewModel
import com.jaygindin.graincraft.app.ui.theme.GrainCraftTheme
import com.jaygindin.graincraft.app.ui.theme.ThemeChoice
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
  var destination by rememberSaveable { mutableStateOf(Destination.DESIGNS) }

  GrainCraftTheme(dark) {
    CompositionLocalProvider(LocalUnitFormatter provides formatter) {
      AppScaffold(current = destination, onSelect = { destination = it }) {
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
    }
  }
}
