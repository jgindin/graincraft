package com.jaygindin.graincraft.app.ui.theme

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/** Maple-cream surfaces, walnut-brown actions, and a darker brown for text. */
internal val GrainCraftLightColorScheme: ColorScheme =
  lightColorScheme(
    background = Color(0xFFFFF8EE),
    onBackground = Color(0xFF2B1D12),
    surface = Color(0xFFFFF8EE),
    onSurface = Color(0xFF2B1D12),
    surfaceVariant = Color(0xFFEFE2CF),
    onSurfaceVariant = Color(0xFF5A4636),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBF2E4),
    surfaceContainer = Color(0xFFF6EBDA),
    surfaceContainerHigh = Color(0xFFF0E3CF),
    surfaceContainerHighest = Color(0xFFEADBC5),
    surfaceTint = Color(0xFF7A4A22),
    inverseSurface = Color(0xFF3F2E21),
    inverseOnSurface = Color(0xFFFBEFDD),
    inversePrimary = Color(0xFFE0B07A),
    outline = Color(0xFF8A7358),
    outlineVariant = Color(0xFFD6C4AA),
    scrim = Color(0xFF000000),
    primary = Color(0xFF7A4A22),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF6B3F1D),
    onPrimaryContainer = Color(0xFFFFF3E0),
    secondary = Color(0xFF6E5A45),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEBD9BF),
    onSecondaryContainer = Color(0xFF2B1D12),
    tertiary = Color(0xFF55633A),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD8E5B8),
    onTertiaryContainer = Color(0xFF1B2808),
    error = Color(0xFFA12A1F),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD3),
    onErrorContainer = Color(0xFF410A05),
  )

/** Deep-brown surfaces with warm cream text and a light caramel primary. */
internal val GrainCraftDarkColorScheme: ColorScheme =
  darkColorScheme(
    background = Color(0xFF1E1510),
    onBackground = Color(0xFFF1E4D3),
    surface = Color(0xFF1E1510),
    onSurface = Color(0xFFF1E4D3),
    surfaceVariant = Color(0xFF3A2D22),
    onSurfaceVariant = Color(0xFFD3C0A8),
    surfaceContainerLowest = Color(0xFF17100B),
    surfaceContainerLow = Color(0xFF241A13),
    surfaceContainer = Color(0xFF2A1F17),
    surfaceContainerHigh = Color(0xFF352819),
    surfaceContainerHighest = Color(0xFF403224),
    surfaceTint = Color(0xFFE0B07A),
    inverseSurface = Color(0xFFF1E4D3),
    inverseOnSurface = Color(0xFF2B1D12),
    inversePrimary = Color(0xFF7A4A22),
    outline = Color(0xFF9C8568),
    outlineVariant = Color(0xFF4F4030),
    scrim = Color(0xFF000000),
    primary = Color(0xFFE0B07A),
    onPrimary = Color(0xFF3A210B),
    primaryContainer = Color(0xFF8A5A2B),
    onPrimaryContainer = Color(0xFFFFF3E0),
    secondary = Color(0xFFD3C0A8),
    onSecondary = Color(0xFF362A1C),
    secondaryContainer = Color(0xFF4F4030),
    onSecondaryContainer = Color(0xFFF1E4D3),
    tertiary = Color(0xFFBFCF9B),
    onTertiary = Color(0xFF2A3813),
    tertiaryContainer = Color(0xFF3F4C28),
    onTertiaryContainer = Color(0xFFD8E5B8),
    error = Color(0xFFFFB4A8),
    onError = Color(0xFF561008),
    errorContainer = Color(0xFF7A2418),
    onErrorContainer = Color(0xFFFFDAD3),
  )

@Composable
fun GrainCraftTheme(dark: Boolean, content: @Composable () -> Unit) {
  val scheme = if (dark) GrainCraftDarkColorScheme else GrainCraftLightColorScheme
  MaterialTheme(
    colorScheme = scheme,
    typography = grainCraftTypography(),
    shapes = GrainCraftShapes,
  ) {
    CompositionLocalProvider(
      LocalRatingColors provides if (dark) DarkRatingColors else LightRatingColors,
      LocalContentColor provides MaterialTheme.colorScheme.onSurface,
      content = content,
    )
  }
}

/** A main action: walnut brown with cream text. */
@Composable
internal fun primaryButtonColors(): ButtonColors =
  ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
  )

/** A quiet action: the tonal tan. */
@Composable
internal fun quietButtonColors(): ButtonColors =
  ButtonDefaults.filledTonalButtonColors(
    containerColor = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
  )
