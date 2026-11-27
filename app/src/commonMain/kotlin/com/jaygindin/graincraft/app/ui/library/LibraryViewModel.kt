package com.jaygindin.graincraft.app.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaygindin.graincraft.app.AppGraph
import com.jaygindin.graincraft.app.data.StoredDesign
import com.jaygindin.graincraft.app.data.WorkspaceStore
import com.jaygindin.graincraft.app.platform.SaveResult
import com.jaygindin.graincraft.core.compile.CompileResult
import com.jaygindin.graincraft.core.compile.CompiledDesign
import com.jaygindin.graincraft.core.compile.DesignCompiler
import com.jaygindin.graincraft.core.design.BoardDesign
import com.jaygindin.graincraft.core.design.DesignDecode
import com.jaygindin.graincraft.core.design.DesignJson
import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.design.EdgeTreatments
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.design.PatternSpec
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.FractionPrecision
import com.jaygindin.graincraft.core.units.LengthUnit
import com.jaygindin.graincraft.core.units.UnitFormatter
import com.jaygindin.graincraft.core.wood.Woods
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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

/** The designs on disk, and the actions the library offers on them. */
class LibraryViewModel(
  private val graph: AppGraph,
  private val woods: () -> Woods,
  private val profile: () -> ShopProfile,
  private val formatter: () -> UnitFormatter = { INCH_FORMATTER },
) : ViewModel() {
  private val mutable = MutableStateFlow(LibraryState())
  val state: StateFlow<LibraryState> = mutable.asStateFlow()

  // Actions queue in order so each one sees the files the previous one left.
  private val working = Mutex()
  private var lastDeletedId: String? = null

  init {
    refresh()
  }

  /** Reads the designs again, for example after the units changed. */
  fun refresh() {
    viewModelScope.launch { locked {} }
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
    viewModelScope.launch { locked { graph.store.saveDesign(design) } }
    return id
  }

  /** The design to open, read after any queued action, so a design just created is found. */
  suspend fun load(id: String): BoardDesign? = working.withLock { graph.store.find(id) }

  fun rename(id: String, name: String) {
    val newName = name.trim()
    if (newName.isEmpty()) return
    viewModelScope.launch {
      locked {
        val design = graph.store.find(id) ?: return@locked
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
      locked {
        val design = graph.store.find(id) ?: return@locked
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
      locked {
        val design = graph.store.find(id) ?: return@locked
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
    viewModelScope.launch {
      locked {
        graph.store.restoreDesign(id)
        mutable.value = mutable.value.copy(pendingUndo = null)
      }
    }
  }

  fun clearMessage() {
    mutable.value = mutable.value.copy(message = null)
  }

  suspend fun importFile() {
    val text = graph.dialogs.openDesignFile() ?: return
    locked {
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
    locked {
      val design = graph.store.find(id) ?: return@locked
      val fileName = design.name.replace(Regex("""[^\p{L}\p{N} _-]"""), "_")
      val json = DesignJson.encode(design)
      when (val saved = graph.dialogs.saveFile("$fileName.graincraft", "json", json)) {
        is SaveResult.Saved -> mutable.say("Exported to ${saved.path}")
        is SaveResult.Failed -> mutable.say("Couldn't save: ${saved.reason}")
        SaveResult.Cancelled -> Unit
      }
    }
  }

  /**
   * Runs [action], then reads the files again so the state shows them as they now are; a store
   * refusal becomes a message.
   */
  private suspend fun locked(action: suspend () -> Unit) {
    working.withLock {
      try {
        action()
      } catch (e: IllegalStateException) {
        mutable.say(e.message ?: "That did not work")
      }
      val listed = graph.store.listDesigns()
      val woodsNow = woods()
      val profileNow = profile()
      val format = formatter()
      val items =
        listed.filterIsInstance<StoredDesign.Readable>().map { stored ->
          val design = stored.design
          val result = DesignCompiler.compile(design, woodsNow, profileNow, format)
          LibraryItem(
            design.id,
            design.name,
            summary(design, format),
            design.updatedAtMillis,
            (result as? CompileResult.Ok)?.compiled,
          )
        }
      mutable.value =
        mutable.value.copy(
          items = items,
          unreadable = listed.filterIsInstance<StoredDesign.Unreadable>(),
          woods = woodsNow,
          loaded = true,
        )
    }
  }
}

private fun summary(design: BoardDesign, format: UnitFormatter): String {
  val grain = if (design.grainMode == GrainMode.END_GRAIN) "End-grain" else "Edge-grain"
  val pattern =
    when (design.pattern) {
      is PatternSpec.Stripes -> "stripes"
      is PatternSpec.Checkerboard -> "checkerboard"
      is PatternSpec.Brick -> "brick"
    }
  val size = "${format.format(design.finished.length)} × ${format.format(design.finished.width)}"
  return "$size · $grain $pattern"
}

private suspend fun WorkspaceStore.find(id: String): BoardDesign? =
  listDesigns().filterIsInstance<StoredDesign.Readable>().firstOrNull { it.design.id == id }?.design

private fun MutableStateFlow<LibraryState>.say(text: String) {
  value = value.copy(message = text)
}

/** Saves an imported design, under a new id and name if its id is already taken. */
private suspend fun WorkspaceStore.import(design: BoardDesign, newId: () -> String) {
  val taken = listDesigns().any { it is StoredDesign.Readable && it.design.id == design.id }
  saveDesign(if (taken) design.copy(id = newId(), name = "${design.name} (imported)") else design)
}
