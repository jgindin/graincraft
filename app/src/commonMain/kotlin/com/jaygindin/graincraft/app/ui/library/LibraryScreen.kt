package com.jaygindin.graincraft.app.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jaygindin.graincraft.app.data.StoredDesign
import com.jaygindin.graincraft.app.ui.theme.Spacing
import com.jaygindin.graincraft.core.wood.Woods
import graincraft.app.generated.resources.Res
import graincraft.app.generated.resources.ic_add
import graincraft.app.generated.resources.ic_grid_view
import graincraft.app.generated.resources.ic_more_vert
import graincraft.app.generated.resources.ic_upload_file
import graincraft.app.generated.resources.ic_warning
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

private val CARD_MIN_WIDTH = 220.dp
private val THUMBNAIL_HEIGHT = 140.dp
private val EMPTY_ICON_SIZE = 64.dp

/**
 * The designs as a grid of cards, with New and Import in the top bar.
 *
 * @param creating the new design dialog is showing; the app's new design shortcut opens it too.
 */
@Composable
fun LibraryScreen(
  viewModel: LibraryViewModel,
  onOpen: (String) -> Unit,
  creating: Boolean,
  onCreatingChange: (Boolean) -> Unit,
) {
  val state by viewModel.state.collectAsState()
  val snackbars = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  var renaming by remember { mutableStateOf<LibraryItem?>(null) }

  LaunchedEffect(state.pendingUndo) {
    val name = state.pendingUndo ?: return@LaunchedEffect
    val result =
      snackbars.showSnackbar(
        "Deleted $name",
        actionLabel = "Undo",
        duration = SnackbarDuration.Long,
      )
    viewModel.endUndo(restore = result == SnackbarResult.ActionPerformed)
  }
  LaunchedEffect(state.message) {
    val text = state.message ?: return@LaunchedEffect
    snackbars.showSnackbar(text)
    viewModel.clearMessage()
  }

  LibraryContent(
    state = state,
    snackbars = snackbars,
    onNew = { onCreatingChange(true) },
    onImport = { scope.launch { viewModel.importFile() } },
    onOpen = onOpen,
    onRename = { renaming = it },
    onDuplicate = viewModel::duplicate,
    onExport = { id -> scope.launch { viewModel.exportFile(id) } },
    onDelete = viewModel::delete,
  )

  if (creating) {
    NewDesignDialog(
      onCreate = { name, mode, kind, size ->
        onCreatingChange(false)
        onOpen(viewModel.create(name, mode, kind, size))
      },
      onDismiss = { onCreatingChange(false) },
    )
  }
  renaming?.let { item ->
    RenameDialog(
      current = item.name,
      onRename = {
        viewModel.rename(item.id, it)
        renaming = null
      },
      onDismiss = { renaming = null },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class) // TopAppBar is still experimental in Material 3
@Composable
internal fun LibraryContent(
  state: LibraryState,
  snackbars: SnackbarHostState,
  onNew: () -> Unit,
  onImport: () -> Unit,
  onOpen: (String) -> Unit,
  onRename: (LibraryItem) -> Unit,
  onDuplicate: (String) -> Unit,
  onExport: (String) -> Unit,
  onDelete: (String) -> Unit,
) {
  Scaffold(
    topBar = { LibraryTopBar(onNew, onImport) },
    snackbarHost = { UndoSnackbarHost(snackbars) },
    containerColor = MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxSize().testTag("screen-designs"),
  ) { padding ->
    Box(Modifier.padding(padding).fillMaxSize()) {
      if (state.loaded && state.items.isEmpty() && state.unreadable.isEmpty()) {
        EmptyLibrary(onNew)
      } else {
        LazyVerticalGrid(
          columns = GridCells.Adaptive(CARD_MIN_WIDTH),
          contentPadding = PaddingValues(Spacing.xl),
          horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
          verticalArrangement = Arrangement.spacedBy(Spacing.lg),
          modifier = Modifier.fillMaxSize().testTag("library-grid"),
        ) {
          items(state.items, key = { it.id }) { item ->
            DesignCard(item, state.woods, onOpen, onRename, onDuplicate, onExport, onDelete)
          }
          items(state.unreadable, key = { it.fileName }) { UnreadableCard(it) }
        }
      }
    }
  }
}

@Composable
private fun EmptyLibrary(onNew: () -> Unit) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(Spacing.lg, Alignment.CenterVertically),
    modifier = Modifier.fillMaxSize().testTag("library-empty"),
  ) {
    Icon(
      painterResource(Res.drawable.ic_grid_view),
      contentDescription = null,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(EMPTY_ICON_SIZE),
    )
    Text("No designs yet", style = MaterialTheme.typography.headlineSmall)
    Text(
      "Start a cutting board, or import a design file.",
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Button(onClick = onNew, modifier = Modifier.testTag("library-empty-new")) {
      Icon(painterResource(Res.drawable.ic_add), contentDescription = null)
      Spacer(Modifier.width(Spacing.sm))
      Text("New design")
    }
  }
}

@Composable
private fun DesignCard(
  item: LibraryItem,
  woods: Woods,
  onOpen: (String) -> Unit,
  onRename: (LibraryItem) -> Unit,
  onDuplicate: (String) -> Unit,
  onExport: (String) -> Unit,
  onDelete: (String) -> Unit,
) {
  var menuOpen by remember { mutableStateOf(false) }
  val act: (() -> Unit) -> Unit = {
    menuOpen = false
    it()
  }
  Card(
    onClick = { onOpen(item.id) },
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    modifier = Modifier.fillMaxWidth().testTag("design-card-${item.id}"),
  ) {
    DesignThumbnail(
      item.compiled,
      woods,
      Modifier.fillMaxWidth()
        .height(THUMBNAIL_HEIGHT)
        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    )
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(start = Spacing.lg, top = Spacing.sm, bottom = Spacing.sm),
    ) {
      Column(Modifier.weight(1f)) {
        Text(
          item.name,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          item.summary,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          "Edited ${formatDate(item.updatedAtMillis)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Box {
        IconButton(
          onClick = { menuOpen = true },
          modifier = Modifier.testTag("design-menu-${item.id}"),
        ) {
          Icon(painterResource(Res.drawable.ic_more_vert), contentDescription = "Actions")
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
          MenuItem("Open", "menu-open") { act { onOpen(item.id) } }
          MenuItem("Rename", "menu-rename") { act { onRename(item) } }
          MenuItem("Duplicate", "menu-duplicate") { act { onDuplicate(item.id) } }
          MenuItem("Export file…", "menu-export") { act { onExport(item.id) } }
          MenuItem("Delete", "menu-delete") { act { onDelete(item.id) } }
        }
      }
    }
  }
}

@Composable
private fun UnreadableCard(item: StoredDesign.Unreadable) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    modifier = Modifier.fillMaxWidth().testTag("unreadable-${item.fileName}"),
  ) {
    Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
      ) {
        Icon(
          painterResource(Res.drawable.ic_warning),
          contentDescription = null,
          tint = MaterialTheme.colorScheme.error,
        )
        Text("Can't open", style = MaterialTheme.typography.titleMedium)
      }
      Text(item.fileName, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
      Text(
        item.reason,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun RenameDialog(current: String, onRename: (String) -> Unit, onDismiss: () -> Unit) {
  var name by remember { mutableStateOf(current) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Rename design") },
    text = {
      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("rename-field"),
      )
    },
    confirmButton = {
      Button(
        onClick = { onRename(name) },
        enabled = name.isNotBlank(),
        modifier = Modifier.testTag("rename-confirm"),
      ) {
        Text("Rename")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun MenuItem(text: String, tag: String, onClick: () -> Unit) {
  DropdownMenuItem(text = { Text(text) }, onClick = onClick, modifier = Modifier.testTag(tag))
}

@OptIn(ExperimentalMaterial3Api::class) // TopAppBar is still experimental in Material 3
@Composable
private fun LibraryTopBar(onNew: () -> Unit, onImport: () -> Unit) {
  TopAppBar(
    title = { Text("Designs") },
    actions = {
      OutlinedButton(onClick = onImport, modifier = Modifier.testTag("library-import")) {
        Icon(painterResource(Res.drawable.ic_upload_file), contentDescription = null)
        Spacer(Modifier.width(Spacing.sm))
        Text("Import")
      }
      Spacer(Modifier.width(Spacing.sm))
      Button(onClick = onNew, modifier = Modifier.testTag("library-new")) {
        Icon(painterResource(Res.drawable.ic_add), contentDescription = null)
        Spacer(Modifier.width(Spacing.sm))
        Text("New")
      }
      Spacer(Modifier.width(Spacing.sm))
    },
    colors =
      TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
  )
}

@Composable
private fun UndoSnackbarHost(state: SnackbarHostState) {
  SnackbarHost(state) { data ->
    Snackbar(
      action = {
        data.visuals.actionLabel?.let { label ->
          TextButton(
            onClick = { data.performAction() },
            modifier = Modifier.testTag("snackbar-undo"),
          ) {
            Text(label)
          }
        }
      },
    ) {
      Text(data.visuals.message)
    }
  }
}
