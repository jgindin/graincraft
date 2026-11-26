package com.jaygindin.graincraft.core.fabrication

import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.Nominal
import com.jaygindin.graincraft.core.units.boardFeet
import kotlin.math.floor

private const val BOARD_FOOT_DIVISOR = 144.0
private const val TENTHS = 10
private val QUARTER_INCH = Length.inches(0, 1, 4)

/** Guards half-up rounding against products like 0.35 x 10 landing just under 3.5. */
private const val ROUNDING_EPSILON = 1e-9

/**
 * `count` identical milled parts of `width × height × length`. Each is cut from `stock` planed to
 * `fromThickness` and ripped to `ripWidth`. A null `stock` means both cross-section dimensions are
 * thicker than any stock in the profile: laminate the part or buy thicker stock. Such a part has
 * `fromThickness` as the smaller dimension and `ripWidth` as the other, and is priced as rough
 * stock `fromThickness` thick rounded up to the next quarter inch.
 */
data class CutPart(
  val speciesId: String,
  val count: Int,
  val width: Length,
  val height: Length,
  val length: Length,
  val stock: Nominal?,
  val fromThickness: Length,
  val ripWidth: Length,
  val laminated: Boolean,
  val panelNames: List<String>,
)

/** Board feet to buy for one species, milling waste included. */
data class SpeciesLumber(val speciesId: String, val boardFeet: Double)

/** `total` is the unrounded sum of `bySpecies`. */
data class Lumber(val bySpecies: List<SpeciesLumber>, val total: Double)

/**
 * Every milled strip of every panel, with identical parts merged in order of first appearance.
 *
 * An edge-grain strip's width is the stock thickness, so a strip wider than the thickest surfaced
 * stock is laminated from two halves, the first taking the odd nanometre, as the layout draws it.
 * Every other part is cut in whichever orientation needs fewer board feet, ties going to the
 * thinner stock, so thin strips are ripped from thick stock rather than planed down from it.
 */
fun cutList(fabrication: Fabrication, profile: ShopProfile): List<CutPart> {
  val thickest = profile.surfacedThickness.values.maxOrNull() ?: Length.ZERO
  val merged = LinkedHashMap<PartKey, CutPart>()
  for (panel in fabrication.panels) {
    for (strip in panel.strips) {
      for (part in partsOf(panel, strip, thickest, profile)) {
        val key = PartKey(part.speciesId, part.width, part.height, part.length, part.laminated)
        val existing = merged[key]
        merged[key] =
          if (existing == null) part
          else
            existing.copy(
              count = existing.count + part.count,
              panelNames = (existing.panelNames + part.panelNames).distinct(),
            )
      }
    }
  }
  return merged.values.toList()
}

/** Board feet per species in order of first appearance, with the profile's milling waste. */
fun lumber(parts: List<CutPart>, profile: ShopProfile): Lumber {
  val bySpecies = LinkedHashMap<String, Double>()
  for (part in parts) {
    val bf = part.count * partBoardFeet(part, profile.kerf) * (1 + profile.millingWaste)
    bySpecies[part.speciesId] = (bySpecies[part.speciesId] ?: 0.0) + bf
  }
  val list = bySpecies.map { (id, bf) -> SpeciesLumber(id, bf) }
  return Lumber(list, list.sumOf { it.boardFeet })
}

/** One decimal, rounded half up: `"5.5 BF"`. */
fun formatBoardFeet(bf: Double): String {
  val tenths = floor(bf * TENTHS + 0.5 + ROUNDING_EPSILON).toLong()
  return "${tenths / TENTHS}.${tenths % TENTHS} BF"
}

private data class PartKey(
  val speciesId: String,
  val width: Length,
  val height: Length,
  val length: Length,
  val laminated: Boolean,
)

private fun partsOf(
  panel: Panel,
  strip: PanelStrip,
  thickest: Length,
  profile: ShopProfile,
): List<CutPart> {
  val height = panel.millHeight
  fun part(width: Length, stock: Nominal?, from: Length, rip: Length, laminated: Boolean) =
    CutPart(
      strip.speciesId,
      1,
      width,
      height,
      panel.length,
      stock,
      from,
      rip,
      laminated,
      listOf(panel.name),
    )

  val width = strip.millWidth
  if (panel.kind == PanelKind.EDGE_GRAIN_BOARD) {
    val laminated = width > thickest
    val first = width / 2
    val widths = if (laminated) listOf(first, width - first) else listOf(width)
    return widths.map { part(it, thinnestStock(it, profile), it, height, laminated) }
  }
  val choice = cheapestOrientation(width, height, profile)
  val part =
    if (choice != null) part(width, choice.stock, choice.from, choice.rip, false)
    else part(width, null, minOf(width, height), maxOf(width, height), false)
  return listOf(part)
}

/** Null when neither dimension fits any stock in the profile. */
private fun cheapestOrientation(width: Length, height: Length, profile: ShopProfile): Orientation? =
  listOfNotNull(
      thinnestStock(width, profile)?.let { Orientation(it, width, height) },
      thinnestStock(height, profile)?.let { Orientation(it, height, width) },
    )
    .minWithOrNull(compareBy<Orientation> { it.cost(profile.kerf) }.thenBy { it.stock.quarters })

private class Orientation(val stock: Nominal, val from: Length, val rip: Length) {
  /** Proportional to board feet, in exact integers so ties are exact. */
  fun cost(kerf: Length): Long = stock.quarters * (rip + kerf).nanometres
}

private fun thinnestStock(thickness: Length, profile: ShopProfile): Nominal? =
  profile.surfacedThickness.entries
    .filter { it.value >= thickness }
    .minByOrNull { it.key.quarters }
    ?.key

/**
 * A part with no stock is priced as rough stock `fromThickness` thick, rounded up to the next
 * quarter inch, since lumber is sold in quarters.
 */
private fun partBoardFeet(part: CutPart, kerf: Length): Double {
  val rip = part.ripWidth + kerf
  val stock = part.stock
  return if (stock != null) boardFeet(stock, rip, part.length)
  else
    roundUpToQuarter(part.fromThickness).inches * rip.inches * part.length.inches /
      BOARD_FOOT_DIVISOR
}

private fun roundUpToQuarter(l: Length): Length {
  val q = QUARTER_INCH.nanometres
  return Length((l.nanometres + q - 1) / q * q)
}
