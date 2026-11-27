package com.jaygindin.graincraft.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaygindin.graincraft.app.data.WorkspaceStore
import com.jaygindin.graincraft.app.ui.library.PatternKind
import com.jaygindin.graincraft.app.ui.library.defaultPattern
import com.jaygindin.graincraft.core.compile.DesignCompiler
import com.jaygindin.graincraft.core.design.BoardDesign
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.design.PatternSpec
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.UnitFormatter
import com.jaygindin.graincraft.core.wood.WoodSpecies
import com.jaygindin.graincraft.core.wood.Woods
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val COMPILE_DELAY_MILLIS = 150L
private const val SAVE_DELAY_MILLIS = 1000L
private const val HISTORY_STEPS = 100

/**
 * The open design: edits with undo and redo, a compile 150 ms after the last change (and whenever
 * the woods, shop profile or units change), and an autosave 1 s after the last change. Nothing is
 * lost on close as long as the caller awaits [flush].
 *
 * @param scope where the debounces run; null means [viewModelScope]. Tests pass a test scope so
 *   virtual time drives them.
 * @param compileDispatcher where compiling runs, off the main thread.
 */
class EditorViewModel(
  initial: BoardDesign,
  private val store: WorkspaceStore,
  private val woods: StateFlow<Woods>,
  private val profile: StateFlow<ShopProfile>,
  private val formatter: StateFlow<UnitFormatter>,
  private val clock: () -> Long,
  scope: CoroutineScope? = null,
  private val compileDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
  private val scope: CoroutineScope = scope ?: viewModelScope

  private val mutable =
    MutableStateFlow(
      EditorState(
        design = initial,
        compile = null,
        saveStatus = SaveStatus.SAVED,
        finish = Finish.RAW,
        canUndo = false,
        canRedo = false,
        pendingGrainSwitch = false,
        woods = woods.value.withExtra(initial.embeddedCustomWoods),
      ),
    )
  val state: StateFlow<EditorState> = mutable.asStateFlow()

  private val undoStack = ArrayDeque<BoardDesign>()
  private val redoStack = ArrayDeque<BoardDesign>()

  // The coalescing key of the last edit, while further edits with it join the same undo step.
  private var runKey: String? = null

  // Bumped on every change to the design's content, so a save's new revision does not recompile.
  private val contentVersion = MutableStateFlow(0)

  private val saving = Mutex()
  private var dirty = false
  private var saveDelay: Job? = null

  init {
    this.scope.launch {
      var first = true
      combine(contentVersion, woods, profile, formatter) { _, w, p, f -> Triple(w, p, f) }
        .collectLatest { (w, p, f) ->
          mutable.update { it.copy(woods = w.withExtra(it.design.embeddedCustomWoods)) }
          // The design shows at once on open; after that, typing should not compile every key.
          if (!first) delay(COMPILE_DELAY_MILLIS)
          first = false
          val design = mutable.value.design
          val result = withContext(compileDispatcher) { DesignCompiler.compile(design, w, p, f) }
          mutable.update { it.copy(compile = result) }
        }
    }
  }

  /**
   * Applies [change] to the design as one undo step. Edits in a row with the same non-null
   * [coalesceKey], such as the keystrokes typed into one field, share a single step; any other
   * edit, an undo or a redo ends the run.
   */
  fun edit(coalesceKey: String? = null, change: (BoardDesign) -> BoardDesign) {
    val current = mutable.value.design
    val next = change(current)
    if (next == current) return
    val continuing = coalesceKey != null && coalesceKey == runKey && undoStack.isNotEmpty()
    runKey = coalesceKey
    if (!continuing) {
      undoStack.addLast(current)
      if (undoStack.size > HISTORY_STEPS) undoStack.removeFirst()
    }
    redoStack.clear()
    changed(next)
  }

  /** Switches grain at once, unless edge grain would need the pattern replaced: then it asks. */
  fun requestGrainMode(mode: GrainMode) {
    val design = mutable.value.design
    if (mode == design.grainMode) return
    if (mode == GrainMode.EDGE_GRAIN && design.pattern !is PatternSpec.Stripes) {
      mutable.update { it.copy(pendingGrainSwitch = true) }
    } else {
      edit { it.copy(grainMode = mode) }
    }
  }

  /** Switches to edge grain with the default stripes, as one undo step. */
  fun confirmGrainSwitch() {
    if (!mutable.value.pendingGrainSwitch) return
    mutable.update { it.copy(pendingGrainSwitch = false) }
    edit {
      it.copy(grainMode = GrainMode.EDGE_GRAIN, pattern = defaultPattern(PatternKind.STRIPES))
    }
  }

  fun cancelGrainSwitch() {
    mutable.update { it.copy(pendingGrainSwitch = false) }
  }

  fun undo() {
    runKey = null
    val previous = undoStack.removeLastOrNull() ?: return
    redoStack.addLast(mutable.value.design)
    changed(restore(previous, mutable.value.design))
  }

  fun redo() {
    runKey = null
    val next = redoStack.removeLastOrNull() ?: return
    undoStack.addLast(mutable.value.design)
    changed(restore(next, mutable.value.design))
  }

  fun setFinish(f: Finish) {
    mutable.update { it.copy(finish = f) }
  }

  /** Writes any pending change now; the editor calls this before it closes. */
  suspend fun flush() {
    saveDelay?.cancel()
    saveDelay = null
    // The window may be closing: a cancelled caller must not leave the change with no timer.
    withContext(NonCancellable) { save() }
  }

  private fun changed(design: BoardDesign) {
    dirty = true
    mutable.update {
      it.copy(
        design = design,
        saveStatus = SaveStatus.SAVING,
        canUndo = undoStack.isNotEmpty(),
        canRedo = redoStack.isNotEmpty(),
        woods = woods.value.withExtra(design.embeddedCustomWoods),
      )
    }
    contentVersion.value++
    saveDelay?.cancel()
    saveDelay = scope.launch {
      delay(SAVE_DELAY_MILLIS)
      // A write that has started finishes even if another edit arrives meanwhile.
      withContext(NonCancellable) { save() }
    }
  }

  // Any failure to write (a full disk, a newer file on disk) keeps the change for the next try.
  @Suppress("TooGenericExceptionCaught")
  private suspend fun save() {
    saving.withLock {
      if (!dirty) return
      val current = mutable.value.design
      val saved =
        current.copy(
          revision = current.revision + 1,
          updatedAtMillis = clock(),
          embeddedCustomWoods = usedCustomWoods(current, woods.value),
        )
      dirty = false
      try {
        store.saveDesign(saved)
      } catch (e: CancellationException) {
        dirty = true
        throw e
      } catch (ignored: Exception) {
        // The status is the whole message: the change stays pending and is written next time.
        dirty = true
        mutable.update { it.copy(saveStatus = SaveStatus.ERROR) }
        return
      }
      mutable.update {
        // An edit made during the write may use a wood the saved list dropped, so keep both then.
        val embedded =
          if (dirty)
            (it.design.embeddedCustomWoods + saved.embeddedCustomWoods).distinctBy { w -> w.id }
          else saved.embeddedCustomWoods
        it.copy(
          design =
            it.design.copy(
              revision = saved.revision,
              updatedAtMillis = saved.updatedAtMillis,
              embeddedCustomWoods = embedded,
            ),
          saveStatus = if (dirty) SaveStatus.SAVING else SaveStatus.SAVED,
          woods = woods.value.withExtra(embedded),
        )
      }
    }
  }
}

