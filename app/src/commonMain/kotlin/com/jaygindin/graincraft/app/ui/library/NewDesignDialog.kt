package com.jaygindin.graincraft.app.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.jaygindin.graincraft.app.ui.LocalUnitFormatter
import com.jaygindin.graincraft.app.ui.theme.Spacing
import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.LengthParse
import com.jaygindin.graincraft.core.units.UnitFormatter
import com.jaygindin.graincraft.core.units.parseLength

private class SizePreset(val tag: String, val size: Dimensions?)

private fun size(l: Length, w: Length, t: Length) = Dimensions(l, w, t)

private val PRESETS =
  listOf(
    SizePreset(
      "18x12",
      size(Length.inches(18), Length.inches(12), Length.inches(1, 1, 2)),
    ),
    SizePreset(
      "16x10",
      size(Length.inches(16), Length.inches(10), Length.inches(1, 1, 4)),
    ),
    SizePreset(
      "24x16",
      size(Length.inches(24), Length.inches(16), Length.inches(1, 3, 4)),
    ),
    SizePreset("custom", null),
  )

private val LENGTH_RANGE = Length.inches(6)..Length.inches(36)
private val WIDTH_RANGE = Length.inches(4)..Length.inches(24)
private val THICKNESS_RANGE = Length.inches(0, 3, 4)..Length.inches(3)

private fun presetLabel(p: SizePreset, f: UnitFormatter): String =
  p.size?.let { "${f.format(it.length)} × ${f.format(it.width)} × ${f.format(it.thickness)}" }
    ?: "Custom"

/** Parses a custom size field: the length, or the reason it is not acceptable. */
private fun check(
  text: String,
  range: ClosedRange<Length>,
  f: UnitFormatter,
): Pair<Length?, String?> =
  when (val r = parseLength(text, f.unit)) {
    is LengthParse.Error -> null to r.reason
    is LengthParse.Ok ->
      if (r.length in range) r.length to null
      else null to "Use ${f.format(range.start)} to ${f.format(range.endInclusive)}"
  }

/** Asks for the name, grain, pattern and size of a new design. */
@Composable
internal fun NewDesignDialog(
  onCreate: (String, GrainMode, PatternKind, Dimensions) -> Unit,
  onDismiss: () -> Unit,
) {
  val formatter = LocalUnitFormatter.current
  var name by remember { mutableStateOf("") }
  var mode by remember { mutableStateOf(GrainMode.END_GRAIN) }
  var kind by remember { mutableStateOf(PatternKind.CHECKERBOARD) }
  var preset by remember { mutableStateOf(PRESETS.first()) }
  var length by remember { mutableStateOf("") }
  var width by remember { mutableStateOf("") }
  var thickness by remember { mutableStateOf("") }

  val l = check(length, LENGTH_RANGE, formatter)
  val w = check(width, WIDTH_RANGE, formatter)
  val t = check(thickness, THICKNESS_RANGE, formatter)
  val chosen = preset.size ?: customSize(l.first, w.first, t.first)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("New design") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Name") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("new-name"),
        )
        Label("Grain")
        GrainPicker(mode) {
          mode = it
          // Only stripes can be built from edge grain.
          if (it == GrainMode.EDGE_GRAIN) kind = PatternKind.STRIPES
        }
        Label("Pattern")
        PatternPicker(kind, mode) { kind = it }
        Label("Size")
        SizePicker(preset, formatter) { preset = it }
        if (preset.size == null) {
          SizeField("Length", length, { length = it }, l.second, "new-custom-length")
          SizeField("Width", width, { width = it }, w.second, "new-custom-width")
          SizeField("Thickness", thickness, { thickness = it }, t.second, "new-custom-thickness")
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { chosen?.let { onCreate(name, mode, kind, it) } },
        enabled = name.isNotBlank() && chosen != null,
        modifier = Modifier.testTag("new-create"),
      ) {
        Text("Create")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.testTag("new-cancel")) { Text("Cancel") }
    },
  )
}

@Composable
private fun Label(text: String) {
  Text(
    text,
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

@Composable
private fun SizeField(
  label: String,
  value: String,
  onChange: (String) -> Unit,
  error: String?,
  tag: String,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onChange,
    label = { Text(label) },
    singleLine = true,
    isError = value.isNotEmpty() && error != null,
    supportingText = { if (value.isNotEmpty() && error != null) Text(error) },
    modifier = Modifier.fillMaxWidth().testTag(tag),
  )
}

@OptIn(ExperimentalMaterial3Api::class) // SegmentedButton is still experimental in Material 3
@Composable
private fun GrainPicker(mode: GrainMode, onPick: (GrainMode) -> Unit) {
  SingleChoiceSegmentedButtonRow {
    listOf(GrainMode.EDGE_GRAIN to "Edge", GrainMode.END_GRAIN to "End").forEachIndexed {
      i,
      (m, text) ->
      SegmentedButton(
        selected = mode == m,
        onClick = { onPick(m) },
        shape = SegmentedButtonDefaults.itemShape(i, 2),
        modifier =
          Modifier.testTag(if (m == GrainMode.EDGE_GRAIN) "new-grain-edge" else "new-grain-end"),
      ) {
        Text(text)
      }
    }
  }
}

@Composable
private fun PatternPicker(kind: PatternKind, mode: GrainMode, onPick: (PatternKind) -> Unit) {
  Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
    PatternKind.entries.forEach { k ->
      FilterChip(
        selected = kind == k,
        onClick = { onPick(k) },
        enabled = mode == GrainMode.END_GRAIN || k == PatternKind.STRIPES,
        label = { Text(k.name.lowercase().replaceFirstChar { it.uppercase() }) },
        modifier = Modifier.testTag("new-pattern-${k.name.lowercase()}"),
      )
    }
  }
}

@Composable
private fun SizePicker(
  selected: SizePreset,
  formatter: UnitFormatter,
  onPick: (SizePreset) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
    PRESETS.forEach { p ->
      FilterChip(
        selected = selected === p,
        onClick = { onPick(p) },
        label = { Text(presetLabel(p, formatter)) },
        modifier = Modifier.testTag("new-size-${p.tag}"),
      )
    }
  }
}

private fun customSize(length: Length?, width: Length?, thickness: Length?): Dimensions? =
  if (length != null && width != null && thickness != null) {
    Dimensions(length, width, thickness)
  } else {
    null
  }
