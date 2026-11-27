package com.jaygindin.graincraft.app.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import com.jaygindin.graincraft.app.ui.preview.ArgbImageFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

/**
 * Hands ARGB pixels to Skia in one copy. An ARGB int written little-endian is the byte sequence B,
 * G, R, A, which is Skia's BGRA_8888.
 */
object SkiaArgbImageFactory : ArgbImageFactory {
  private const val BYTES_PER_PIXEL = 4

  override fun create(argb: IntArray, width: Int, height: Int): ImageBitmap {
    val bytes = ByteArray(argb.size * BYTES_PER_PIXEL)
    ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(argb)
    val info = ImageInfo(width, height, ColorType.BGRA_8888, ColorAlphaType.OPAQUE)
    val bitmap = Bitmap()
    bitmap.allocPixels(info)
    bitmap.installPixels(info, bytes, width * BYTES_PER_PIXEL)
    bitmap.setImmutable()
    return bitmap.asComposeImageBitmap()
  }
}
