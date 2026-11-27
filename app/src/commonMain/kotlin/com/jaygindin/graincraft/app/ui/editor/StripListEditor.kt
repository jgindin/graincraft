package com.jaygindin.graincraft.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.jaygindin.graincraft.app.ui.components.LengthField
import com.jaygindin.graincraft.app.ui.theme.Spacing
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.design.PatternSpec
import com.jaygindin.graincraft.core.design.Strip
import com.jaygindin.graincraft.core.design.StripePresets
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.wood.Woods
import graincraft.app.generated.resources.Res
import graincraft.app.generated.resources.ic_add
import graincraft.app.generated.resources.ic_arrow_downward
import graincraft.app.generated.resources.ic_arrow_upward
import graincraft.app.generated.resources.ic_delete
import graincraft.app.generated.resources.ic_expand_more
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val WIDTH_FIELD = 112.dp
private val ROW_ICON = 20.dp
private const val FALLBACK_SPECIES = "black-walnut"
private const val FALLBACK_ACCENT = "hard-maple"
private val FALLBACK_WIDTH = Length.inches(1, 1, 2)
private val PINSTRIPE_WIDTH = Length.inches(0, 1, 4)
private const val FIBONACCI_UNITS = 19 // 1 + 1 + 2 + 3 + 5 + 3 + 2 + 1 + 1
private const val MAX_ALTERNATING_SPECIES = 3

/**
 * The strips across the board, each with a species, a width, and buttons to move or remove it; then
 * "Add strip", the presets, and for end grain the row height. Strip tags count from 0.
 */
@Composable
internal fun StripListEditor(
  pattern: PatternSpec.Stripes,
  grainMode: GrainMode,
  boardWidth: Length,
  woods: Woods,
  update: PartUpdate<PatternSpec.Stripes>,
) {
  val strips = pattern.strips
  fun change(key: String? = null, f: (List<Strip>) -> List<Strip>) =
    update.update(key) { it.copy(strips = f(it.strips)) }
  Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
    strips.forEachIndexed { i, strip ->
      StripRow(
        index = i,
        strip = strip,
        count = strips.size,
        grainMode = grainMode,
        woods = woods,
        onSpecies = { id -> change { it.replaced(i) { s -> s.copy(speciesId = id) } } },
        onWidth = { w -> change("strip-$i-width") { it.replaced(i) { s -> s.copy(width = w) } } },
        onMove = { by -> change { it.moved(i, by) } },
        onRemove = { change { it.filterIndexed { j, _ -> j != i } } },
      )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
      OutlinedButton(
        onClick = { change { it + nextStrip(it) } },
        modifier = Modifier.testTag("strip-add"),
      ) {
        Icon(
          painterResource(Res.drawable.ic_add),
          contentDescription = null,
          modifier = Modifier.size(ROW_ICON),
        )
        Text("Add strip", Modifier.padding(start = Spacing.sm))
      }
      PresetsMenu(boardWidth) { preset -> change { preset(it) } }
    }
    if (grainMode == GrainMode.END_GRAIN) {
      LengthField(
        label = "Row height",
        value = pattern.rowHeight,
        onValueChange = { h -> update.update("row-height") { it.copy(rowHeight = h) } },
        testTag = "row-height",
        supportingText = "How far each row of end grain runs along the board",
        allowZero = false,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun StripRow(
  index: Int,
  strip: Strip,
  count: Int,
  grainMode: GrainMode,
  woods: Woods,
  onSpecies: (String) -> Unit,
  onWidth: (Length) -> Unit,
  onMove: (Int) -> Unit,
  onRemove: () -> Unit,
) {
  Column {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        "Strip ${index + 1}",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.weight(1f))
      RowButton(Res.drawable.ic_arrow_upward, "Move up", "strip-$index-up", index > 0) {
        onMove(-1)
      }
      RowButton(
        Res.drawable.ic_arrow_downward,
        "Move down",
        "strip-$index-down",
        index < count - 1,
      ) {
        onMove(1)
      }
      RowButton(Res.drawable.ic_delete, "Remove", "strip-$index-remove", count > 1, onRemove)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
      SpeciesDropdown(
        "Wood",
        woods,
        strip.speciesId,
        grainMode,
        "strip-$index-species",
        Modifier.weight(1f),
        onSpecies,
      )
      LengthField(
        label = "Width",
        value = strip.width,
        onValueChange = onWidth,
        testTag = "strip-$index-width",
        allowZero = false,
        modifier = Modifier.width(WIDTH_FIELD),
      )
    }
  }
}

@Composable
private fun RowButton(
  icon: DrawableResource,
  description: String,
  tag: String,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.testTag(tag)) {
    Icon(painterResource(icon), contentDescription = description, Modifier.size(ROW_ICON))
  }
}

