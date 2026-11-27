package com.jaygindin.graincraft.app.ui.preview

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals

class PreviewTransformTest {
  private val board = Size(18f, 12f)
  private val viewport = Size(1000f, 700f)
  private val fitted = PreviewTransform.fit(board, viewport)

  private fun assertNear(expected: Float, actual: Float) =
    assertEquals(expected, actual, absoluteTolerance = 1e-3f)

  @Test
  fun fitLeavesAFivePercentMarginOnTheTighterSideAndCentres() {
    // 90% of 1000 across 18" is 50 px/in; 90% of 700 across 12" would be 52.5, so width wins.
    assertNear(50f, fitted.scale)
    assertNear(50f, fitted.offset.x)
    assertNear(50f, fitted.offset.y)
  }

  @Test
  fun fitOnATallViewportIsLimitedByTheHeight() {
    val t = PreviewTransform.fit(board, Size(1000f, 400f))
    assertNear(30f, t.scale)
    assertNear((1000f - 18f * 30f) / 2f, t.offset.x)
    assertNear(20f, t.offset.y)
  }

  @Test
  fun zoomAtKeepsTheFocusPointStill() {
    val focus = Offset(300f, 200f)
    val before = fitted.toBoard(focus)
    val zoomed = fitted.zoomAt(2f, focus)
    assertNear(100f, zoomed.scale)
    val after = zoomed.toBoard(focus)
    assertNear(before.x, after.x)
    assertNear(before.y, after.y)
    assertNear(focus.x, zoomed.toScreen(before).x)
    assertNear(focus.y, zoomed.toScreen(before).y)
  }

  @Test
  fun zoomClampsBetweenAQuarterAndEightTimesTheFit() {
    val focus = Offset(500f, 350f)
    assertNear(8f, fitted.zoomAt(100f, focus).zoom)
    assertNear(0.25f, fitted.zoomAt(0.001f, focus).zoom)
    // Zooming in and out in several steps still stops at the limits.
    var t = fitted
    repeat(40) { t = t.zoomAt(1.5f, focus) }
    assertNear(8f, t.zoom)
    assertNear(400f, t.scale)
    // At the limit, the focus point still does not move.
    val p = t.toBoard(focus)
    val again = t.zoomAt(1.5f, focus)
    assertNear(p.x, again.toBoard(focus).x)
  }

  @Test
  fun panMovesTheOffset() {
    val moved = fitted.pan(Offset(10f, -5f))
    assertEquals(Offset(60f, 45f), moved.offset)
    assertNear(fitted.scale, moved.scale)
  }
}
