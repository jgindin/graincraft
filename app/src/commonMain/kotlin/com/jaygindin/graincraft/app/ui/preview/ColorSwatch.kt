package com.jaygindin.graincraft.app.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jaygindin.graincraft.core.wood.Rgb

/**
 * A small rounded square of a wood colour, which is data rather than theme. A null [colour] draws
 * an empty outline, for text that is not a colour yet.
 */
@Composable
fun ColorSwatch(colour: Rgb?, modifier: Modifier = Modifier, size: Dp = 24.dp) {
  val shape = MaterialTheme.shapes.small
  val fill = if (colour == null) Color.Transparent else Color(colour.argb)
  Box(
    modifier =
      modifier
        .size(size)
        .background(fill, shape)
        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
  )
}
