package com.jaygindin.graincraft.app.ui.preview

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import com.jaygindin.graincraft.app.platform.SkiaArgbImageFactory
import com.jaygindin.graincraft.core.layout.Fibre
import com.jaygindin.graincraft.core.texture.GrainTexture
import com.jaygindin.graincraft.core.texture.TextureSpec
import com.jaygindin.graincraft.core.wood.WoodCatalog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class TextureCacheTest {
  private class CountingFactory : ArgbImageFactory {
    var created = 0

    override fun create(argb: IntArray, width: Int, height: Int): ImageBitmap {
      created++
      return SkiaArgbImageFactory.create(argb, width, height)
    }
  }

  private val walnut = WoodCatalog.byId("black-walnut")!!

  private fun spec(seed: Int = 1, ppi: Float = 32f) =
    TextureSpec(walnut.raw, walnut.poreType, Fibre.Z, seed, mirrored = false, pixelsPerInch = ppi)

  @Test
  fun theSameSpecAndSizeReturnTheSameBitmap() {
    val factory = CountingFactory()
    val cache = TextureCache(factory)
    val first = cache.get(spec(), 48, 48)
    val second = cache.get(spec(), 48, 48)
    assertSame(first, second)
    assertEquals(1, factory.created)
    assertNotSame(first, cache.get(spec(), 64, 48))
    assertNotSame(first, cache.get(spec(seed = 2), 48, 48))
    assertNotSame(first, cache.get(spec().copy(mirrored = true), 48, 48))
    assertEquals(4, factory.created)
  }

  @Test
  fun theBitmapHoldsTheTexturePixels() {
    val cache = TextureCache(SkiaArgbImageFactory)
    val image = cache.get(spec(), 20, 10)
    val expected = GrainTexture.render(spec(), 20, 10)
    val pixels = image.toPixelMap()
    assertEquals(20, image.width)
    assertEquals(10, image.height)
    for (y in 0 until 10) {
      for (x in 0 until 20) assertEquals(expected[y * 20 + x], pixels[x, y].toArgb(), "$x,$y")
    }
  }

  @Test
  fun evictsTheLeastRecentlyUsedPastMaxEntries() {
    val factory = CountingFactory()
    val cache = TextureCache(factory, maxEntries = 2)
    val a = cache.get(spec(seed = 1), 8, 8)
    cache.get(spec(seed = 2), 8, 8)
    // Using `a` again makes seed 2 the least recently used, so seed 3 pushes it out.
    assertSame(a, cache.get(spec(seed = 1), 8, 8))
    cache.get(spec(seed = 3), 8, 8)
    assertEquals(3, factory.created)
    assertSame(a, cache.get(spec(seed = 1), 8, 8))
    assertEquals(3, factory.created)
    cache.get(spec(seed = 2), 8, 8)
    assertEquals(4, factory.created)
  }

  @Test
  fun pixelDensityIsBucketedToAPowerOfTwoBetween16And256() {
    assertEquals(16f, bucketPixelsPerInch(1f))
    assertEquals(16f, bucketPixelsPerInch(16f))
    assertEquals(32f, bucketPixelsPerInch(16.5f))
    assertEquals(64f, bucketPixelsPerInch(50f))
    assertEquals(64f, bucketPixelsPerInch(64f))
    assertEquals(256f, bucketPixelsPerInch(200f))
    assertEquals(256f, bucketPixelsPerInch(5000f))
  }
}
