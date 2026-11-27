package com.jaygindin.graincraft.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.jaygindin.graincraft.app.ui.LocalUnitFormatter
import com.jaygindin.graincraft.app.ui.components.LengthField
import com.jaygindin.graincraft.app.ui.library.PatternKind
import com.jaygindin.graincraft.app.ui.library.defaultPattern
import com.jaygindin.graincraft.app.ui.theme.Spacing
import com.jaygindin.graincraft.core.compile.message
import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.design.PatternSpec
import com.jaygindin.graincraft.core.fabrication.CompileError
import com.jaygindin.graincraft.core.units.Length

/**
 * The left pane: grain, finished size, pattern and its parameters, and edge details. Every change
 * is one undo step, applied to the design as it is when the change lands.
 */
@Composable
internal fun DesignPane(
  state: EditorState,
  viewModel: EditorViewModel,
  errors: List<CompileError>,
  modifier: Modifier,
) {
  val design = state.design
  Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = modifier) {
    Column(
      Modifier.verticalScroll(rememberScrollState()).padding(Spacing.lg),
      verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
      Heading("Grain")
      GrainPicker(design.grainMode, viewModel::requestGrainMode)
      HorizontalDivider()
      Heading("Finished size")
      SizeFields(design.finished, errors) { key, f ->
        viewModel.edit(key) { it.copy(finished = f(it.finished)) }
      }
      HorizontalDivider()
      Heading("Pattern")
      PatternPicker(design.pattern, design.grainMode) { kind ->
        viewModel.edit {
          if (it.pattern.kind == kind) it else it.copy(pattern = defaultPattern(kind))
        }
      }
      PatternParams(design.pattern, design.grainMode, design.finished.width, state.woods) { key, f
        ->
        viewModel.edit(key) { it.copy(pattern = f(it.pattern)) }
      }
      HorizontalDivider()
      Heading("Edge details")
      EdgeDetailsSection(design.edges) { key, f ->
        viewModel.edit(key) { it.copy(edges = f(it.edges)) }
      }
    }
  }
}

/**
 * Changes one part of the design, [T], as an undo step. A text field passes its own key, so its
 * keystrokes share one step; other controls pass none.
 */
internal fun interface PartUpdate<T> {
  fun update(key: String?, change: (T) -> T)
}

internal operator fun <T> PartUpdate<T>.invoke(change: (T) -> T) = update(null, change)

/** The same updates, aimed at a part [P] of [T] that [lift] applies a change to. */
internal fun <T, P> PartUpdate<T>.part(lift: (T, (P) -> P) -> T): PartUpdate<P> =
  PartUpdate { key, change ->
    update(key) { lift(it, change) }
  }

@Composable
private fun Heading(text: String) {
  Text(text, style = MaterialTheme.typography.titleSmall)
}

@OptIn(ExperimentalMaterial3Api::class) // SegmentedButton is still experimental in Material 3
@Composable
private fun GrainPicker(mode: GrainMode, onPick: (GrainMode) -> Unit) {
  SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
    listOf(GrainMode.EDGE_GRAIN to "Edge grain", GrainMode.END_GRAIN to "End grain")
      .forEachIndexed { i, (m, text) ->
        SegmentedButton(
          selected = mode == m,
          onClick = { onPick(m) },
          shape = SegmentedButtonDefaults.itemShape(i, 2),
          modifier = Modifier.testTag(if (m == GrainMode.EDGE_GRAIN) "grain-edge" else "grain-end"),
        ) {
          Text(text)
        }
      }
  }
}

/** Length, width and thickness; a size the compiler refuses shows why under its field. */
@Composable
private fun SizeFields(
  finished: Dimensions,
  errors: List<CompileError>,
  update: PartUpdate<Dimensions>,
) {
  val formatter = LocalUnitFormatter.current
  fun problem(field: String): String? =
    errors
      .firstOrNull {
        (it is CompileError.OutOfRange && it.field == field) ||
          (it is CompileError.NonPositiveDimension && it.field == field)
      }
      ?.message(formatter)

  @Composable
  fun field(label: String, field: String, value: Length, set: (Dimensions, Length) -> Dimensions) =
    LengthField(
      label = label,
      value = value,
      onValueChange = { l -> update.update("dim-$field") { set(it, l) } },
      testTag = "dim-$field",
      supportingText = problem(field),
      allowZero = false,
      modifier = Modifier.fillMaxWidth(),
    )

  Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
    field("Length", "length", finished.length) { d, l -> d.copy(length = l) }
    field("Width", "width", finished.width) { d, l -> d.copy(width = l) }
    field("Thickness", "thickness", finished.thickness) { d, l -> d.copy(thickness = l) }
  }
}

@Composable
private fun PatternPicker(pattern: PatternSpec, mode: GrainMode, onPick: (PatternKind) -> Unit) {
  Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
    PatternKind.entries.forEach { k ->
      FilterChip(
        selected = pattern.kind == k,
        onClick = { onPick(k) },
        // Edge grain can only be striped: the other patterns need end-to-end glue joints.
        enabled = mode == GrainMode.END_GRAIN || k == PatternKind.STRIPES,
        label = { Text(k.name.lowercase().replaceFirstChar { it.uppercase() }) },
        modifier = Modifier.testTag("pattern-${k.name.lowercase()}"),
      )
    }
  }
}

private val PatternSpec.kind: PatternKind
  get() =
    when (this) {
      is PatternSpec.Stripes -> PatternKind.STRIPES
      is PatternSpec.Checkerboard -> PatternKind.CHECKERBOARD
      is PatternSpec.Brick -> PatternKind.BRICK
    }
