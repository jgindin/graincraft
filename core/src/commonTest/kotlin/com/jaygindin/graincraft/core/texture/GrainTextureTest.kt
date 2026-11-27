package com.jaygindin.graincraft.core.texture

import com.jaygindin.graincraft.core.layout.Fibre
import com.jaygindin.graincraft.core.wood.WoodCatalog
import com.jaygindin.graincraft.core.wood.WoodSpecies
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GrainTextureTest {
  private fun species(id: String): WoodSpecies = WoodCatalog.byId(id)!!

  private fun spec(
    id: String = "black-walnut",
    fibre: Fibre = Fibre.Z,
    seed: Int = textureSeed(id, 0, 0),
    mirrored: Boolean = false,
  ): TextureSpec {
    val wood = species(id)
    return TextureSpec(wood.raw, wood.poreType, fibre, seed, mirrored, 32f)
  }

  private fun channel(argb: Int, shift: Int): Int = (argb shr shift) and 0xFF

  private fun distance(a: Int, b: Int): Double {
    var sum = 0.0
    for (shift in intArrayOf(16, 8, 0)) {
      val d = (channel(a, shift) - channel(b, shift)).toDouble()
      sum += d * d
    }
    return sum
  }

  private fun meanColour(pixels: IntArray): Int {
    var r = 0L
    var g = 0L
    var b = 0L
    for (p in pixels) {
      r += channel(p, 16)
      g += channel(p, 8)
      b += channel(p, 0)
    }
    val n = pixels.size
    return (0xFF shl 24) or ((r / n).toInt() shl 16) or ((g / n).toInt() shl 8) or (b / n).toInt()
  }

  private fun grey(argb: Int): Int = channel(argb, 16) * 3 + channel(argb, 8) * 6 + channel(argb, 0)

  private fun meanDiffs(pixels: IntArray, size: Int): Pair<Double, Double> {
    var across = 0.0
    var along = 0.0
    var count = 0
    for (y in 0 until size - 1) {
      for (x in 0 until size - 1) {
        val here = grey(pixels[y * size + x])
        across += abs(here - grey(pixels[y * size + x + 1]))
        along += abs(here - grey(pixels[(y + 1) * size + x]))
        count++
      }
    }
    return across / count to along / count
  }

  @Test
  fun deterministic() {
    val a = GrainTexture.render(spec(), 64, 48)
    val b = GrainTexture.render(spec(), 64, 48)
    assertEquals(64 * 48, a.size)
    assertContentEquals(a, b)
    assertTrue(a.all { (it ushr 24) == 0xFF })
  }

  @Test
  fun mirroredIsTheMirrorImage() {
    for (fibre in Fibre.entries) {
      val plain = GrainTexture.render(spec(fibre = fibre), 50, 30)
      val flipped = GrainTexture.render(spec(fibre = fibre, mirrored = true), 50, 30)
      for (y in 0 until 30) {
        for (x in 0 until 50) {
          assertEquals(plain[y * 50 + x], flipped[y * 50 + (49 - x)])
        }
      }
    }
  }

  @Test
  fun longGrainVariesAcrossNotAlong() {
    val (horizontal, vertical) = meanDiffs(GrainTexture.render(spec(fibre = Fibre.X), 64, 64), 64)
    assertTrue(horizontal < vertical / 2, "horizontal $horizontal vertical $vertical")
  }

  @Test
  fun endGrainVariesBothWays() {
    for (id in listOf("black-walnut", "hard-maple", "white-oak")) {
      for (strip in 0 until 6) {
        val (h, v) =
          meanDiffs(GrainTexture.render(spec(id, seed = textureSeed(id, strip, 0)), 64, 64), 64)
        assertTrue(h < v * 3 && v < h * 3, "$id strip $strip horizontal $h vertical $v")
      }
    }
  }

  @Test
  fun usesTheSpeciesTones() {
    val ids = listOf("black-walnut", "hard-maple", "african-padauk")
    for (fibre in Fibre.entries) {
      for (id in ids) {
        val mean = meanColour(GrainTexture.render(spec(id, fibre), 64, 64))
        val own = distance(mean, species(id).raw.base.argb)
        for (other in ids.filter { it != id }) {
          assertTrue(own < distance(mean, species(other).raw.base.argb), "$id $fibre vs $other")
        }
      }
    }
  }

  @Test
  fun differentStripsDiffer() {
    val a = GrainTexture.render(spec(seed = textureSeed("black-walnut", 0, 0)), 64, 64)
    val b = GrainTexture.render(spec(seed = textureSeed("black-walnut", 1, 0)), 64, 64)
    assertFalse(a.contentEquals(b))
  }

  @Test
  fun seedIsStableAndSensitive() {
    assertEquals(textureSeed("hard-maple", 2, 3), textureSeed("hard-maple", 2, 3))
    val seeds =
      setOf(
        textureSeed("hard-maple", 2, 3),
        textureSeed("hard-maple", 3, 2),
        textureSeed("black-walnut", 2, 3),
        textureSeed("hard-maple", 2, 4),
      )
    assertEquals(4, seeds.size)
  }
}
