package com.jaygindin.graincraft.app.ui.woods

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.jaygindin.graincraft.app.ui.theme.LocalRatingColors
import com.jaygindin.graincraft.core.wood.PoreType
import com.jaygindin.graincraft.core.wood.Rating
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

internal fun Rating.label(): String =
  when (this) {
    Rating.RECOMMENDED -> "Recommended"
    Rating.CAUTION -> "Caution"
    Rating.NOT_SUITABLE -> "Not suitable"
  }

internal fun PoreType.label(): String =
  when (this) {
    PoreType.DIFFUSE -> "Diffuse"
    PoreType.SEMI_RING -> "Semi-ring-porous"
    PoreType.RING_OPEN -> "Ring-porous, open"
    PoreType.RING_CLOSED -> "Ring-porous, closed"
  }

/** A read-only chip such as "Edge: Caution", coloured by the rating. */
@Composable
internal fun RatingChip(prefix: String, rating: Rating, modifier: Modifier = Modifier) {
  val colours = LocalRatingColors.current
  val (container, content) =
    when (rating) {
      Rating.RECOMMENDED -> colours.recommended to colours.onRecommended
      Rating.CAUTION -> colours.caution to colours.onCaution
      Rating.NOT_SUITABLE -> colours.notSuitable to colours.onNotSuitable
    }
  AssistChip(
    onClick = {},
    label = { Text("$prefix: ${rating.label()}") },
    colors = AssistChipDefaults.assistChipColors(containerColor = container, labelColor = content),
    border = null,
    modifier = modifier,
  )
}

/** A small icon that says what it means when hovered. */
@OptIn(ExperimentalMaterial3Api::class) // TooltipBox is still experimental in Material 3
@Composable
internal fun FlagIcon(icon: DrawableResource, description: String, tag: String) {
  TooltipBox(
    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
    tooltip = { PlainTooltip { Text(description) } },
    state = rememberTooltipState(),
  ) {
    Icon(painterResource(icon), contentDescription = description, modifier = Modifier.testTag(tag))
  }
}
