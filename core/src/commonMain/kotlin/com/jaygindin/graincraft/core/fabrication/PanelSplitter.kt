package com.jaygindin.graincraft.core.fabrication

import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.max
import com.jaygindin.graincraft.core.units.min
import com.jaygindin.graincraft.core.units.sum

private class Candidate(val groups: List<IntRange>, val widest: Length, val narrowest: Length) {
  fun beats(other: Candidate?): Boolean =
    other == null ||
      widest < other.widest ||
      (widest == other.widest && narrowest > other.narrowest)
}

private class Splitter(private val widths: List<Length>, private val limit: Length) {
  private val n = widths.size

  /** The fewest groups that cover strips `i` until the end; `minGroups[n]` is 0. */
  private val minGroups = IntArray(n + 1).also { fillMinGroups(it) }
  private var best: Candidate? = null

  private fun fillMinGroups(out: IntArray) {
    for (i in n - 1 downTo 0) {
      var sum = Length.ZERO
      var fewest = Int.MAX_VALUE
      var j = i
      while (j < n && sum + widths[j] <= limit) {
        sum += widths[j]
        fewest = minOf(fewest, out[j + 1] + 1)
        j++
      }
      out[i] = fewest
    }
  }

  fun solve(): List<IntRange> {
    walk(0, minGroups[0], emptyList(), Length.ZERO, limit)
    return best?.groups.orEmpty()
  }

  private fun walk(
    start: Int,
    left: Int,
    groups: List<IntRange>,
    widest: Length,
    narrowest: Length,
  ) {
    if (start == n) {
      // Boundaries are visited leftmost first, so only a strictly better score replaces.
      val c = Candidate(groups, widest, narrowest)
      if (c.beats(best)) best = c
      return
    }
    var sum = Length.ZERO
    var end = start
    while (left > 0 && end < n && sum + widths[end] <= limit) {
      sum += widths[end]
      if (minGroups[end + 1] <= left - 1) {
        walk(end + 1, left - 1, groups + listOf(start..end), max(sum, widest), min(sum, narrowest))
      }
      end++
    }
  }
}

/**
 * Splits strips into contiguous groups no wider than `limit`. Picks the fewest groups, then the
 * smallest widest group, then the widest narrowest group, then the leftmost boundaries (compared in
 * order). Null when a single strip exceeds the limit; a null limit gives one group.
 */
fun splitPanel(stripWidths: List<Length>, limit: Length?): List<IntRange>? =
  when {
    stripWidths.isEmpty() -> emptyList()
    limit == null || stripWidths.sum() <= limit -> listOf(stripWidths.indices)
    stripWidths.any { it > limit } -> null
    else -> Splitter(stripWidths, limit).solve()
  }

/** `ceil(width / limit)` equal parts that sum to `width`; the first parts take the leftover. */
fun splitEvenly(width: Length, limit: Length?): List<Length> {
  if (limit == null || width <= limit) return listOf(width)
  val n = ((width.nanometres + limit.nanometres - 1) / limit.nanometres).toInt()
  val base = width.nanometres / n
  val extra = (width.nanometres - base * n).toInt()
  return List(n) { i -> Length(if (i < extra) base + 1 else base) }
}

/** `("1", 2)` gives `1A`, `1B`; a single part keeps the bare name. */
fun subPanelNames(base: String, count: Int): List<String> =
  if (count == 1) listOf(base) else List(count) { i -> base + ('A' + i) }