private fun speciesIds(pattern: PatternSpec): List<String> =
  when (pattern) {
    is PatternSpec.Stripes -> pattern.strips.map { it.speciesId }
    is PatternSpec.Checkerboard -> listOf(pattern.speciesA, pattern.speciesB)
    is PatternSpec.Brick -> listOf(pattern.brickSpecies, pattern.mortarSpecies)
  }.distinct()

/**
 * A design from the history, carrying the [current] revision forward so saves never count back, and
 * every embedded wood either knows in case the user has since deleted one.
 */
private fun restore(from: BoardDesign, current: BoardDesign): BoardDesign =
  from.copy(
    revision = current.revision,
    updatedAtMillis = current.updatedAtMillis,
    embeddedCustomWoods =
      (current.embeddedCustomWoods + from.embeddedCustomWoods).distinctBy { it.id },
  )

/**
 * A copy of each custom wood the pattern uses, from the user's [woods] when they still have it (it
 * may have been edited) and otherwise the copy the design already carries.
 */
private fun usedCustomWoods(design: BoardDesign, woods: Woods): List<WoodSpecies> {
  val customs = woods.customs.associateBy { it.id }
  val embedded = design.embeddedCustomWoods.associateBy { it.id }
  return speciesIds(design.pattern).mapNotNull { customs[it] ?: embedded[it] }
}
