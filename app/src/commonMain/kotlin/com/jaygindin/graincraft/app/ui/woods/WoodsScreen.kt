package com.jaygindin.graincraft.app.ui.woods

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.jaygindin.graincraft.app.ui.preview.ColorSwatch
import com.jaygindin.graincraft.app.ui.theme.Spacing
import com.jaygindin.graincraft.core.wood.GrainRating
import com.jaygindin.graincraft.core.wood.WoodSpecies
import graincraft.app.generated.resources.Res
import graincraft.app.generated.resources.ic_add
import graincraft.app.generated.resources.ic_expand_less
import graincraft.app.generated.resources.ic_expand_more
import graincraft.app.generated.resources.ic_palette
import graincraft.app.generated.resources.ic_search
import graincraft.app.generated.resources.ic_warning
import graincraft.app.generated.resources.ic_water_drop
import org.jetbrains.compose.resources.painterResource

private val FILTER_LABELS =
  mapOf(
    WoodFilter.RECOMMENDED_ONLY to "Recommended only",
    WoodFilter.HIDE_NOT_SUITABLE to "Hide not suitable",
    WoodFilter.CUSTOM to "Custom",
  )

/** The species list: search, filters, expandable rows, and the custom wood dialog. */
@Composable
fun WoodsScreen(viewModel: WoodsViewModel) {
  val woods by viewModel.visible.collectAsState()
  val query by viewModel.query.collectAsState()
  val filters by viewModel.filters.collectAsState()
  // `null` is closed; a dialog for a new wood has no species to edit.
  var dialog by remember { mutableStateOf<CustomWoodDialogTarget?>(null) }
  var deleting by remember { mutableStateOf<WoodSpecies?>(null) }

  WoodsContent(
    woods = woods,
    query = query,
    filters = filters,
    onQuery = viewModel::setQuery,
    onFilter = viewModel::setFilter,
    onAdd = { dialog = CustomWoodDialogTarget(null) },
    onEdit = { dialog = CustomWoodDialogTarget(it) },
    onDelete = { deleting = it },
  )
  dialog?.let { target ->
    CustomWoodDialog(
      initial = target.wood,
      onSave = {
        viewModel.saveCustom(it)
        dialog = null
      },
      onDismiss = { dialog = null },
    )
  }
  deleting?.let { wood ->
    AlertDialog(
      onDismissRequest = { deleting = null },
      title = { Text("Delete ${wood.name}?") },
      text = { Text("Designs that use it keep their own copy, so they are not changed.") },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.deleteCustom(wood.id)
            deleting = null
          },
          modifier = Modifier.testTag("wood-delete-confirm"),
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(
          onClick = { deleting = null },
          modifier = Modifier.testTag("wood-delete-cancel"),
        ) {
          Text("Cancel")
        }
      },
    )
  }
}

/** Which dialog to open: [wood] is the custom wood being edited, or null to add one. */
private class CustomWoodDialogTarget(val wood: WoodSpecies?)

@OptIn(ExperimentalMaterial3Api::class) // TopAppBar is still experimental in Material 3
@Composable
private fun WoodsContent(
  woods: List<WoodSpecies>,
  query: String,
  filters: Set<WoodFilter>,
  onQuery: (String) -> Unit,
  onFilter: (WoodFilter, Boolean) -> Unit,
  onAdd: () -> Unit,
  onEdit: (WoodSpecies) -> Unit,
  onDelete: (WoodSpecies) -> Unit,
) {
  var expanded by rememberSaveable { mutableStateOf(emptySet<String>()) }
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Woods") },
        actions = {
          Button(onClick = onAdd, modifier = Modifier.testTag("woods-add-custom")) {
            Icon(painterResource(Res.drawable.ic_add), contentDescription = null)
            Text("Add custom wood", modifier = Modifier.padding(start = Spacing.sm))
          }
          Spacer(Modifier.width(Spacing.lg))
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
          ),
      )
    },
    containerColor = MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxSize().testTag("screen-woods"),
  ) { padding ->
    Column(
      verticalArrangement = Arrangement.spacedBy(Spacing.md),
      modifier = Modifier.padding(padding).padding(horizontal = Spacing.xl),
    ) {
      SearchAndFilters(query, filters, onQuery, onFilter)
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier.fillMaxSize().testTag("woods-list"),
      ) {
        items(woods, key = { it.id }) { wood ->
          val open = wood.id in expanded
          WoodRow(
            wood = wood,
            expanded = open,
            onToggle = { expanded = if (open) expanded - wood.id else expanded + wood.id },
            onEdit = { onEdit(wood) },
            onDelete = { onDelete(wood) },
          )
        }
      }
    }
  }
}

