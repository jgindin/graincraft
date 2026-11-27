package com.jaygindin.graincraft.app.ui.preview

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.jaygindin.graincraft.core.layout.BoardLayout
import com.jaygindin.graincraft.core.wood.Woods

private val MISSING_WOOD = Color.Gray

/**
 * Draws the board as seen from above, each piece in its species' raw base colour, scaled to fit the
 * drawing area and centred in it.
 */
fun DrawScope.drawFlatBoard(layout: BoardLayout, woods: Woods) {
  val length = layout.size.length.nanometres.toFloat()
  val width = layout.size.width.nanometres.toFloat()
  if (length <= 0f || width <= 0f) return
  val scale = minOf(size.width / length, size.height / width)
  val origin = Offset((size.width - length * scale) / 2f, (size.height - width * scale) / 2f)
  for (piece in layout.pieces) {
    val colour = woods.byId(piece.speciesId)?.raw?.base?.argb?.let(::Color) ?: MISSING_WOOD
    val path =
      Path().apply {
        piece.polygon.forEachIndexed { i, p ->
          val x = origin.x + p.x.nanometres * scale
          val y = origin.y + p.y.nanometres * scale
          if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
      }
    drawPath(path, colour)
  }
}
