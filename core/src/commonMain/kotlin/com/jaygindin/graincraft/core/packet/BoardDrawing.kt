package com.jaygindin.graincraft.core.packet

import com.jaygindin.graincraft.core.layout.BoardLayout
import com.jaygindin.graincraft.core.layout.Piece
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.UnitFormatter
import com.jaygindin.graincraft.core.wood.Rgb
import kotlin.math.abs
import kotlin.math.roundToInt

private const val DRAWING_WIDTH_PX = 480.0
private const val DRAWING_MARGIN_PX = 36.0
private const val LABEL_GAP_PX = 10.0
private const val OUTLINE = "#333333"
private const val HUNDREDTHS = 100
private const val RGB_MASK = 0xFFFFFF
private const val HEX_DIGITS = 6
private const val HEX_RADIX = 16

/**
 * A flat top view of the board in the colour `fill` gives each species, with the length and width
 * marked. There is one `<rect>` per layout piece; rounded corners clip the group through a path, so
 * the rect count stays equal to the piece count.
 */
internal fun boardDrawingSvg(
  layout: BoardLayout,
  cornerRadius: Length,
  formatter: UnitFormatter,
  fill: (String) -> String,
): String {
  val ppi = DRAWING_WIDTH_PX / layout.size.length.inches
  val w = layout.size.length.inches * ppi
  val h = layout.size.width.inches * ppi
  val m = DRAWING_MARGIN_PX
  val clip = cornerRadius.inches * ppi
  val totalW = n(w + 2 * m)
  val totalH = n(h + 2 * m)
  return buildString {
    append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"$totalW\" height=\"$totalH\" ")
    append("viewBox=\"0 0 $totalW $totalH\" font-family=\"sans-serif\" font-size=\"12\">\n")
    if (clip > 0) append(roundedClip(w, h, clip))
    append("<g transform=\"translate(${n(m)} ${n(m)})\"")
    if (clip > 0) append(" clip-path=\"url(#board-clip)\"")
    append(">\n")
    for (piece in layout.pieces) append(pieceRect(piece, ppi, fill(piece.speciesId)))
    append("</g>\n")
    val length = escapeHtml(formatter.format(layout.size.length))
    val width = escapeHtml(formatter.format(layout.size.width))
    append("<text x=\"${n(m + w / 2)}\" y=\"${n(m - LABEL_GAP_PX)}\" text-anchor=\"middle\">")
    append("$length</text>\n")
    append("<text x=\"${n(m + w + LABEL_GAP_PX)}\" y=\"${n(m + h / 2)}\">$width</text>\n")
    append("</svg>")
  }
}

/** The board outline with rounded corners, in the clipped group's own coordinates. */
private fun roundedClip(w: Double, h: Double, r: Double): String {
  val rr = minOf(r, w / 2, h / 2)
  val a = "A${n(rr)} ${n(rr)} 0 0 1 "
  val d =
    "M${n(rr)} 0H${n(w - rr)}$a${n(w)} ${n(rr)}V${n(h - rr)}$a${n(w - rr)} ${n(h)}" +
      "H${n(rr)}$a 0 ${n(h - rr)}V${n(rr)}$a${n(rr)} 0Z"
  return "<defs><clipPath id=\"board-clip\"><path d=\"$d\"/></clipPath></defs>\n"
}

private fun pieceRect(piece: Piece, ppi: Double, fill: String): String {
  val x0 = piece.polygon.minOf { it.x.inches } * ppi
  val x1 = piece.polygon.maxOf { it.x.inches } * ppi
  val y0 = piece.polygon.minOf { it.y.inches } * ppi
  val y1 = piece.polygon.maxOf { it.y.inches } * ppi
  return "<rect x=\"${n(x0)}\" y=\"${n(y0)}\" width=\"${n(x1 - x0)}\" height=\"${n(y1 - y0)}\" " +
    "fill=\"$fill\" stroke=\"$OUTLINE\" stroke-width=\"0.5\"/>\n"
}

internal fun hex(rgb: Rgb): String =
  "#" + (rgb.argb and RGB_MASK).toString(HEX_RADIX).padStart(HEX_DIGITS, '0').uppercase()

/** Two decimals at most, written the same on every platform. */
private fun n(v: Double): String {
  val hundredths = (v * HUNDREDTHS).roundToInt()
  val sign = if (hundredths < 0) "-" else ""
  val a = abs(hundredths)
  val frac = a % HUNDREDTHS
  val whole = a / HUNDREDTHS
  return if (frac == 0) "$sign$whole" else "$sign$whole.${frac.toString().padStart(2, '0')}"
}
