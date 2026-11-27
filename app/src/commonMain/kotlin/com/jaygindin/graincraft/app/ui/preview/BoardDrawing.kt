package com.jaygindin.graincraft.app.ui.preview

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntSize
import com.jaygindin.graincraft.app.ui.editor.Finish
import com.jaygindin.graincraft.core.design.EdgeTreatments
import com.jaygindin.graincraft.core.design.JuiceGroove
import com.jaygindin.graincraft.core.design.fitsOn
import com.jaygindin.graincraft.core.layout.BoardLayout
import com.jaygindin.graincraft.core.layout.Fibre
import com.jaygindin.graincraft.core.layout.Piece
import com.jaygindin.graincraft.core.texture.TextureSpec
import com.jaygindin.graincraft.core.texture.textureSeed
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.wood.Woods
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

private val MISSING_WOOD = Color.Gray

/** No texture side is rendered longer than this, so a long strip at high zoom stays affordable. */
private const val MAX_TEXTURE_SIDE = 2048f
private const val MIN_TEXTURE_PPI = 16f

/** How dark the gaps between pieces read: as glue lines, not as the background showing through. */
private const val GLUE_ALPHA = 0.45f
private const val GLUE_LINE_ALPHA = 0.18f
private const val GLUE_LINE_WIDTH = 0.6f
private const val HOVER_FILL_ALPHA = 0.16f
private const val HOVER_STROKE_ALPHA = 0.8f
private const val GROOVE_FILL_ALPHA = 0.4f
private const val GROOVE_EDGE_ALPHA = 1f
private const val GROOVE_LIGHT_ALPHA = 0.35f
private const val GROOVE_EDGE_WIDTH = 1.5f
private const val SHADOW_LAYERS = 6
private const val SHADOW_ALPHA = 0.035f
private const val SHADOW_DROP = 1.2f
private const val OUTLINE_WIDTH = 1f

/** Swaps x and y; it is its own inverse, so row-major and column-major readings agree. */
private val TRANSPOSE =
  Matrix().apply {
    this[0, 0] = 0f
    this[1, 1] = 0f
    this[0, 1] = 1f
    this[1, 0] = 1f
  }

/**
 * A piece ready to draw, in board inches. [frame] is the rectangle its texture covers: for end
 * grain, the same size for every cell of a strip (its largest cell on the board, since the layout
 * keeps only what survives the trim), so every slice of a strip shows the same rings, and a cell
 * trimmed more than the others shows part of the same frame.
 *
 * End-grain textures run along the slice: texture x is board y, so mirroring a texture turns the
 * cell end for end across the board, as rotating the slice does.
 */
internal class PreparedPiece(
  val piece: Piece,
  val polygon: List<Offset>,
  val frame: Rect,
  val spec: TextureSpec?,
  val transposed: Boolean,
) {
  fun contains(p: Offset): Boolean {
    var inside = false
    var j = polygon.lastIndex
    for (i in polygon.indices) {
      val a = polygon[i]
      val b = polygon[j]
      if ((a.y > p.y) != (b.y > p.y) && p.x < (b.x - a.x) * (p.y - a.y) / (b.y - a.y) + a.x) {
        inside = !inside
      }
      j = i
    }
    return inside
  }
}

private data class StripKey(val speciesId: String, val sourceStrip: Int, val lamination: Int)

private fun Piece.bounds(): Rect {
  val xs = polygon.map { it.x.inches.toFloat() }
  val ys = polygon.map { it.y.inches.toFloat() }
  return Rect(xs.min(), ys.min(), xs.max(), ys.max())
}