@Composable
private fun WoodRow(
  wood: WoodSpecies,
  expanded: Boolean,
  onToggle: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  Card(
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    modifier = Modifier.fillMaxWidth(),
  ) {
    ListItem(
      leadingContent = {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
          ColorSwatch(wood.raw.base, Modifier.testTag("wood-raw-${wood.id}"))
          ColorSwatch(wood.oiled.base, Modifier.testTag("wood-oiled-${wood.id}"))
        }
      },
      headlineContent = { Text(wood.name) },
      supportingContent = { Text("Janka ${wood.jankaLbf} lbf · ${wood.poreType.label()}") },
      trailingContent = { RowTrailing(wood, expanded) },
      colors = ListItemDefaults.colors(containerColor = Color.Transparent),
      modifier = Modifier.clickable(onClick = onToggle).testTag("wood-row-${wood.id}"),
    )
    if (expanded) WoodDetails(wood, onEdit, onDelete)
  }
}

@Composable
private fun RowTrailing(wood: WoodSpecies, expanded: Boolean) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (wood.oily) {
      FlagIcon(
        Res.drawable.ic_water_drop,
        "Oily: wipe with acetone before gluing",
        "wood-oily-${wood.id}",
      )
    }
    if (wood.bleedsColor) {
      FlagIcon(
        Res.drawable.ic_palette,
        "Colour can bleed into neighbouring wood",
        "wood-bleeds-${wood.id}",
      )
    }
    if (wood.irritant) {
      FlagIcon(Res.drawable.ic_warning, "Dust irritates skin or lungs", "wood-irritant-${wood.id}")
    }
    RatingChip("Edge", wood.suitability.edgeGrain.rating, Modifier.testTag("wood-edge-${wood.id}"))
    RatingChip("End", wood.suitability.endGrain.rating, Modifier.testTag("wood-end-${wood.id}"))
    Icon(
      painterResource(if (expanded) Res.drawable.ic_expand_less else Res.drawable.ic_expand_more),
      contentDescription = if (expanded) "Collapse" else "Expand",
    )
  }
}

@Composable
private fun WoodDetails(wood: WoodSpecies, onEdit: () -> Unit, onDelete: () -> Unit) {
  Column(
    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    modifier =
      Modifier.padding(horizontal = Spacing.lg)
        .padding(bottom = Spacing.lg)
        .testTag("wood-reasons-${wood.id}"),
  ) {
    if (wood.botanicalName.isNotBlank()) {
      Text(
        wood.botanicalName,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    GrainReasons("Edge grain", wood.suitability.edgeGrain)
    GrainReasons("End grain", wood.suitability.endGrain)
    wood.notes.forEach { Text("Note: $it", style = MaterialTheme.typography.bodyMedium) }
    if (wood.custom) {
      Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        OutlinedButton(onClick = onEdit, modifier = Modifier.testTag("wood-edit-${wood.id}")) {
          Text("Edit")
        }
        TextButton(onClick = onDelete, modifier = Modifier.testTag("wood-delete-${wood.id}")) {
          Text("Delete")
        }
      }
    }
  }
}

@Composable
private fun GrainReasons(title: String, rating: GrainRating) {
  Column {
    Text("$title: ${rating.rating.label()}", style = MaterialTheme.typography.titleSmall)
    rating.reasons.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
  }
}

@Composable
private fun SearchAndFilters(
  query: String,
  filters: Set<WoodFilter>,
  onQuery: (String) -> Unit,
  onFilter: (WoodFilter, Boolean) -> Unit,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    OutlinedTextField(
      value = query,
      onValueChange = onQuery,
      placeholder = { Text("Search woods") },
      leadingIcon = { Icon(painterResource(Res.drawable.ic_search), contentDescription = null) },
      singleLine = true,
      modifier = Modifier.width(320.dp).testTag("woods-search"),
    )
    WoodFilter.entries.forEach { filter ->
      FilterChip(
        selected = filter in filters,
        onClick = { onFilter(filter, filter !in filters) },
        label = { Text(FILTER_LABELS.getValue(filter)) },
        modifier = Modifier.testTag("woods-filter-${filter.name.lowercase()}"),
      )
    }
  }
}
