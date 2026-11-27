package com.jaygindin.graincraft.core.fabrication

import com.jaygindin.graincraft.core.units.Length

/** Why a design cannot be turned into a build plan. */
sealed interface CompileError {
  data class OutOfRange(val field: String, val min: Length, val max: Length) : CompileError

  data object PatternNotAllowedInEdgeGrain : CompileError

  data object NoStrips : CompileError

  data class NonPositiveWidth(val stripIndex: Int) : CompileError

  /** A strip, as drawn or once fitted to the board's width, is outside the buildable range. */
  data class StripOutOfRange(val stripIndex: Int, val min: Length, val max: Length) : CompileError

  data class NonPositiveDimension(val field: String) : CompileError

  data class UnknownSpecies(val id: String) : CompileError

  data class NoValidSplit(val panel: String, val limit: Length) : CompileError
}
