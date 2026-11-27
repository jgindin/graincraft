package com.jaygindin.graincraft.app.data

import com.jaygindin.graincraft.app.ui.theme.ThemeChoice
import com.jaygindin.graincraft.core.units.FractionPrecision
import com.jaygindin.graincraft.core.units.LengthUnit
import kotlinx.serialization.Serializable

/** Where the desktop window was last placed, in screen pixels. */
@Serializable data class WindowPlacement(val x: Int, val y: Int, val width: Int, val height: Int)

/**
 * The user's app-wide choices; every field has a default so a new field never breaks an old file.
 */
@Serializable
data class AppSettings(
  val units: LengthUnit = LengthUnit.INCH,
  val precision: FractionPrecision = FractionPrecision.SIXTEENTH,
  val theme: ThemeChoice = ThemeChoice.SYSTEM,
  val window: WindowPlacement? = null,
)
