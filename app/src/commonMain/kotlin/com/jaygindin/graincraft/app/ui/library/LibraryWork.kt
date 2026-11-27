package com.jaygindin.graincraft.app.ui.library

import com.jaygindin.graincraft.app.data.StoredDesign
import com.jaygindin.graincraft.app.data.WorkspaceStore
import com.jaygindin.graincraft.core.compile.CompileResult
import com.jaygindin.graincraft.core.compile.DesignCompiler
import com.jaygindin.graincraft.core.design.BoardDesign
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.design.PatternSpec
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.UnitFormatter
import com.jaygindin.graincraft.core.wood.Woods
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** What every thumbnail is compiled with; a change to any of them rebuilds the library. */
internal data class CompileInputs(
  val woods: Woods,
  val profile: ShopProfile,
  val formatter: UnitFormatter,
)

/**
 * The library's work queue. Actions run one at a time, so each sees the files the previous one
 * left; a failure becomes "Couldn't <what>: reason" in [state]; and after each action the designs
 * are listed again and compiled on [compileDispatcher], off the main thread.
 */
internal class LibraryWork(
  private val store: WorkspaceStore,
  private val state: MutableStateFlow<LibraryState>,
  private val compileDispatcher: CoroutineDispatcher,
  private val inputs: () -> CompileInputs,
) {
  private val working = Mutex()
  private var compiledWith: CompileInputs? = null

  /** True when the listed thumbnails were compiled with the current woods, profile and units. */
  val isCurrent: Boolean
    get() = inputs() == compiledWith

  suspend fun run(what: String = "", action: suspend () -> Unit = {}) {
    working.withLock {
      attempt(what) { action() }
      attempt("read your designs") { reload() }
    }
  }

  /** [block]'s result after any queued action, or null when it failed (and said so). */
  suspend fun <T> read(what: String, block: suspend () -> T): T? = working.withLock {
    attempt(what, block)
  }

  // Store and dialog failures are platform exceptions (an IOException on the desktop) that common
  // code cannot name, so everything but cancellation is caught and shown.
  @Suppress("TooGenericExceptionCaught")
  suspend fun <T> attempt(what: String, block: suspend () -> T): T? =
    try {
      block()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      state.say("Couldn't $what: ${e.message ?: e::class.simpleName}")
      null
    }

  private suspend fun reload() {
    val listed = store.listDesigns()
    val now = inputs()
    val items =
      withContext(compileDispatcher) {
        listed.filterIsInstance<StoredDesign.Readable>().map { item(it.design, now) }
      }
    compiledWith = now
    state.value =
      state.value.copy(
        items = items,
        unreadable = listed.filterIsInstance<StoredDesign.Unreadable>(),
        woods = now.woods,
        loaded = true,
      )
  }
}

internal fun MutableStateFlow<LibraryState>.say(text: String) {
  value = value.copy(message = text)
}

private fun item(design: BoardDesign, inputs: CompileInputs): LibraryItem {
  val result = DesignCompiler.compile(design, inputs.woods, inputs.profile, inputs.formatter)
  return LibraryItem(
    design.id,
    design.name,
    summary(design, inputs.formatter),
    design.updatedAtMillis,
    (result as? CompileResult.Ok)?.compiled,
  )
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
