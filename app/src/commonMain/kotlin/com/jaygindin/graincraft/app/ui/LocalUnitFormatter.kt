package com.jaygindin.graincraft.app.ui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import com.jaygindin.graincraft.core.units.FractionPrecision
import com.jaygindin.graincraft.core.units.LengthUnit
import com.jaygindin.graincraft.core.units.UnitFormatter

/**
 * The formatter for the user's chosen units, provided by the app shell; every length a screen shows
 * goes through it.
 */
val LocalUnitFormatter: ProvidableCompositionLocal<UnitFormatter> = staticCompositionLocalOf {
  UnitFormatter(LengthUnit.INCH, FractionPrecision.SIXTEENTH)
}