/** Works out each piece's polygon, texture frame and texture spec once per layout and finish. */
internal fun prepareBoard(layout: BoardLayout, woods: Woods, finish: Finish): List<PreparedPiece> {
  val bounds = layout.pieces.map { it.bounds() }
  // The largest cell of each strip is its whole cross-section; trimmed cells are smaller.
  val sections = mutableMapOf<StripKey, Size>()
  layout.pieces.forEachIndexed { i, piece ->
    if (piece.fibre == Fibre.Z) {
      val key = StripKey(piece.speciesId, piece.sourceStripIndex, piece.laminationIndex)
      val b = bounds[i]
      val seen = sections[key] ?: Size.Zero
      sections[key] = Size(max(seen.width, b.width), max(seen.height, b.height))
    }
  }
  return layout.pieces.mapIndexed { i, piece ->
    val b = bounds[i]
    val wood = woods.byId(piece.speciesId)
    val endGrain = piece.fibre == Fibre.Z
    val frame =
      if (endGrain) {
        val section =
          sections.getValue(
            StripKey(piece.speciesId, piece.sourceStripIndex, piece.laminationIndex),
          )
        // A cell trimmed by the board's near edge lost its near side, so its frame hangs off it.
        val left = if (b.left <= 0f && b.width < section.width) b.right - section.width else b.left
        val top = if (b.top <= 0f && b.height < section.height) b.bottom - section.height else b.top
        Rect(Offset(left, top), section)
      } else {
        b
      }
    val spec = wood?.let {
      TextureSpec(
        tones = if (finish == Finish.OILED) it.oiled else it.raw,
        poreType = it.poreType,
        fibre = piece.fibre,
        seed = textureSeed(piece.speciesId, piece.sourceStripIndex, piece.laminationIndex),
        mirrored = piece.flipped,
        pixelsPerInch = 0f,
      )
    }
    PreparedPiece(
      piece = piece,
      polygon = piece.polygon.map { Offset(it.x.inches.toFloat(), it.y.inches.toFloat()) },
      frame = frame,
      spec = spec,
      transposed = endGrain,
    )
  }
}

/** The species, the strip and the slice a piece came from, for the hover tooltip. */
internal fun Piece.label(woods: Woods): String {
  val name = woods.byId(speciesId)?.name ?: speciesId
  val source = if (sourceStripIndex >= 0) "strip ${sourceStripIndex + 1}" else "mortar"
  return listOfNotNull(name, source, sliceIndex?.let { "slice ${it + 1}" }).joinToString(" · ")
}

private fun PreviewTransform.path(points: List<Offset>): Path =
  Path().apply {
    points.forEachIndexed { i, p ->
      val s = toScreen(p)
      if (i == 0) moveTo(s.x, s.y) else lineTo(s.x, s.y)
    }
    close()
  }

private fun Float.toLength(): Length = Length.inchesApprox(toDouble())

/**
 * A corner radius larger than the rect allows, or a rect with no area, is clamped, never thrown.
 */
private fun roundRect(rect: Rect, radius: Float): RoundRect {
  val r = radius.coerceIn(0f, max(0f, min(rect.width, rect.height) / 2f))
  return RoundRect(rect, CornerRadius(r))
}

/** Draws the board: a soft shadow, the textured pieces, the juice groove and the outline. */
internal fun DrawScope.drawBoard(
  pieces: List<PreparedPiece>,
  board: Size,
  edges: EdgeTreatments,
  transform: PreviewTransform,
  cache: TextureCache,
  colors: BoardPreviewColors,
  hovered: PreparedPiece?,
) {
  if (board.width <= 0f || board.height <= 0f) return
  val scale = transform.scale
  val rect =
    Rect(transform.toScreen(Offset.Zero), transform.toScreen(Offset(board.width, board.height)))
  val shape = roundRect(rect, edges.cornerRadius.inches.toFloat() * scale)
  val outlinePx = OUTLINE_WIDTH * density

  for (i in 1..SHADOW_LAYERS) {
    val spread = i * density
    val drop = SHADOW_DROP * i * density
    drawPath(
      Path().apply {
        addRoundRect(
          roundRect(rect.inflate(spread).translate(0f, drop), shape.topLeftCornerRadius.x + spread),
        )
      },
      colors.shadow.copy(alpha = SHADOW_ALPHA),
    )
  }

  val boardPath = Path().apply { addRoundRect(shape) }
  clipPath(boardPath) {
    drawRect(colors.shadow.copy(alpha = GLUE_ALPHA), rect.topLeft, rect.size)
    val ppi = bucketPixelsPerInch(scale)
    val visible = Rect(Offset.Zero, size)
    // Only pieces on screen need their textures rendered, which matters when zoomed far in.
    for (p in pieces) {
      val bounds =
        Rect(transform.toScreen(p.frame.topLeft), transform.toScreen(p.frame.bottomRight))
      if (bounds.overlaps(visible)) drawPiece(p, transform, ppi, cache)
    }
    for (p in pieces) {
      drawPath(
        transform.path(p.polygon),
        colors.shadow.copy(alpha = GLUE_LINE_ALPHA),
        style = Stroke(GLUE_LINE_WIDTH * density),
      )
    }
    hovered?.let {
      val path = transform.path(it.polygon)
      drawPath(path, colors.highlight.copy(alpha = HOVER_FILL_ALPHA))
      drawPath(path, colors.highlight.copy(alpha = HOVER_STROKE_ALPHA), style = Stroke(2 * density))
    }
    // The live edges can be ahead of the last compiled board, so the fit is checked here too;
    // the compiler's warning tells the user why the groove is missing.
    edges.juiceGroove
      ?.takeIf { it.fitsOn(board.width.toLength(), board.height.toLength()) }
      ?.let { drawGroove(it, rect, shape, scale, colors) }
  }
  drawPath(boardPath, colors.outline, style = Stroke(outlinePx))
}

