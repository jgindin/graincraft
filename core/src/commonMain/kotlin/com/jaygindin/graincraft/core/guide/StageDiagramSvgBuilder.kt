package com.jaygindin.graincraft.core.guide

import com.jaygindin.graincraft.core.wood.Rgb
import kotlin.math.abs
import kotlin.math.roundToLong

private const val SVG_NAMESPACE = "http://www.w3.org/2000/svg"
private const val STROKE = "#333333"
private const val LABEL_PX = 11
private const val HATCH_PX = 6
private const val TICK_PX = 4.0
private const val DIMENSION_GAP_PX = 4.0
private const val HUNDREDTHS = 100
private const val RGB_MASK = 0xFFFFFF
private const val HEX_DIGITS = 6
private const val HEX_RADIX = 16

/**
 * Draws a [Diagram] as a self-contained SVG string: plain shapes, species fills and labels, with no
 * external references, so a shop packet can embed it as is.
 */
object StageDiagramSvgBuilder {
  fun svg(diagram: Diagram, pixelsPerInch: Double = 24.0): String {
    val px = { inches: Double -> num(inches * pixelsPerInch) }
    val out = StringBuilder()
    val w = px(diagram.width)
    val h = px(diagram.height)
    out.append("<svg xmlns=\"$SVG_NAMESPACE\" width=\"$w\" height=\"$h\" viewBox=\"0 0 $w $h\"")
    out.append(" font-family=\"sans-serif\" font-size=\"$LABEL_PX\">")
    hatchPatterns(diagram, out)
    for (element in diagram.elements) {
      when (element) {
        is DiagramElement.Box -> box(element, px, out)
        is DiagramElement.Line -> line(element, px, out)
        is DiagramElement.Text ->
          out
            .append("<text x=\"${px(element.x)}\" y=\"${px(element.y)}\" fill=\"$STROKE\">")
            .append(escape(element.text))
            .append("</text>")
        is DiagramElement.Dimension -> dimension(element, pixelsPerInch, out)
      }
    }
    out.append("</svg>")
    return out.toString()
  }

  /** One pattern per hatched fill colour, so a hatched box keeps its species colour underneath. */
  private fun hatchPatterns(diagram: Diagram, out: StringBuilder) {
    val fills =
      diagram.elements
        .filterIsInstance<DiagramElement.Box>()
        .filter { it.hatched }
        .map { it.fill }
        .distinct()
    if (fills.isEmpty()) return
    out.append("<defs>")
    for (fill in fills) {
      out.append("<pattern id=\"${hatchId(fill)}\" patternUnits=\"userSpaceOnUse\"")
      out.append(" width=\"$HATCH_PX\" height=\"$HATCH_PX\" patternTransform=\"rotate(45)\">")
      out.append("<path d=\"M0 0H${HATCH_PX}V${HATCH_PX}H0Z\" fill=\"${colour(fill)}\"/>")
      out.append("<path d=\"M0 0V$HATCH_PX\" stroke=\"$STROKE\" stroke-width=\"1.5\"/>")
      out.append("</pattern>")
    }
    out.append("</defs>")
  }

  private fun box(b: DiagramElement.Box, px: (Double) -> String, out: StringBuilder) {
    val fill = if (b.hatched) "url(#${hatchId(b.fill)})" else colour(b.fill)
    out.append("<rect x=\"${px(b.x)}\" y=\"${px(b.y)}\" width=\"${px(b.w)}\"")
    out.append(" height=\"${px(b.h)}\" fill=\"$fill\" stroke=\"$STROKE\" stroke-width=\"1\"/>")
    val label = b.label ?: return
    out.append("<text x=\"${px(b.x + b.w / 2)}\" y=\"${px(b.y + b.h / 2)}\"")
    out.append(" text-anchor=\"middle\" dominant-baseline=\"central\" fill=\"$STROKE\">")
    out.append(escape(label)).append("</text>")
  }

  private fun line(l: DiagramElement.Line, px: (Double) -> String, out: StringBuilder) {
    out.append("<line x1=\"${px(l.x1)}\" y1=\"${px(l.y1)}\" x2=\"${px(l.x2)}\" y2=\"${px(l.y2)}\"")
    out.append(" stroke=\"$STROKE\" stroke-width=\"1\"")
    if (l.dashed) out.append(" stroke-dasharray=\"4 3\"")
    out.append("/>")
  }

  /** A measuring line with end ticks across it and the label beside its middle. */
  private fun dimension(d: DiagramElement.Dimension, ppi: Double, out: StringBuilder) {
    val x1 = d.x1 * ppi
    val y1 = d.y1 * ppi
    val x2 = d.x2 * ppi
    val y2 = d.y2 * ppi
    val horizontal = abs(x2 - x1) >= abs(y2 - y1)
    fun seg(ax: Double, ay: Double, bx: Double, by: Double) =
      out
        .append("<line x1=\"${num(ax)}\" y1=\"${num(ay)}\" x2=\"${num(bx)}\" y2=\"${num(by)}\"")
        .append(" stroke=\"$STROKE\" stroke-width=\"1\"/>")
    seg(x1, y1, x2, y2)
    if (horizontal) {
      seg(x1, y1 - TICK_PX, x1, y1 + TICK_PX)
      seg(x2, y2 - TICK_PX, x2, y2 + TICK_PX)
    } else {
      seg(x1 - TICK_PX, y1, x1 + TICK_PX, y1)
      seg(x2 - TICK_PX, y2, x2 + TICK_PX, y2)
    }
    val mx = (x1 + x2) / 2
    val my = (y1 + y2) / 2
    val (tx, ty, anchor) =
      if (horizontal) Triple(mx, my - DIMENSION_GAP_PX, "middle")
      else Triple(mx + DIMENSION_GAP_PX * 2, my, "start")
    out.append("<text x=\"${num(tx)}\" y=\"${num(ty)}\" text-anchor=\"$anchor\" fill=\"$STROKE\">")
    out.append(escape(d.label)).append("</text>")
  }

  private fun hatchId(fill: Rgb?): String = "hatch-" + (fill?.let { hex(it) } ?: "none")

  private fun colour(fill: Rgb?): String = fill?.let { "#" + hex(it) } ?: "none"

  private fun hex(rgb: Rgb): String =
    (rgb.argb and RGB_MASK).toString(HEX_RADIX).padStart(HEX_DIGITS, '0').uppercase()

  private fun escape(s: String): String =
    s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

  /**
   * Two decimals at most, written the same on every platform (a Double's `toString` differs between
   * the JVM and Wasm).
   */
  private fun num(v: Double): String {
    val hundredths = (v * HUNDREDTHS).roundToLong()
    val sign = if (hundredths < 0) "-" else ""
    val a = abs(hundredths)
    val whole = a / HUNDREDTHS
    val frac = a % HUNDREDTHS
    return when {
      frac == 0L -> "$sign$whole"
      frac % 10 == 0L -> "$sign$whole.${frac / 10}"
      else -> "$sign$whole.${frac.toString().padStart(2, '0')}"
    }
  }
}
