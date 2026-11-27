package com.jaygindin.graincraft.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.jaygindin.graincraft.app.data.AppSettings
import com.jaygindin.graincraft.app.ui.LocalUnitFormatter
import com.jaygindin.graincraft.app.ui.theme.Spacing
import com.jaygindin.graincraft.app.ui.theme.ThemeChoice
import com.jaygindin.graincraft.core.units.FractionPrecision
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.LengthUnit
import graincraft.app.generated.resources.Res
import graincraft.app.generated.resources.ic_folder_open
import org.jetbrains.compose.resources.painterResource

// Shows a difference between every units and precision choice: 1-9/16", 1-17/32" and 38.9 mm.
private val EXAMPLE_LENGTH = Length.inches(1, 17, 32)

/**
 * Units, fraction precision, theme, where the data lives, and About.
 *
 * @param dataFolder the store's folder, shown to the user.
 * @param version the app version for About.
 */
@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel,
  dataFolder: String,
  version: String,
  onOpenFolder: () -> Unit,
) {
  val settings by viewModel.settings.collectAsState()
  SettingsContent(settings, viewModel::update, dataFolder, version, onOpenFolder)
}

@OptIn(ExperimentalMaterial3Api::class) // TopAppBar is still experimental in Material 3
@Composable
internal fun SettingsContent(
  settings: AppSettings,
  onChange: (AppSettings) -> Unit,
  dataFolder: String,
  version: String,
  onOpenFolder: () -> Unit,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Settings") },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
          ),
      )
    },
    containerColor = MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxSize().testTag("screen-settings"),
  ) { padding ->
    Column(
      verticalArrangement = Arrangement.spacedBy(Spacing.lg),
      modifier =
        Modifier.padding(padding)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = Spacing.xl, vertical = Spacing.sm),
    ) {
      UnitsCard(settings, onChange)
      SettingsCard("Appearance") {
        ChoiceRow(
          label = "Theme",
          options =
            listOf(
              ThemeChoice.SYSTEM to "System",
              ThemeChoice.LIGHT to "Light",
              ThemeChoice.DARK to "Dark",
            ),
          selected = settings.theme,
          onSelect = { onChange(settings.copy(theme = it)) },
          tag = "settings-theme",
          optionTag = { "settings-theme-${it.name.lowercase()}" },
        )
      }
      DataCard(dataFolder, onOpenFolder)
      AboutCard(version)
      Spacer(Modifier.padding(bottom = Spacing.lg))
    }
  }
}

@Composable
private fun UnitsCard(settings: AppSettings, onChange: (AppSettings) -> Unit) {
  SettingsCard("Units") {
    ChoiceRow(
      label = "Units",
      options = listOf(LengthUnit.INCH to "Imperial", LengthUnit.MILLIMETRE to "Metric"),
      selected = settings.units,
      onSelect = { onChange(settings.copy(units = it)) },
      tag = "settings-units",
      optionTag = {
        if (it == LengthUnit.INCH) "settings-units-imperial" else "settings-units-metric"
      },
    )
    ChoiceRow(
      label = "Fraction precision",
      supporting = "Imperial only".takeIf { settings.units == LengthUnit.MILLIMETRE },
      options =
        listOf(
          FractionPrecision.SIXTEENTH to "1/16\"",
          FractionPrecision.THIRTY_SECOND to "1/32\"",
        ),
      selected = settings.precision,
      onSelect = { onChange(settings.copy(precision = it)) },
      tag = "settings-precision",
      optionTag = { "settings-precision-${it.denominator}" },
      enabled = settings.units == LengthUnit.INCH,
    )
    Text(
      "Example: ${LocalUnitFormatter.current.format(EXAMPLE_LENGTH)}",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun DataCard(dataFolder: String, onOpenFolder: () -> Unit) {
  SettingsCard("Data folder") {
    Text(
      "Designs, custom woods, the shop profile and these settings are saved in:",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        dataFolder,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.weight(1f),
      )
      TextButton(onClick = onOpenFolder, modifier = Modifier.testTag("settings-open-folder")) {
        Icon(painterResource(Res.drawable.ic_folder_open), contentDescription = null)
        Spacer(Modifier.width(Spacing.sm))
        Text("Open folder")
      }
    }
  }
}

@Composable
private fun AboutCard(version: String) {
  SettingsCard("About") {
    Text("GrainCraft Studio", style = MaterialTheme.typography.titleSmall)
    Text("Version $version", style = MaterialTheme.typography.bodyMedium)
    Text(
      "Licensed under the Apache License 2.0",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      "Noto Sans font licensed under the SIL Open Font License 1.1",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

/** A titled card; settings cards share one readable width rather than stretching. */
@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
  Card(
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(Spacing.md),
      modifier = Modifier.padding(Spacing.lg),
    ) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      content()
    }
  }
}

/** A label on the left and a segmented choice on the right. */
@Composable
private fun <T> ChoiceRow(
  label: String,
  options: List<Pair<T, String>>,
  selected: T,
  onSelect: (T) -> Unit,
  tag: String,
  optionTag: (T) -> String,
  supporting: String? = null,
  enabled: Boolean = true,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Column(modifier = Modifier.weight(1f)) {
      Text(label, style = MaterialTheme.typography.bodyLarge)
      if (supporting != null) {
        Text(
          supporting,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    SingleChoiceSegmentedButtonRow(modifier = Modifier.testTag(tag)) {
      options.forEachIndexed { index, (value, text) ->
        SegmentedButton(
          selected = value == selected,
          onClick = { onSelect(value) },
          shape = SegmentedButtonDefaults.itemShape(index, options.size),
          enabled = enabled,
          label = { Text(text) },
          modifier = Modifier.testTag(optionTag(value)),
        )
      }
    }
  }
}
