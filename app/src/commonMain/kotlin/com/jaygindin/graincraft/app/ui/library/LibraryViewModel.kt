package com.jaygindin.graincraft.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaygindin.graincraft.app.AppGraph
import com.jaygindin.graincraft.app.data.StoredDesign
import com.jaygindin.graincraft.app.data.WorkspaceStore
import com.jaygindin.graincraft.app.data.isValidDesignId
import com.jaygindin.graincraft.app.platform.SaveResult
import com.jaygindin.graincraft.core.compile.CompiledDesign
import com.jaygindin.graincraft.core.design.BoardDesign
import com.jaygindin.graincraft.core.design.DesignDecode
import com.jaygindin.graincraft.core.design.DesignJson
import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.design.EdgeTreatments
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.FractionPrecision
import com.jaygindin.graincraft.core.units.LengthUnit
import com.jaygindin.graincraft.core.units.UnitFormatter
import com.jaygindin.graincraft.core.wood.Woods
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One card in the library; [compiled] is null when the design does not compile. */
data class LibraryItem(
  val id: String,
  val name: String,
  val summary: String,
  val updatedAtMillis: Long,
  val compiled: CompiledDesign?,
)

/**
 * What the library shows. [pendingUndo] is the name of the design just deleted, while it can still
 * be restored; [message] is a one-off notice (such as a failed import) for the screen to show once.
 */
data class LibraryState(
  val items: List<LibraryItem> = emptyList(),
  val unreadable: List<StoredDesign.Unreadable> = emptyList(),
  val pendingUndo: String? = null,
  val message: String? = null,
  val woods: Woods = Woods(emptyList()),
  val loaded: Boolean = false,
)

private val INCH_FORMATTER = UnitFormatter(LengthUnit.INCH, FractionPrecision.SIXTEENTH)
private const val UNTITLED = "Untitled design"
private const val INPUTS_DELAY_MILLIS = 300L

/**
 * The designs on disk, and the actions the library offers on them. Designs are compiled on
 * [compileDispatcher], off the main thread; a failed action becomes a message, never a crash.
 */