/** A ready-made strip list, built from the strips there are now. */
private class Preset(val name: String, val tag: String, val build: (List<Strip>) -> List<Strip>)

@Composable
private fun PresetsMenu(boardWidth: Length, onPick: ((List<Strip>) -> List<Strip>) -> Unit) {
  var open by remember { mutableStateOf(false) }
  val presets = remember(boardWidth) { presets(boardWidth) }
  Box {
    TextButton(onClick = { open = true }, modifier = Modifier.testTag("stripe-presets")) {
      Text("Presets")
      Icon(
        painterResource(Res.drawable.ic_expand_more),
        contentDescription = null,
        modifier = Modifier.size(ROW_ICON),
      )
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
      presets.forEach { preset ->
        DropdownMenuItem(
          text = { Text(preset.name) },
          onClick = {
            onPick(preset.build)
            open = false
          },
          modifier = Modifier.testTag("stripe-preset-${preset.tag}"),
        )
      }
    }
  }
}

/** Equal widths share the board's width; the other presets keep the woods already chosen. */
private fun presets(boardWidth: Length): List<Preset> =
  listOf(
    Preset("Uniform widths", "uniform") { strips ->
      StripePresets.uniform(strips.map { it.speciesId }, boardWidth / strips.size.coerceAtLeast(1))
    },
    Preset("Alternating", "alternating") { strips ->
      val count = strips.size.coerceAtLeast(2)
      StripePresets.alternating(alternatingSpecies(strips), count, boardWidth / count)
    },
    Preset("Mirror", "mirror") { strips -> StripePresets.mirror(strips) },
    Preset("Fibonacci widths", "fibonacci") { strips ->
      val (a, b) = alternatingSpecies(strips)
      StripePresets.fibonacci(a, b, boardWidth / FIBONACCI_UNITS)
    },
    Preset("Pinstripes", "pinstripe") { strips ->
      // Always a second wood, so the strips left between the accents are never empty.
      val accent = alternatingSpecies(strips)[1]
      StripePresets.pinstripe(strips.filter { it.speciesId != accent }, accent, PINSTRIPE_WIDTH)
    },
  )

/** Two or three different woods from the strips, padded with a contrasting default. */
private fun alternatingSpecies(strips: List<Strip>): List<String> {
  val distinct = strips.map { it.speciesId }.distinct().take(MAX_ALTERNATING_SPECIES)
  return when {
    distinct.size >= 2 -> distinct
    distinct.firstOrNull() == FALLBACK_ACCENT -> listOf(FALLBACK_ACCENT, FALLBACK_SPECIES)
    else -> listOf(distinct.firstOrNull() ?: FALLBACK_SPECIES, FALLBACK_ACCENT)
  }
}

/** A new strip continues the alternation: a copy of the one before the last. */
private fun nextStrip(strips: List<Strip>): Strip =
  strips.getOrNull(strips.size - 2)
    ?: strips.lastOrNull()
    ?: Strip(FALLBACK_SPECIES, FALLBACK_WIDTH)

private fun List<Strip>.replaced(i: Int, f: (Strip) -> Strip): List<Strip> = mapIndexed { j, s ->
  if (j == i) f(s) else s
}

private fun List<Strip>.moved(i: Int, by: Int): List<Strip> {
  val j = i + by
  if (i !in indices || j !in indices) return this
  return toMutableList().also {
    it[i] = this[j]
    it[j] = this[i]
  }
}
