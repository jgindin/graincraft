package com.jaygindin.graincraft.core.guide

import com.jaygindin.graincraft.core.wood.Rgb

/**
 * One shape in a build diagram, in inches from the diagram's top-left corner. Diagrams are drawn on
 * screen and as SVG, so the coordinates are plain numbers rather than `Length`s.
 */
sealed interface DiagramElement {
  /** A rectangle; a null `fill` is an outline only, and `hatched` marks a rotated slice. */
  data class Box(
    val x: Double,
    val y: Double,
    val w: Double,
    val h: Double,
    val fill: Rgb?,
    val label: String?,
    val hatched: Boolean = false,
  ) : DiagramElement

  data class Line(
    val x1: Double,
    val y1: Double,
    val x2: Double,
    val y2: Double,
    val dashed: Boolean = false,
  ) : DiagramElement

  data class Text(val x: Double, val y: Double, val text: String) : DiagramElement

  /** A measured distance from one point to another, with its formatted length as `label`. */
  data class Dimension(
    val x1: Double,
    val y1: Double,
    val x2: Double,
    val y2: Double,
    val label: String,
  ) : DiagramElement
}

/** A build diagram `width × height` inches. */
data class Diagram(val width: Double, val height: Double, val elements: List<DiagramElement>)

enum class CalloutKind {
  WARNING,
  TIP,
}

/** A highlighted note on a stage: a safety `WARNING` or a piece of shop advice. */
data class Callout(val kind: CalloutKind, val text: String)

enum class StageKind {
  BUY_AND_MILL,
  PREPARE,
  GLUE_PANELS,
  FLATTEN_PANELS,
  CROSSCUT,
  ARRANGE_AND_GLUE,
  FLATTEN_BOARD,
  TRIM,
  EDGE_DETAILS,
  FINISH,
}

/** One stage of the build guide; `number` counts only the stages the board needs. */
data class Stage(
  val number: Int,
  val kind: StageKind,
  val title: String,
  val steps: List<String>,
  val callouts: List<Callout>,
  val diagram: Diagram?,
)
