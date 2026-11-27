package com.jaygindin.graincraft.app.ui.editor

import com.jaygindin.graincraft.core.compile.CompileResult
import com.jaygindin.graincraft.core.design.BoardDesign
import com.jaygindin.graincraft.core.wood.Woods

/** What the top bar's save status shows. */
enum class SaveStatus {
  /** Everything is on disk. */
  SAVED,

  /** A change is waiting for autosave or being written. */
  SAVING,

  /** The last write failed; the change is kept and the next edit or close tries again. */
  ERROR,
}

/** Which tones the preview paints the woods in. */
enum class Finish {
  RAW,
  OILED,
}

/**
 * Everything the design editor shows.
 *
 * @property compile the latest compile of [design], or null before the first one finishes; it lags
 *   an edit by the compile debounce.
 * @property woods the user's woods plus the design's embedded custom woods, as the compiler sees
 *   them.
 * @property pendingGrainSwitch the user asked for edge grain on a pattern that edge grain cannot
 *   make, and a confirmation is showing.
 */
data class EditorState(
  val design: BoardDesign,
  val compile: CompileResult?,
  val saveStatus: SaveStatus,
  val finish: Finish,
  val canUndo: Boolean,
  val canRedo: Boolean,
  val pendingGrainSwitch: Boolean,
  val woods: Woods,
)
