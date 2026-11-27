package com.jaygindin.graincraft.app.ui.theme

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Semantic colours for the woodworking ratings, each with the colour for text drawn on it. */
data class RatingColors(
  val recommended: Color,
  val onRecommended: Color,
  val caution: Color,
  val onCaution: Color,
  val notSuitable: Color,
  val onNotSuitable: Color,
)

internal val LightRatingColors =
  RatingColors(
    recommended = Color(0xFF2E6B34),
    onRecommended = Color(0xFFFFFFFF),
    caution = Color(0xFF8A5300),
    onCaution = Color(0xFFFFFFFF),
    notSuitable = Color(0xFFA12A1F),
    onNotSuitable = Color(0xFFFFFFFF),
  )

internal val DarkRatingColors =
  RatingColors(
    recommended = Color(0xFF8FD694),
    onRecommended = Color(0xFF0D3A14),
    caution = Color(0xFFF2C060),
    onCaution = Color(0xFF3D2800),
    notSuitable = Color(0xFFFF9E92),
    onNotSuitable = Color(0xFF4A0E08),
  )

/** The rating colours of the current theme. */
val LocalRatingColors: ProvidableCompositionLocal<RatingColors> = staticCompositionLocalOf {
  LightRatingColors
}
