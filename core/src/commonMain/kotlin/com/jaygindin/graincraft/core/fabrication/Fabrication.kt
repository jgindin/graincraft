package com.jaygindin.graincraft.core.fabrication

import com.jaygindin.graincraft.core.design.FlatteningMethod
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.units.Length

enum class PanelKind {
  EDGE_GRAIN_BOARD,
  PANEL_1,
  MORTAR,
}

/**
 * One strip of a long-grain panel. `sourceStripIndex` points into the fit's columns; a mortar
 * sub-panel board has `-1 - subPanelIndex`.
 */
data class PanelStrip(
  val speciesId: String,
  val millWidth: Length,
  val sourceStripIndex: Int,
  val role: StripRole,
)

/**
 * A long-grain glue-up: strips are milled to `millHeight`, and the panel is flattened to
 * `flattenTo`.
 */
data class Panel(
  val name: String,
  val kind: PanelKind,
  val strips: List<PanelStrip>,
  val width: Length,
  val millHeight: Length,
  val flattenTo: Length,
  val length: Length,
  val flattening: LongGrainFlattening,
)

/** One row of the final glue-up; `panels` are the sub-panel names, left to right as placed. */
data class Row(
  val index: Int,
  val kind: RowKind,
  val height: Length,
  val rotated: Boolean,
  val shift: Length,
  val panels: List<String>,
)

/** How the board is built: the panels, how they are sliced and how the slices go together. */
data class Fabrication(
  val mode: GrainMode,
  val panels: List<Panel>,
  /** Null for edge grain, which is not sliced. */
  val sliceThickness: Length?,
  val slicesPerPanel: Map<String, Int>,
  val rows: List<Row>,
  val glueUpWidth: Length,
  val glueUpLength: Length,
  /** Null for edge grain, which is flattened in the long-grain tool. */
  val finalFlatten: FlatteningMethod?,
  val finalAllowance: Length,
)

sealed interface FabricationResult {
  data class Ok(val fabrication: Fabrication) : FabricationResult

  data class Failed(val error: CompileError) : FabricationResult
}
