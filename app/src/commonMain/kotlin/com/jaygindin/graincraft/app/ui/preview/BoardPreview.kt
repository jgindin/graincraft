package com.jaygindin.graincraft.app.ui.preview

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import com.jaygindin.graincraft.app.ui.LocalUnitFormatter
import com.jaygindin.graincraft.app.ui.editor.Finish
import com.jaygindin.graincraft.app.ui.theme.Spacing
import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.design.EdgeTreatments
import com.jaygindin.graincraft.core.layout.BoardLayout
import com.jaygindin.graincraft.core.units.UnitFormatter
import com.jaygindin.graincraft.core.wood.Woods
import graincraft.app.generated.resources.Res
import graincraft.app.generated.resources.ic_fit_screen
import graincraft.app.generated.resources.ic_zoom_in
import graincraft.app.generated.resources.ic_zoom_out
import kotlin.math.pow
import org.jetbrains.compose.resources.painterResource

/** The theme colours the preview's chrome uses; the board itself is coloured by its woods. */
@Immutable
data class BoardPreviewColors(
  val background: Color,
  val outline: Color,
  val groove: Color,
  val shadow: Color,
  val highlight: Color,
)

object BoardPreviewDefaults {
  @Composable
  fun colors(): BoardPreviewColors =
    BoardPreviewColors(
      background = MaterialTheme.colorScheme.surfaceContainerHigh,
      outline = MaterialTheme.colorScheme.outline,
      groove = MaterialTheme.colorScheme.outline,
      shadow = MaterialTheme.colorScheme.scrim,
      highlight = MaterialTheme.colorScheme.surfaceContainerLowest,
    )
}

private const val BUTTON_ZOOM = 1.5f
private const val WHEEL_ZOOM = 1.2f

/**
 * The finished board as it will look, each piece textured with its species' grain in [finish], with
 * rounded corners and a juice groove from [edges]. Zoom with the scroll wheel or the buttons, pan
 * by dragging, and hover a piece to see where it came from. With [showDimensions], the actual size
 * shows under the board, and [target] beside it when they differ.
 */
@OptIn(ExperimentalMaterial3Api::class) // TooltipBox is still experimental in Material 3
@Composable
fun BoardPreview(
  layout: BoardLayout,
  woods: Woods,
  finish: Finish,
  edges: EdgeTreatments,
  target: Dimensions,
  showDimensions: Boolean,
  modifier: Modifier = Modifier,
  colors: BoardPreviewColors = BoardPreviewDefaults.colors(),
) {
  val images = LocalArgbImageFactory.current
  val cache = remember(images) { TextureCache(images) }
  val pieces = remember(layout, woods, finish) { prepareBoard(layout, woods, finish) }
  val board = Size(layout.size.length.inches.toFloat(), layout.size.width.inches.toFloat())
  val view = remember(board) { PreviewView(board) }
  var pointer by remember { mutableStateOf<Offset?>(null) }
  val hovered = pointer?.let { p -> pieces.firstOrNull { it.contains(view.transform.toBoard(p)) } }
  val tooltip = rememberTooltipState(isPersistent = true)
  LaunchedEffect(hovered) { if (hovered != null) tooltip.show() else tooltip.dismiss() }

  Box(modifier.background(colors.background).onSizeChanged { view.viewport = it }) {
    TooltipBox(
      positionProvider = remember(pointer) { BesidePointer(pointer ?: Offset.Zero) },
      tooltip = {
        hovered?.let {
          PlainTooltip {
            Text(
              it.piece.label(woods),
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.testTag("preview-tooltip"),
            )
          }
        }
      },
      state = tooltip,
      focusable = false,
      enableUserInput = false,
    ) {
      Canvas(
        Modifier.fillMaxSize()
          .testTag("preview-canvas")
          .pointerInput(view) {
            awaitPointerEventScope {
              while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.first()
                when (event.type) {
                  PointerEventType.Scroll -> {
                    view.zoomAt(WHEEL_ZOOM.pow(-change.scrollDelta.y), change.position)
                    change.consume()
                  }
                  PointerEventType.Exit -> pointer = null
                  else -> pointer = change.position
                }
              }
            }
          }
          .pointerInput(view) { detectDragGestures { _, drag -> view.pan(drag) } },
      ) {
        drawBoard(pieces, board, edges, view.transform, cache, colors, hovered)
      }
    }
    ControlsBand(
      view,
      if (showDimensions) layout.size to target else null,
      Modifier.align(Alignment.BottomCenter),
    )
  }
}

/**
 * The zoom and pan state. Until the user zooms or pans, the board stays fitted, so it follows the
 * viewport as the window is resized.
 */
