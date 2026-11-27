package com.jaygindin.graincraft.core.design

import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.min
import kotlinx.serialization.Serializable

enum class EdgeProfile {
  NONE,
  CHAMFER,
  ROUNDOVER,
}

enum class FlatteningMethod {
  DRUM_SANDER,
  ROUTER_SLED,
  BELT_AND_ORBITAL,
}

/** A groove routed around the board; `inset` is its distance from the edge. */
@Serializable data class JuiceGroove(val width: Length, val depth: Length, val inset: Length)

/** The narrowest board side this groove fits on: its inner edge must leave wood inside it. */
fun JuiceGroove.minimumSide(): Length = (inset + width) * 2

/** True when the groove's inner edge leaves wood inside it on a [length] × [width] board. */
fun JuiceGroove.fitsOn(length: Length, width: Length): Boolean = minimumSide() < min(length, width)

@Serializable
data class EdgeTreatments(
  val cornerRadius: Length = Length.ZERO,
  val edgeProfile: EdgeProfile = EdgeProfile.NONE,
  val edgeProfileSize: Length = Length.ZERO,
  val juiceGroove: JuiceGroove? = null,
  val fingerGrips: Boolean = false,
  val feet: Boolean = false,
)