/**
 * The groove darkens the wood under it (multiplied, so the grain still shows through), with a
 * shadowed outer wall and a lit inner one, as a routed cove looks under light from the viewer.
 */
private fun DrawScope.drawGroove(
  groove: JuiceGroove,
  rect: Rect,
  board: RoundRect,
  scale: Float,
  colors: BoardPreviewColors,
) {
  val width = groove.width.inches.toFloat() * scale
  val inset = groove.inset.inches.toFloat() * scale + width / 2f
  val centre =
    roundRect(rect.deflate(inset), board.topLeftCornerRadius.x - inset).coerceRadius(width)
  val radius = centre.topLeftCornerRadius.x
  fun edge(offset: Float) =
    Path().apply { addRoundRect(roundRect(rect.deflate(inset + offset), radius - offset)) }
  drawPath(
    Path().apply { addRoundRect(centre) },
    colors.groove,
    alpha = GROOVE_FILL_ALPHA,
    style = Stroke(width),
    blendMode = BlendMode.Multiply,
  )
  val line = Stroke(GROOVE_EDGE_WIDTH * density)
  drawPath(
    edge(-width / 2f),
    colors.groove,
    alpha = GROOVE_EDGE_ALPHA,
    style = line,
    blendMode = BlendMode.Multiply,
  )
  drawPath(edge(width / 2f), colors.highlight, alpha = GROOVE_LIGHT_ALPHA, style = line)
}

/**
 * A groove's centre line keeps at least half its width as radius, so its inner edge stays round.
 */
private fun RoundRect.coerceRadius(width: Float): RoundRect =
  roundRect(Rect(left, top, right, bottom), max(topLeftCornerRadius.x, width / 2f))

private fun DrawScope.drawPiece(
  p: PreparedPiece,
  transform: PreviewTransform,
  viewPpi: Float,
  cache: TextureCache,
) {
  val path = transform.path(p.polygon)
  val spec = p.spec
  if (spec == null) {
    drawPath(path, MISSING_WOOD)
    return
  }
  // Texture axes: x along the long grain, or along the slice (board y) for end grain.
  val texWidthIn = if (p.transposed) p.frame.height else p.frame.width
  val texHeightIn = if (p.transposed) p.frame.width else p.frame.height
  var ppi = viewPpi
  while (max(texWidthIn, texHeightIn) * ppi > MAX_TEXTURE_SIDE && ppi > MIN_TEXTURE_PPI) ppi /= 2f
  val w = ceil(texWidthIn * ppi).toInt().coerceAtLeast(1)
  val h = ceil(texHeightIn * ppi).toInt().coerceAtLeast(1)
  val image = cache.get(spec.copy(pixelsPerInch = ppi), w, h)
  val origin = transform.toScreen(p.frame.topLeft)
  val k = transform.scale / ppi
  clipPath(path) {
    withTransform(
      {
        translate(origin.x, origin.y)
        if (p.transposed) transform(TRANSPOSE)
        scale(k, k, pivot = Offset.Zero)
      },
    ) {
      drawImage(
        image,
        dstSize = IntSize(image.width, image.height),
        filterQuality = FilterQuality.High,
      )
    }
  }
}
