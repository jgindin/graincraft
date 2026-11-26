package com.jaygindin.graincraft.core.fabrication

import com.jaygindin.graincraft.core.design.FlatteningMethod
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.min

/** What flattens a long-grain panel. */
enum class FlatteningTool {
  PLANER,
  DRUM_SANDER,
  ROUTER_SLED,
  BELT_AND_ORBITAL,
}

/** `widthLimit` is null for tools with no width limit; `allowance` is what the tool removes. */
data class LongGrainFlattening(
  val tool: FlatteningTool,
  val widthLimit: Length?,
  val allowance: Length,
)

/**
 * Picks the first set machine (planer, then drum sander) that fits `panelWidth`. If none fits but
 * one is set, the widest machine is used and the panel gets split. With no machine, the profile's
 * end-grain method applies, with its own allowance.
 */
fun longGrainFlattening(panelWidth: Length, profile: ShopProfile): LongGrainFlattening {
  val machines =
    listOfNotNull(
      profile.planerMaxWidth?.let { FlatteningTool.PLANER to it },
      profile.drumSanderMaxWidth?.let { FlatteningTool.DRUM_SANDER to it },
    )
  val chosen =
    machines.firstOrNull { (_, limit) -> panelWidth <= limit }
      ?: machines.reduceOrNull { best, next -> if (next.second > best.second) next else best }
  if (chosen != null) {
    return LongGrainFlattening(chosen.first, chosen.second, profile.panelFlattenAllowance)
  }
  val method = profile.endGrainFlattening
  val tool =
    when (method) {
      FlatteningMethod.DRUM_SANDER -> FlatteningTool.DRUM_SANDER
      FlatteningMethod.ROUTER_SLED -> FlatteningTool.ROUTER_SLED
      FlatteningMethod.BELT_AND_ORBITAL -> FlatteningTool.BELT_AND_ORBITAL
    }
  val allowance = profile.endGrainFlattenAllowance[method] ?: profile.panelFlattenAllowance
  return LongGrainFlattening(tool, null, allowance)
}

/**
 * The widest sub-panel allowed, or null for no limit. End-grain panels are also crosscut, so the
 * crosscut capacity caps them.
 */
fun splitLimit(
  flattening: LongGrainFlattening,
  endGrainPanel: Boolean,
  profile: ShopProfile,
): Length? {
  val machine = flattening.widthLimit
  if (!endGrainPanel) return machine
  return if (machine == null) profile.crosscutMaxWidth else min(machine, profile.crosscutMaxWidth)
}
