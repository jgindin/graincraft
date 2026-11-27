package com.jaygindin.graincraft.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import graincraft.app.generated.resources.Res
import graincraft.app.generated.resources.noto_sans_bold
import graincraft.app.generated.resources.noto_sans_regular
import org.jetbrains.compose.resources.Font

/** Noto Sans (SIL OFL): Regular for normal and medium weights, Bold for semi-bold and up. */
@Composable
internal fun grainCraftFontFamily(): FontFamily =
  FontFamily(
    Font(Res.font.noto_sans_regular, FontWeight.Normal),
    Font(Res.font.noto_sans_regular, FontWeight.Medium),
    Font(Res.font.noto_sans_bold, FontWeight.SemiBold),
    Font(Res.font.noto_sans_bold, FontWeight.Bold),
  )

private fun style(family: FontFamily, size: Int, lineHeight: Int, weight: FontWeight): TextStyle =
  TextStyle(
    fontFamily = family,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
  )

/** The app's type scale in the bundled font; body text never goes below 13 sp. */
@Composable
internal fun grainCraftTypography(): Typography {
  val f = grainCraftFontFamily()
  return Typography(
    headlineSmall = style(f, 22, 28, FontWeight.Bold),
    titleLarge = style(f, 20, 26, FontWeight.Bold),
    titleMedium = style(f, 16, 22, FontWeight.Bold),
    titleSmall = style(f, 14, 20, FontWeight.SemiBold),
    bodyLarge = style(f, 16, 22, FontWeight.Normal),
    bodyMedium = style(f, 14, 20, FontWeight.Normal),
    bodySmall = style(f, 13, 18, FontWeight.Normal),
    labelLarge = style(f, 14, 20, FontWeight.SemiBold),
    labelMedium = style(f, 12, 16, FontWeight.SemiBold),
    labelSmall = style(f, 11, 14, FontWeight.Medium),
  )
}
