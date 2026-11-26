package com.jaygindin.graincraft.core.design

import com.jaygindin.graincraft.core.units.Length

/** Ready-made strip lists; the user edits the result freely. */
object StripePresets {
  private val FIBONACCI = listOf(1, 1, 2, 3, 5)

  /** One strip per species, in order. */
  fun uniform(speciesIds: List<String>, width: Length): List<Strip> = speciesIds.map {
    Strip(it, width)
  }

  /** `count` strips cycling through the species. */
  fun alternating(speciesIds: List<String>, count: Int, width: Length): List<Strip> {
    require(speciesIds.isNotEmpty()) { "at least one species" }
    return List(count) { Strip(speciesIds[it % speciesIds.size], width) }
  }

  /** ABC becomes ABCBA. */
  fun mirror(strips: List<Strip>): List<Strip> = strips + strips.dropLast(1).asReversed()

  /** Widths 1, 1, 2, 3, 5 units mirrored, with the two species alternating from `a`. */
  fun fibonacci(a: String, b: String, unit: Length): List<Strip> =
    mirror(FIBONACCI.map { Strip(a, unit * it) }).mapIndexed { i, s ->
      s.copy(speciesId = if (i % 2 == 0) a else b)
    }

  /** An accent strip between each pair of neighbours. */
  fun pinstripe(strips: List<Strip>, accentId: String, accentWidth: Length): List<Strip> =
    strips.flatMapIndexed { i, s ->
      if (i == 0) listOf(s) else listOf(Strip(accentId, accentWidth), s)
    }
}
