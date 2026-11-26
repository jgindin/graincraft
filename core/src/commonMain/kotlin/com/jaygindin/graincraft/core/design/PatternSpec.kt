package com.jaygindin.graincraft.core.design

import com.jaygindin.graincraft.core.units.Length
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A strip of one species; `width` is the finished, visible width. */
@Serializable data class Strip(val speciesId: String, val width: Length)

@Serializable
sealed interface PatternSpec {
  /** `rowHeight` is the height of one row of an end-grain board and is unused for edge grain. */
  @Serializable
  @SerialName("stripes")
  data class Stripes(
    val strips: List<Strip>,
    val rowHeight: Length = Length.inches(1, 1, 2),
  ) : PatternSpec

  @Serializable
  @SerialName("checkerboard")
  data class Checkerboard(val speciesA: String, val speciesB: String, val targetCell: Length) :
    PatternSpec

  @Serializable
  @SerialName("brick")
  data class Brick(
    val brickSpecies: String,
    val mortarSpecies: String,
    val targetBrickWidth: Length,
    val targetBrickHeight: Length,
    val mortarWidth: Length,
  ) : PatternSpec
}