class LibraryViewModel(
  private val graph: AppGraph,
  private val woods: () -> Woods,
  private val profile: () -> ShopProfile,
  private val formatter: () -> UnitFormatter = { INCH_FORMATTER },
  private val compileDispatcher: CoroutineDispatcher = graph.compute,
) : ViewModel() {
  private val mutable = MutableStateFlow(LibraryState())
  val state: StateFlow<LibraryState> = mutable.asStateFlow()

  private val work =
    LibraryWork(graph.store, mutable, compileDispatcher) {
      CompileInputs(woods(), profile(), formatter())
    }
  private var lastDeletedId: String? = null
  private var pendingInputs: Job? = null

  init {
    refresh()
  }

  /** Reads the designs again, for example after leaving the editor. */
  fun refresh() {
    viewModelScope.launch { work.run() }
  }

  /**
   * The woods, shop profile or units may have changed. Typing in Shop changes the profile on every
   * key, so this waits until the changes stop, and does nothing if they end where the library
   * already is.
   */
  fun inputsChanged() {
    pendingInputs?.cancel()
    pendingInputs = viewModelScope.launch {
      delay(INPUTS_DELAY_MILLIS)
      if (!work.isCurrent) work.run()
    }
  }

  /** Saves a new design built from the chosen options and returns its id. */
  fun create(name: String, mode: GrainMode, pattern: PatternKind, size: Dimensions): String {
    val id = graph.newId()
    val design =
      BoardDesign(
        id = id,
        name = name.trim().ifBlank { UNTITLED },
        revision = 1,
        updatedAtMillis = graph.clock(),
        grainMode = mode,
        finished = size,
        pattern = defaultPattern(pattern),
        edges = EdgeTreatments(),
      )
    viewModelScope.launch { work.run("create the design") { graph.store.saveDesign(design) } }
    return id
  }

  /** The design to open, read after any queued action, so a design just created is found. */
  suspend fun load(id: String): BoardDesign? = work.read("open the design") { graph.store.find(id) }

  fun rename(id: String, name: String) {
    val newName = name.trim()
    if (newName.isEmpty()) return
    viewModelScope.launch {
      work.run("rename the design") {
        val design = graph.store.find(id) ?: return@run
        graph.store.saveDesign(
          design.copy(
            name = newName,
            revision = design.revision + 1,
            updatedAtMillis = graph.clock(),
          ),
        )
      }
    }
  }

  fun duplicate(id: String) {
    viewModelScope.launch {
      work.run("duplicate the design") {
        val design = graph.store.find(id) ?: return@run
        graph.store.saveDesign(
          design.copy(
            id = graph.newId(),
            name = "${design.name} copy",
            revision = 1,
            updatedAtMillis = graph.clock(),
          ),
        )
      }
    }
  }

  fun delete(id: String) {
    viewModelScope.launch {
      work.run("delete the design") {
        val design = graph.store.find(id) ?: return@run
        graph.store.trashDesign(id)
        lastDeletedId = id
        mutable.value = mutable.value.copy(pendingUndo = design.name)
      }
    }
  }

  /**
   * Ends the undo offer for the design just deleted: [restore] brings it back; otherwise it stays
   * in the trash until it is purged.
   */
  fun endUndo(restore: Boolean) {
    val id = lastDeletedId ?: return
    lastDeletedId = null
    if (!restore) {
      mutable.value = mutable.value.copy(pendingUndo = null)
      return
    }
    mutable.value = mutable.value.copy(pendingUndo = null)
    viewModelScope.launch { work.run("restore the design") { graph.store.restoreDesign(id) } }
  }

  fun clearMessage() {
    mutable.value = mutable.value.copy(message = null)
  }

  suspend fun importFile() {
    val text = work.attempt("import the file") { graph.dialogs.openDesignFile() } ?: return
    work.run("import the file") {
      when (val decoded = DesignJson.decode(text)) {
        is DesignDecode.NewerSchema ->
          mutable.say(
            "This file was made by a newer version of GrainCraft Studio and cannot be opened.",
          )
        is DesignDecode.Invalid ->
          mutable.say("This is not a GrainCraft design file: ${decoded.reason}")
        is DesignDecode.Ok -> graph.store.import(decoded.design, graph.newId)
      }
    }
  }

  suspend fun exportFile(id: String) {
    work.run("export the design") {
      val design = graph.store.find(id) ?: return@run
      val fileName = design.name.replace(Regex("""[^\p{L}\p{N} _-]"""), "_")
      val json = DesignJson.encode(design)
      when (val saved = graph.dialogs.saveFile("$fileName.graincraft", "json", json)) {
        is SaveResult.Saved -> mutable.say("Exported to ${saved.path}")
        is SaveResult.Failed -> mutable.say("Couldn't save: ${saved.reason}")
        SaveResult.Cancelled -> Unit
      }
    }
  }
}

private suspend fun WorkspaceStore.find(id: String): BoardDesign? =
  listDesigns().filterIsInstance<StoredDesign.Readable>().firstOrNull { it.design.id == id }?.design

/**
 * Saves an imported design: under a new id and name if its id is already taken (by a design or by a
 * file of that name the library cannot open), and under a new id alone if its id cannot name a
 * file.
 */
private suspend fun WorkspaceStore.import(design: BoardDesign, newId: () -> String) {
  val taken =
    listDesigns().any {
      (it is StoredDesign.Readable && it.design.id == design.id) ||
        it.fileName.startsWith("${design.id}.")
    }
  val saved =
    when {
      taken -> design.copy(id = newId(), name = "${design.name} (imported)")
      !isValidDesignId(design.id) -> design.copy(id = newId())
      else -> design
    }
  saveDesign(saved)
}
