package com.jaygindin.graincraft.core.guide

import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.fabrication.PanelKind
import com.jaygindin.graincraft.core.layout.buildLayout

// Diagrams are in inches from the top-left corner, with a margin all round.
private const val MARGIN = 0.5
private const val PANEL_GAP = 1.0
private const val LABEL_HEIGHT = 0.6
private const val DIMENSION_GAP = 0.5
private const val SWATCH = 0.4
private const val LEGEND_GAP = 0.3

/** An average label character's width in inches, for spacing the legend. */
private const val CHAR_WIDTH = 0.1

/**
 * Every panel's strip cross-section side by side, in glue-up order, coloured by species, with each
 * panel named and measured and a species legend underneath.
 */
internal fun glueDiagram(g: GuideContext): Diagram {
  val elements = mutableListOf<DiagramElement>()
  val top = MARGIN + LABEL_HEIGHT
  val tallest = g.fabrication.panels.maxOf { it.millHeight.inches }
  val dimensionY = top + tallest + DIMENSION_GAP
  var x = MARGIN
  for (panel in g.fabrication.panels) {
    val name =
      when {
        panel.kind == PanelKind.MORTAR && panel.name == "M" -> "Mortar"
        panel.name == "1" -> "Panel 1"
        else -> panel.name
      }
    elements += DiagramElement.Text(x, MARGIN + LABEL_HEIGHT / 2, name)
    var right = x
    for (strip in panel.strips) {
      val w = strip.millWidth.inches
      elements +=
        DiagramElement.Box(right, top, w, panel.millHeight.inches, colour(g, strip.speciesId), null)
      right += w
    }
    elements += DiagramElement.Dimension(x, dimensionY, right, dimensionY, g.f(panel.width))
    x = right + PANEL_GAP
  }
  val panelsRight = x - PANEL_GAP
  var legendX = MARGIN
  val legendY = dimensionY + DIMENSION_GAP
  val species = g.fabrication.panels.flatMap { p -> p.strips.map { it.speciesId } }.distinct()
  for (id in species) {
    elements += DiagramElement.Box(legendX, legendY, SWATCH, SWATCH, colour(g, id), null)
    val name = g.name(id)
    elements += DiagramElement.Text(legendX + SWATCH + LEGEND_GAP / 2, legendY + SWATCH, name)
    legendX += SWATCH + LEGEND_GAP + name.length * CHAR_WIDTH + LEGEND_GAP
  }
  val width = maxOf(panelsRight, legendX) + MARGIN
  return Diagram(width, legendY + SWATCH + MARGIN, elements)
}

/**
 * A side view of the first panel-1 panel: the squaring cut at one end, then one dashed line per
 * slice, so the drawing shows exactly the slices to cut.
 */
internal fun crosscutDiagram(g: GuideContext): Diagram {
  val panel = g.panel1.first()
  val t = checkNotNull(g.fabrication.sliceThickness)
  val slices = g.fabrication.slicesPerPanel.getValue(panel.name)
  val top = MARGIN + LABEL_HEIGHT
  val height = panel.flattenTo.inches
  val length = panel.length.inches
  val fill = colour(g, panel.strips.first().speciesId)
  val square = MARGIN + (g.profile.endTrim / 2).inches
  val pitch = (t + g.profile.kerf).inches
  val elements =
    mutableListOf<DiagramElement>(
      DiagramElement.Box(MARGIN, top, length, height, fill, panel.name),
      DiagramElement.Line(square, top, square, top + height),
      DiagramElement.Dimension(
        square,
        top - LABEL_HEIGHT / 2,
        square + t.inches,
        top - LABEL_HEIGHT / 2,
        g.f(t),
      ),
    )
  for (i in 1..slices) {
    val cut = square + i * pitch
    elements += DiagramElement.Line(cut, top, cut, top + height, dashed = true)
  }
  val dimensionY = top + height + DIMENSION_GAP
  elements +=
    DiagramElement.Dimension(MARGIN, dimensionY, MARGIN + length, dimensionY, g.f(panel.length))
  return Diagram(length + 2 * MARGIN, dimensionY + DIMENSION_GAP + MARGIN, elements)
}

/**
 * The glue-up from above, before its sides are trimmed: every piece of every row, with the rows
 * that are rotated 180° hatched.
 */
internal fun arrangeDiagram(g: GuideContext): Diagram {
  val f = g.fabrication
  val glueUp = Dimensions(f.glueUpLength, f.glueUpWidth, g.design.finished.thickness)
  val layout = buildLayout(f, glueUp, g.profile)
  val elements = mutableListOf<DiagramElement>()
  for (piece in layout.pieces) {
    val x0 = piece.polygon.minOf { it.x.inches }
    val x1 = piece.polygon.maxOf { it.x.inches }
    val y0 = piece.polygon.minOf { it.y.inches }
    val y1 = piece.polygon.maxOf { it.y.inches }
    elements +=
      DiagramElement.Box(
        MARGIN + x0,
        MARGIN + y0,
        x1 - x0,
        y1 - y0,
        colour(g, piece.speciesId),
        null,
        hatched = piece.flipped,
      )
  }
  val length = glueUp.length.inches
  val width = glueUp.width.inches
  val below = MARGIN + width + DIMENSION_GAP
  val right = MARGIN + length + DIMENSION_GAP
  elements += DiagramElement.Dimension(MARGIN, below, MARGIN + length, below, g.f(glueUp.length))
  elements += DiagramElement.Dimension(right, MARGIN, right, MARGIN + width, g.f(glueUp.width))
  var height = below + DIMENSION_GAP
  if (layout.pieces.any { it.flipped }) {
    height += LABEL_HEIGHT
    elements += DiagramElement.Text(MARGIN, height, "Hatched rows are rotated 180°.")
  }
  return Diagram(right + LABEL_HEIGHT * 2 + MARGIN, height + MARGIN, elements)
}

private fun colour(g: GuideContext, speciesId: String) = g.woods.byId(speciesId)?.raw?.base