private class PreviewView(val board: Size) {
  var viewport by mutableStateOf(IntSize.Zero)

  /** The height of the bottom band of controls, which the fitted board keeps clear of. */
  var reservedBottom by mutableStateOf(0)
  private var moved by mutableStateOf<PreviewTransform?>(null)

  private val usable: Size
    get() =
      Size(
        viewport.width.toFloat(),
        (viewport.height - reservedBottom).coerceAtLeast(0).toFloat(),
      )

  private val fitted: PreviewTransform
    get() = PreviewTransform.fit(board, usable)

  val transform: PreviewTransform
    get() = moved ?: fitted

  private val centre: Offset
    get() = Offset(usable.width / 2f, usable.height / 2f)

  fun zoomAt(factor: Float, focus: Offset = centre) {
    // The fitted scale follows the viewport, so the zoom limits do too.
    val current = transform.copy(fitScale = fitted.fitScale)
    moved = current.zoomAt(factor, focus)
  }

  fun pan(by: Offset) {
    moved = transform.pan(by)
  }

  fun fit() {
    moved = null
  }
}

/**
 * The band along the bottom with the zoom pill and, when [dimensions] (actual to target) is given,
 * the size label. It reports its height so the fitted board stays above it.
 */
@Composable
private fun ControlsBand(
  view: PreviewView,
  dimensions: Pair<Dimensions, Dimensions>?,
  modifier: Modifier,
) {
  Box(
    modifier.fillMaxWidth().onSizeChanged { view.reservedBottom = it.height }.padding(Spacing.md),
  ) {
    dimensions?.let { (actual, target) ->
      DimensionsLabel(actual, target, Modifier.align(Alignment.Center))
    }
    ZoomButtons(view, Modifier.align(Alignment.CenterEnd))
  }
}

@Composable
private fun ZoomButtons(view: PreviewView, modifier: Modifier) {
  // A floating pill, so the controls read as sitting above the board when it reaches under them.
  Surface(
    modifier,
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    shadowElevation = 2.dp,
  ) {
    Row(
      Modifier.padding(Spacing.xs),
      horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
      IconButton(
        onClick = { view.zoomAt(1f / BUTTON_ZOOM) },
        modifier = Modifier.testTag("preview-zoom-out"),
      ) {
        Icon(painterResource(Res.drawable.ic_zoom_out), contentDescription = "Zoom out")
      }
      IconButton(
        onClick = { view.zoomAt(BUTTON_ZOOM) },
        modifier = Modifier.testTag("preview-zoom-in"),
      ) {
        Icon(painterResource(Res.drawable.ic_zoom_in), contentDescription = "Zoom in")
      }
      IconButton(onClick = view::fit, modifier = Modifier.testTag("preview-fit")) {
        Icon(painterResource(Res.drawable.ic_fit_screen), contentDescription = "Fit to view")
      }
    }
  }
}

/** The board's size, with the target in brackets when the fit changed it. */
@Composable
private fun DimensionsLabel(actual: Dimensions, target: Dimensions, modifier: Modifier) {
  val formatter = LocalUnitFormatter.current
  val text =
    if (actual == target) formatter.size(actual)
    else "${formatter.size(actual)} (target ${formatter.size(target)})"
  Surface(
    modifier,
    shape = MaterialTheme.shapes.small,
    color = MaterialTheme.colorScheme.surfaceContainerLowest,
    shadowElevation = 1.dp,
  ) {
    Text(
      text,
      style = MaterialTheme.typography.labelLarge,
      modifier =
        Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)
          .testTag("preview-dimensions"),
    )
  }
}

private fun UnitFormatter.size(d: Dimensions): String =
  "${format(d.length)} × ${format(d.width)} × ${format(d.thickness)}"

/** Puts a tooltip just below and to the right of the pointer, flipping where it would not fit. */
private class BesidePointer(private val pointer: Offset) : PopupPositionProvider {
  override fun calculatePosition(
    anchorBounds: IntRect,
    windowSize: IntSize,
    layoutDirection: LayoutDirection,
    popupContentSize: IntSize,
  ): IntOffset {
    val at = IntOffset(anchorBounds.left + pointer.x.toInt(), anchorBounds.top + pointer.y.toInt())
    val x =
      if (at.x + GAP + popupContentSize.width <= windowSize.width) at.x + GAP
      else at.x - GAP - popupContentSize.width
    val y =
      if (at.y + GAP + popupContentSize.height <= windowSize.height) at.y + GAP
      else at.y - GAP - popupContentSize.height
    return IntOffset(x.coerceAtLeast(0), y.coerceAtLeast(0))
  }

  private companion object {
    const val GAP = 16
  }
}
