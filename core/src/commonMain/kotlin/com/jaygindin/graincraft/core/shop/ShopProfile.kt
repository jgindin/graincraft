package com.jaygindin.graincraft.core.shop

import com.jaygindin.graincraft.core.design.FlatteningMethod
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.Nominal
import kotlinx.serialization.Serializable

private val EIGHTH = Length.inches(0, 1, 8)
private val SIXTEENTH = Length.inches(0, 1, 16)
private val PLANER_WIDTH = Length.inches(13)
private val CROSSCUT_WIDTH = Length.inches(12)
private val END_TRIM = Length.inches(2)
private const val DEFAULT_MILLING_WASTE = 0.30

private val FOUR_QUARTER_SURFACED = Length.inches(0, 13, 16)
private val FIVE_QUARTER_SURFACED = Length.inches(1, 1, 16)
private val SIX_QUARTER_SURFACED = Length.inches(1, 5, 16)
private val EIGHT_QUARTER_SURFACED = Length.inches(1, 3, 4)

/** The user's shop tools and stock; the build math reads every allowance from here. */
@Serializable
data class ShopProfile(
  val kerf: Length = EIGHTH,
  /** Null means no planer. */
  val planerMaxWidth: Length? = PLANER_WIDTH,
  val drumSanderMaxWidth: Length? = null,
  /** Sled or miter saw capacity. */
  val crosscutMaxWidth: Length = CROSSCUT_WIDTH,
  val endGrainFlattening: FlatteningMethod = FlatteningMethod.ROUTER_SLED,
  /** Removed from a long-grain panel's height when planed. */
  val panelFlattenAllowance: Length = SIXTEENTH,
  val endGrainFlattenAllowance: Map<FlatteningMethod, Length> =
    mapOf(
      FlatteningMethod.DRUM_SANDER to SIXTEENTH,
      FlatteningMethod.ROUTER_SLED to EIGHTH,
      FlatteningMethod.BELT_AND_ORBITAL to SIXTEENTH,
    ),
  val edgeGrainFlattenAllowance: Length = SIXTEENTH,
  /** Total length added to strips for squaring and snipe. */
  val endTrim: Length = END_TRIM,
  /** Removed when squaring the glued board's sides. */
  val edgeTrimPerSide: Length = EIGHTH,
  /** Extra rough lumber for defects and milling, as a fraction. */
  val millingWaste: Double = DEFAULT_MILLING_WASTE,
  val surfacedThickness: Map<Nominal, Length> =
    mapOf(
      Nominal.FOUR_QUARTER to FOUR_QUARTER_SURFACED,
      Nominal.FIVE_QUARTER to FIVE_QUARTER_SURFACED,
      Nominal.SIX_QUARTER to SIX_QUARTER_SURFACED,
      Nominal.EIGHT_QUARTER to EIGHT_QUARTER_SURFACED,
    ),
  val glue: String = "Titebond III",
)
