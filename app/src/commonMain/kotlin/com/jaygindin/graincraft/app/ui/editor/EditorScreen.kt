package com.jaygindin.graincraft.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jaygindin.graincraft.app.ui.LocalUnitFormatter
import com.jaygindin.graincraft.app.ui.preview.BoardPreview
import com.jaygindin.graincraft.app.ui.theme.Spacing
import com.jaygindin.graincraft.core.compile.CompileResult
import com.jaygindin.graincraft.core.compile.CompiledDesign
import com.jaygindin.graincraft.core.compile.message
import com.jaygindin.graincraft.core.fabrication.CompileError
import graincraft.app.generated.resources.Res
import graincraft.app.generated.resources.ic_arrow_back
import graincraft.app.generated.resources.ic_error
import graincraft.app.generated.resources.ic_print
import graincraft.app.generated.resources.ic_redo
import graincraft.app.generated.resources.ic_undo
import org.jetbrains.compose.resources.painterResource

private val THREE_PANES_FROM = 1200.dp
private val DESIGN_PANE_WIDTH = 360.dp
private val RESULTS_PANE_WIDTH = 380.dp
private val NAME_FIELD_WIDTH = 320.dp

/**
 * The design editor: the design's parameters on the left, the board in the centre, and its issues,
 * build guide and lumber on the right. Below 1200 dp of window width the right pane becomes tabs
 * beside the preview.
 */
@Composable
fun EditorScreen(viewModel: EditorViewModel, onBack: () -> Unit, onExport: () -> Unit) {
  val state by viewModel.state.collectAsState()
  val focus = LocalFocusManager.current
  val compiled = rememberLastCompiled(state.compile)
  val errors = (state.compile as? CompileResult.Failed)?.errors.orEmpty()
  var tab by rememberSaveable { mutableStateOf(EditorTab.PREVIEW) }

  Scaffold(
    topBar = {
      EditorTopBar(
        state = state,
        onBack = onBack,
        onRename = { name -> viewModel.edit("name") { it.copy(name = name) } },
        // A field being typed in would otherwise keep showing its text over the restored value.
        onUndo = {
          focus.clearFocus()
          viewModel.undo()
        },
        onRedo = {
          focus.clearFocus()
          viewModel.redo()
        },
        onFinish = viewModel::setFinish,
        onExport = onExport,
        canExport = state.compile is CompileResult.Ok,
      )
    },
    containerColor = MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxSize().testTag("screen-editor"),
  ) { padding ->
    Box(Modifier.padding(padding).fillMaxSize()) {
      // The window's width, not the editor's: the rail beside the editor does not count.
      val threePanes = windowWidth() >= THREE_PANES_FROM
      Row(Modifier.fillMaxSize()) {
        DesignPane(state, viewModel, errors, Modifier.width(DESIGN_PANE_WIDTH).fillMaxHeight())
        VerticalDivider()
        if (threePanes) {
          Centre(state, compiled, errors, Modifier.weight(1f).fillMaxHeight())
          VerticalDivider()
          Column(Modifier.width(RESULTS_PANE_WIDTH).fillMaxHeight()) {
            val shown = if (tab == EditorTab.PREVIEW) EditorTab.ISSUES else tab
            ResultTabs(EditorTab.RESULTS, shown, onSelect = { tab = it })
            ResultContent(shown, state, compiled, errors, Modifier.weight(1f))
          }
        } else {
          Column(Modifier.weight(1f).fillMaxHeight()) {
            ResultTabs(EditorTab.entries, tab, onSelect = { tab = it })
            if (tab == EditorTab.PREVIEW) {
              Centre(state, compiled, errors, Modifier.weight(1f).fillMaxWidth())
            } else {
              ResultContent(tab, state, compiled, errors, Modifier.weight(1f))
            }
          }
        }
      }
    }
  }

  if (state.pendingGrainSwitch) {
    GrainSwitchDialog(viewModel::confirmGrainSwitch, viewModel::cancelGrainSwitch)
  }
}

@Composable
private fun windowWidth(): Dp =
  with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }

/**
 * The latest successful compile. While the design has errors the preview keeps showing the last
 * board that worked, so one bad keystroke does not blank the screen.
 */
@Composable
private fun rememberLastCompiled(compile: CompileResult?): CompiledDesign? {
  val last = remember { mutableStateOf<CompiledDesign?>(null) }
  val now = (compile as? CompileResult.Ok)?.compiled
  SideEffect { if (now != null) last.value = now }
  return now ?: last.value
}

@OptIn(ExperimentalMaterial3Api::class) // TopAppBar and SegmentedButton are still experimental
@Composable
private fun EditorTopBar(
  state: EditorState,
  onBack: () -> Unit,
  onRename: (String) -> Unit,
  onUndo: () -> Unit,
  onRedo: () -> Unit,
  onFinish: (Finish) -> Unit,
  onExport: () -> Unit,
  canExport: Boolean,
) {
  TopAppBar(
    navigationIcon = {
      IconButton(onClick = onBack, modifier = Modifier.testTag("editor-back")) {
        Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = "Back")
      }
    },
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
      ) {
        NameField(state.design.name, onRename)
        SaveStatusText(state.saveStatus)
      }
    },
    actions = {
      IconButton(
        onClick = onUndo,
        enabled = state.canUndo,
        modifier = Modifier.testTag("editor-undo"),
      ) {
        Icon(painterResource(Res.drawable.ic_undo), contentDescription = "Undo")
      }
      IconButton(
        onClick = onRedo,
        enabled = state.canRedo,
        modifier = Modifier.testTag("editor-redo"),
      ) {
        Icon(painterResource(Res.drawable.ic_redo), contentDescription = "Redo")
      }
      FinishPicker(state.finish, onFinish, Modifier.padding(horizontal = Spacing.md))
      FilledTonalButton(
        onClick = onExport,
        enabled = canExport,
        modifier = Modifier.padding(end = Spacing.md).testTag("editor-export"),
      ) {
        Icon(
          painterResource(Res.drawable.ic_print),
          contentDescription = null,
          modifier = Modifier.size(SegmentedButtonDefaults.IconSize),
        )
        Text("Export packet", Modifier.padding(start = Spacing.sm))
      }
    },
    colors =
      TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
  )
}

