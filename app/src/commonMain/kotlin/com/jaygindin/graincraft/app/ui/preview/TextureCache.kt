package com.jaygindin.graincraft.app.ui.preview

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import com.jaygindin.graincraft.core.texture.GrainTexture
import com.jaygindin.graincraft.core.texture.TextureSpec

/**
 * Turns ARGB pixels (row-major, as [GrainTexture] makes them) into an [ImageBitmap]. Common Compose
 * has no fast way to do this, so each platform supplies one.
 */
fun interface ArgbImageFactory {
  fun create(argb: IntArray, width: Int, height: Int): ImageBitmap
}

/** The platform's [ArgbImageFactory], provided by the app shell. */
val LocalArgbImageFactory: ProvidableCompositionLocal<ArgbImageFactory> = staticCompositionLocalOf {
  error("No ArgbImageFactory is provided")
}

private const val MIN_PPI = 16
private const val MAX_PPI = 256

/**
 * The texture density for a view drawn at [pixelsPerInch]: the next power of two, from 16 to 256,
 * so zooming re-renders a texture only when the density doubles or halves.
 */
fun bucketPixelsPerInch(pixelsPerInch: Float): Float {
  var bucket = MIN_PPI
  while (bucket < pixelsPerInch && bucket < MAX_PPI) bucket *= 2
  return bucket.toFloat()
}

/** Rendered grain textures, keyed by spec and size, keeping the [maxEntries] most recently used. */
class TextureCache(private val images: ArgbImageFactory, private val maxEntries: Int = 256) {
  private data class Key(val spec: TextureSpec, val width: Int, val height: Int)

  // A LinkedHashMap keeps insertion order, so re-inserting on every hit keeps it in LRU order.
  private val entries = LinkedHashMap<Key, ImageBitmap>()

  fun get(spec: TextureSpec, widthPx: Int, heightPx: Int): ImageBitmap {
    val key = Key(spec, widthPx, heightPx)
    val hit = entries.remove(key)
    if (hit != null) {
      entries[key] = hit
      return hit
    }
    val image = images.create(GrainTexture.render(spec, widthPx, heightPx), widthPx, heightPx)
    entries[key] = image
    while (entries.size > maxEntries) entries.remove(entries.keys.first())
    return image
  }
}
