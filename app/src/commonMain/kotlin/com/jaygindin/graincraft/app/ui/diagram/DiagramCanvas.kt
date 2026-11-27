package com.jaygindin.graincraft.app.ui.diagram

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jaygindin.graincraft.core.guide.Diagram
import com.jaygindin.graincraft.core.guide.DiagramElement
import com.jaygindin.graincraft.core.wood.Rgb
import kotlin.math.abs
import kotlin.math.max

/**
 * Labels are laid out for this many inches of height, as the SVG of the same diagram draws them.
 */
private const val LABEL_INCHES = 11.0 / 24.0
/** At this scale a label is about 9 dp tall, the smallest that still reads. */
private const val MIN_DP_PER_INCH = 20f
private const val HATCH_SPACING_PX = 6f
private const val TICK_PX = 4f
private const val LABEL_GAP_PX = 3f
private const val DASH_ON_PX = 4f
private const val DASH_OFF_PX = 3f

/** Wood fills are data colours, so a label on one picks dark or light ink by its luminance. */
private const val LIGHT_FILL_LUMINANCE = 0.18
private val INK_ON_LIGHT = Color(0xFF2A211A)
private val INK_ON_DARK = Color(0xFFF7F1E8)

/**
 * A build-guide diagram, scaled to fit the width it is given, with its height following the
 * diagram's proportions. Where fitting would shrink its labels past reading, it is drawn at the
 * smallest readable scale instead and scrolls sideways. Shapes are filled with the species'
 * colours; strokes and loose text use the theme's ink so the diagram reads in light and dark.
 */
@Composable
fun DiagramCanvas(diagram: Diagram, modifier: Modifier = Modifier) {
  val measurer = rememberTextMeasurer()
  val ink = MaterialTheme.colorScheme.onSurface
  val labelStyle = MaterialTheme.typography.labelSmall
  if (diagram.width <= 0.0 || diagram.height <= 0.0) return
  BoxWithConstraints(modifier.fillMaxWidth()) {
    val perInch = max(maxWidth.value / diagram.width.toFloat(), MIN_DP_PER_INCH).dp
    val width = perInch * diagram.width.toFloat()
    val height = perInch * diagram.height.toFloat()
    Box(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
      Canvas(Modifier.size(width, height).testTag("diagram-canvas")) {
        val scale = (size.width / diagram.width).toFloat()
        // Labels scale with the drawing, as in the SVG, so they keep to the spaces the diagram
        // leaves for them; a larger drawing stops growing them at the theme's label size.
        val labelPx = (LABEL_INCHES * scale).toFloat()
        val fontSize: TextUnit = minOf(labelPx.toSp().value, labelStyle.fontSize.value).sp
        val style = labelStyle.copy(fontSize = fontSize)
        val painter = DiagramPainter(this, measurer, style, ink, scale)
        diagram.elements.forEach(painter::draw)
      }
    }
  }
}

private class DiagramPainter(
  private val scope: DrawScope,
  private val measurer: TextMeasurer,
  private val style: TextStyle,
  private val ink: Color,
  private val scale: Float,
) {
  private fun px(inches: Double): Float = (inches * scale).toFloat()

  fun draw(element: DiagramElement) {
    when (element) {
      is DiagramElement.Box -> box(element)
      is DiagramElement.Line -> line(element)
      is DiagramElement.Text -> text(element)
      is DiagramElement.Dimension -> dimension(element)
    }
  }

  private fun box(b: DiagramElement.Box) =
    with(scope) {
      val topLeft = Offset(px(b.x), px(b.y))
      val size = Size(px(b.w), px(b.h))
      b.fill?.let { drawRect(Color(it.argb), topLeft, size) }
      if (b.hatched) hatch(topLeft, size, labelInk(b.fill).copy(alpha = HATCH_ALPHA))
      drawRect(ink, topLeft, size, style = Stroke(width = 1f))
      val label = b.label ?: return@with
      val layout = measurer.measure(label, style.copy(color = labelInk(b.fill)))
      drawText(
        layout,
        topLeft =
          Offset(
            topLeft.x + (size.width - layout.size.width) / 2f,
            topLeft.y + (size.height - layout.size.height) / 2f,
          ),
      )
    }

  /** Diagonal lines across the box, as the SVG's hatch pattern marks a rotated slice. */
  private fun hatch(topLeft: Offset, size: Size, colour: Color) =
    with(scope) {
      clipRect(topLeft.x, topLeft.y, topLeft.x + size.width, topLeft.y + size.height) {
        var d = -size.height
        while (d < size.width) {
          drawLine(
            colour,
            Offset(topLeft.x + d, topLeft.y + size.height),
            Offset(topLeft.x + d + size.height, topLeft.y),
            strokeWidth = HATCH_STROKE_PX,
          )
          d += HATCH_SPACING_PX
        }
      }
    }

  private fun line(l: DiagramElement.Line) =
    scope.drawLine(
      ink,
      Offset(px(l.x1), px(l.y1)),
      Offset(px(l.x2), px(l.y2)),
      strokeWidth = 1f,
      pathEffect =
        if (l.dashed) PathEffect.dashPathEffect(floatArrayOf(DASH_ON_PX, DASH_OFF_PX)) else null,
    )

  /** Loose text sits on its baseline at (x, y), as in SVG. */
  private fun text(t: DiagramElement.Text) =
    with(scope) {
      val layout = measurer.measure(t.text, style.copy(color = ink))
      drawText(layout, topLeft = Offset(px(t.x), px(t.y) - layout.firstBaseline))
    }

  /** A measuring line with ticks across its ends and its label beside the middle. */
  private fun dimension(d: DiagramElement.Dimension) =
    with(scope) {
      val a = Offset(px(d.x1), px(d.y1))
      val b = Offset(px(d.x2), px(d.y2))
      val horizontal = abs(b.x - a.x) >= abs(b.y - a.y)
      drawLine(ink, a, b, strokeWidth = 1f)
      val tick = if (horizontal) Offset(0f, TICK_PX) else Offset(TICK_PX, 0f)
      drawLine(ink, a - tick, a + tick, strokeWidth = 1f)
      drawLine(ink, b - tick, b + tick, strokeWidth = 1f)
      val layout = measurer.measure(d.label, style.copy(color = ink))
      val mid = Offset((a.x + b.x) / 2f, (a.y + b.y) / 2f)
      val at =
        if (horizontal) {
          Offset(mid.x - layout.size.width / 2f, mid.y - LABEL_GAP_PX - layout.size.height)
        } else {
          Offset(mid.x + TICK_PX + LABEL_GAP_PX, mid.y - layout.size.height / 2f)
        }
      drawText(layout, topLeft = at)
    }

  private fun labelInk(fill: Rgb?): Color =
    when {
      fill == null -> ink
      fill.luminance > LIGHT_FILL_LUMINANCE -> INK_ON_LIGHT
      else -> INK_ON_DARK
    }

  private companion object {
    const val HATCH_ALPHA = 0.55f
    const val HATCH_STROKE_PX = 1.2f
  }
}
