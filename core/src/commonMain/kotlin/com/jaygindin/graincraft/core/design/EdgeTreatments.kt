package com.jaygindin.graincraft.core.design

import com.jaygindin.graincraft.core.units.Length
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

@Serializable
data class EdgeTreatments(
  val cornerRadius: Length = Length.ZERO,
  val edgeProfile: EdgeProfile = EdgeProfile.NONE,
  val edgeProfileSize: Length = Length.ZERO,
  val juiceGroove: JuiceGroove? = null,
  val fingerGrips: Boolean = false,
  val feet: Boolean = false,
)
