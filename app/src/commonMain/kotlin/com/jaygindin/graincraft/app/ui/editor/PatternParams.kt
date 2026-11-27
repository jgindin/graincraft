package com.jaygindin.graincraft.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.jaygindin.graincraft.app.ui.components.LengthField
import com.jaygindin.graincraft.app.ui.preview.ColorSwatch
import com.jaygindin.graincraft.app.ui.theme.LocalRatingColors
import com.jaygindin.graincraft.app.ui.theme.Spacing
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.design.PatternSpec
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.wood.Rating
import com.jaygindin.graincraft.core.wood.WoodSpecies
import com.jaygindin.graincraft.core.wood.Woods

private val SWATCH_SIZE = 20.dp

/** The fields of whichever pattern the design has. */
@Composable
internal fun PatternParams(
  pattern: PatternSpec,
  grainMode: GrainMode,
  boardWidth: Length,
  woods: Woods,
  update: PartUpdate<PatternSpec>,
) {
  when (pattern) {
    is PatternSpec.Stripes ->
      StripListEditor(
        pattern,
        grainMode,
        boardWidth,
        woods,
        update.part<PatternSpec, PatternSpec.Stripes> { p, f ->
          if (p is PatternSpec.Stripes) f(p) else p
        },
      )
    is PatternSpec.Checkerboard ->
      CheckerboardParams(
        pattern,
        grainMode,
        woods,
        update.part<PatternSpec, PatternSpec.Checkerboard> { p, f ->
          if (p is PatternSpec.Checkerboard) f(p) else p
        },
      )
    is PatternSpec.Brick ->
      BrickParams(
        pattern,
        grainMode,
        woods,
        update.part<PatternSpec, PatternSpec.Brick> { p, f ->
          if (p is PatternSpec.Brick) f(p) else p
        },
      )
  }
}

@Composable
private fun CheckerboardParams(
  pattern: PatternSpec.Checkerboard,
  grainMode: GrainMode,
  woods: Woods,
  update: PartUpdate<PatternSpec.Checkerboard>,
) {
  Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
    SpeciesDropdown("Wood A", woods, pattern.speciesA, grainMode, "checker-a") { id ->
      update { it.copy(speciesA = id) }
    }
    SpeciesDropdown("Wood B", woods, pattern.speciesB, grainMode, "checker-b") { id ->
      update { it.copy(speciesB = id) }
    }
    LengthField(
      label = "Cell size",
      value = pattern.targetCell,
      onValueChange = { l -> update.update("checker-cell") { it.copy(targetCell = l) } },
      testTag = "checker-cell",
      supportingText = "Squares are adjusted slightly to fit the board",
      allowZero = false,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun BrickParams(
  pattern: PatternSpec.Brick,
  grainMode: GrainMode,
  woods: Woods,
  update: PartUpdate<PatternSpec.Brick>,
) {
  Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
    SpeciesDropdown("Bricks", woods, pattern.brickSpecies, grainMode, "brick-species") { id ->
      update { it.copy(brickSpecies = id) }
    }
    SpeciesDropdown("Mortar", woods, pattern.mortarSpecies, grainMode, "mortar-species") { id ->
      update { it.copy(mortarSpecies = id) }
    }
    LengthField(
      label = "Brick width",
      value = pattern.targetBrickWidth,
      onValueChange = { l -> update.update("brick-width") { it.copy(targetBrickWidth = l) } },
      testTag = "brick-width",
      allowZero = false,
      modifier = Modifier.fillMaxWidth(),
    )
    LengthField(
      label = "Brick height",
      value = pattern.targetBrickHeight,
      onValueChange = { l -> update.update("brick-height") { it.copy(targetBrickHeight = l) } },
      testTag = "brick-height",
      allowZero = false,
      modifier = Modifier.fillMaxWidth(),
    )
    LengthField(
      label = "Mortar width",
      value = pattern.mortarWidth,
      onValueChange = { l -> update.update("mortar-width") { it.copy(mortarWidth = l) } },
      testTag = "mortar-width",
      allowZero = false,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

/**
 * A read-only field that opens a menu of every wood, built-in and custom, each with its swatch.
 * Woods not suited to [grainMode] can still be picked, but are marked in the rating colour. Option
 * tags are `<tag>-<species id>`.
 */
@OptIn(ExperimentalMaterial3Api::class) // ExposedDropdownMenuBox is still experimental
@Composable
internal fun SpeciesDropdown(
  label: String,
  woods: Woods,
  selectedId: String,
  grainMode: GrainMode,
  tag: String,
  modifier: Modifier = Modifier,
  onSelect: (String) -> Unit,
) {
  var open by remember { mutableStateOf(false) }
  val selected = woods.byId(selectedId)
  val ratings = LocalRatingColors.current
  ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }, modifier = modifier) {
    OutlinedTextField(
      value = selected?.name ?: selectedId,
      onValueChange = {},
      readOnly = true,
      singleLine = true,
      label = { Text(label) },
      leadingIcon = { ColorSwatch(selected?.raw?.base, size = SWATCH_SIZE) },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
      modifier =
        Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
          .fillMaxWidth()
          .testTag(tag),
    )
    ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
      speciesMenuOrder(woods).forEach { wood ->
        val rating =
          if (grainMode == GrainMode.END_GRAIN) wood.suitability.endGrain.rating
          else wood.suitability.edgeGrain.rating
        DropdownMenuItem(
          text = { Text(wood.name) },
          leadingIcon = { ColorSwatch(wood.raw.base, size = SWATCH_SIZE) },
          trailingIcon =
            if (rating == Rating.NOT_SUITABLE) {
              {
                Text(
                  "Not suitable",
                  style = MaterialTheme.typography.labelMedium,
                  color = ratings.notSuitable,
                )
              }
            } else {
              null
            },
          onClick = {
            onSelect(wood.id)
            open = false
          },
          modifier = Modifier.testTag("$tag-${wood.id}"),
        )
      }
    }
  }
}

/**
 * The species in the order the wood menus list them: alphabetical by name, ignoring case, with
 * custom woods mixed in, so a wood is found where its name says it is.
 */
internal fun speciesMenuOrder(woods: Woods): List<WoodSpecies> =
  woods.all.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
