package com.jaygindin.graincraft.app.ui.preview

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/**
 * How the board maps onto the preview: a board point in inches lands at `offset + point * scale` on
 * screen. [fitScale] is the scale that fits the board, and zoom stays between [MIN_ZOOM] and
 * [MAX_ZOOM] times it.
 */
data class PreviewTransform(
  val scale: Float,
  val offset: Offset,
  val fitScale: Float = scale,
) {
  /** The zoom relative to the fitted view. */
  val zoom: Float
    get() = scale / fitScale

  /** Zooms by [factor], within the limits, keeping the board point under [focus] where it is. */
  fun zoomAt(factor: Float, focus: Offset): PreviewTransform {
    val newScale = (scale * factor).coerceIn(fitScale * MIN_ZOOM, fitScale * MAX_ZOOM)
    val applied = newScale / scale
    return copy(scale = newScale, offset = focus - (focus - offset) * applied)
  }

  fun pan(by: Offset): PreviewTransform = copy(offset = offset + by)

  fun toScreen(board: Offset): Offset = offset + board * scale

  fun toBoard(screen: Offset): Offset = (screen - offset) / scale

  companion object {
    const val MIN_ZOOM = 0.25f
    const val MAX_ZOOM = 8f

    /** The share of the viewport left empty on each side of a fitted board. */
    const val MARGIN = 0.05f

    /** Centres a [board] (in inches) in [viewport], as large as fits inside the margin. */
    fun fit(board: Size, viewport: Size): PreviewTransform {
      if (board.width <= 0f || board.height <= 0f) return PreviewTransform(1f, Offset.Zero)
      val usable = 1f - 2f * MARGIN
      val scale =
        minOf(viewport.width * usable / board.width, viewport.height * usable / board.height)
          .coerceAtLeast(Float.MIN_VALUE)
      val offset =
        Offset(
          (viewport.width - board.width * scale) / 2f,
          (viewport.height - board.height * scale) / 2f,
        )
      return PreviewTransform(scale, offset)
    }
  }
}