/**
 * The design's name, renamed as it is typed so leaving the editor never loses it; a blank field
 * leaves the old name and shows it again when the field is left.
 */
@Composable
private fun NameField(name: String, onRename: (String) -> Unit) {
  val focus = LocalFocusManager.current
  var text by remember { mutableStateOf(name) }
  // A name changed from outside the field, such as by undo, replaces what it shows; the user's
  // own typing (spaces at the end included) is left alone.
  LaunchedEffect(name) { if (text.trim() != name) text = name }
  OutlinedTextField(
    value = text,
    onValueChange = {
      text = it
      val trimmed = it.trim()
      if (trimmed.isNotEmpty() && trimmed != name) onRename(trimmed)
    },
    singleLine = true,
    textStyle = MaterialTheme.typography.titleMedium,
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
    keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
    modifier =
      Modifier.width(NAME_FIELD_WIDTH).testTag("editor-name").onFocusChanged {
        if (!it.isFocused && text.isBlank()) text = name
      },
  )
}

@Composable
private fun SaveStatusText(status: SaveStatus) {
  val (text, colour) =
    when (status) {
      SaveStatus.SAVED -> "Saved" to MaterialTheme.colorScheme.onSurfaceVariant
      SaveStatus.SAVING -> "Saving…" to MaterialTheme.colorScheme.onSurfaceVariant
      SaveStatus.ERROR -> "Couldn't save; will retry" to MaterialTheme.colorScheme.error
    }
  Text(
    text,
    style = MaterialTheme.typography.labelLarge,
    color = colour,
    modifier = Modifier.testTag("editor-save-status"),
  )
}

@OptIn(ExperimentalMaterial3Api::class) // SegmentedButton is still experimental in Material 3
@Composable
private fun FinishPicker(finish: Finish, onPick: (Finish) -> Unit, modifier: Modifier) {
  SingleChoiceSegmentedButtonRow(modifier) {
    Finish.entries.forEachIndexed { i, f ->
      SegmentedButton(
        selected = finish == f,
        onClick = { onPick(f) },
        shape = SegmentedButtonDefaults.itemShape(i, Finish.entries.size),
        modifier = Modifier.testTag("editor-finish-${f.name.lowercase()}"),
      ) {
        Text(if (f == Finish.RAW) "Raw" else "Oiled")
      }
    }
  }
}

/** The board preview, with any compile errors in a card above it. */
@Composable
private fun Centre(
  state: EditorState,
  compiled: CompiledDesign?,
  errors: List<CompileError>,
  modifier: Modifier,
) {
  var showDimensions by rememberSaveable { mutableStateOf(true) }
  Column(modifier) {
    if (errors.isNotEmpty()) {
      CompileErrorCard(errors, Modifier.fillMaxWidth().padding(Spacing.lg))
    }
    Box(Modifier.weight(1f).fillMaxWidth()) {
      if (compiled != null) {
        BoardPreview(
          layout = compiled.layout,
          woods = state.woods,
          finish = state.finish,
          edges = state.design.edges,
          target = state.design.finished,
          showDimensions = showDimensions,
          modifier = Modifier.fillMaxSize(),
        )
      } else if (errors.isEmpty()) {
        CircularProgressIndicator(Modifier.align(Alignment.Center))
      }
      FilterChip(
        selected = showDimensions,
        onClick = { showDimensions = !showDimensions },
        label = { Text("Dimensions") },
        modifier =
          Modifier.align(Alignment.TopEnd).padding(Spacing.md).testTag("preview-dimensions-toggle"),
      )
    }
  }
}

@Composable
private fun CompileErrorCard(errors: List<CompileError>, modifier: Modifier) {
  val formatter = LocalUnitFormatter.current
  Card(
    colors =
      CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
      ),
    modifier = modifier.testTag("compile-error"),
  ) {
    Row(Modifier.padding(Spacing.lg), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
      Icon(painterResource(Res.drawable.ic_error), contentDescription = "Error")
      Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text("This design can't be built yet", style = MaterialTheme.typography.titleSmall)
        errors.forEachIndexed { i, error ->
          Text(
            error.message(formatter),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("compile-error-$i"),
          )
        }
      }
    }
  }
}

@Composable
private fun GrainSwitchDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
  AlertDialog(
    onDismissRequest = onCancel,
    title = { Text("Switch to edge grain?") },
    text = {
      Text(
        "Edge-grain boards can only be striped, so this pattern will be replaced with stripes. " +
          "You can undo the switch.",
      )
    },
    confirmButton = {
      TextButton(onClick = onConfirm, modifier = Modifier.testTag("grain-switch-confirm")) {
        Text("Switch to stripes")
      }
    },
    dismissButton = {
      TextButton(onClick = onCancel, modifier = Modifier.testTag("grain-switch-cancel")) {
        Text("Cancel")
      }
    },
  )
}
