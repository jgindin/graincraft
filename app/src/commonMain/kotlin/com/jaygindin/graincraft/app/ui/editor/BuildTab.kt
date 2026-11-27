package com.jaygindin.graincraft.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.jaygindin.graincraft.app.ui.diagram.DiagramCanvas
import com.jaygindin.graincraft.app.ui.theme.LocalRatingColors
import com.jaygindin.graincraft.app.ui.theme.Spacing
import com.jaygindin.graincraft.core.compile.CompiledDesign
import com.jaygindin.graincraft.core.guide.Callout
import com.jaygindin.graincraft.core.guide.CalloutKind
import com.jaygindin.graincraft.core.guide.Stage
import graincraft.app.generated.resources.Res
import graincraft.app.generated.resources.ic_info
import graincraft.app.generated.resources.ic_warning
import org.jetbrains.compose.resources.painterResource

private val CALLOUT_ICON = 20.dp

/** The build guide: one card per stage, with its steps, callouts and diagram. */
@Composable
internal fun BuildTab(compiled: CompiledDesign, modifier: Modifier) {
  Column(
    modifier.verticalScroll(rememberScrollState()).padding(Spacing.lg),
    verticalArrangement = Arrangement.spacedBy(Spacing.md),
  ) {
    compiled.stages.forEach { StageCard(it) }
  }
}

@Composable
private fun StageCard(stage: Stage) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    modifier = Modifier.fillMaxWidth().testTag("stage-${stage.number}"),
  ) {
    Column(Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
      Text("${stage.number}. ${stage.title}", style = MaterialTheme.typography.titleSmall)
      stage.steps.forEach { step ->
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
          Text("•", style = MaterialTheme.typography.bodySmall)
          Text(step, style = MaterialTheme.typography.bodySmall)
        }
      }
      stage.callouts.forEach { CalloutRow(it) }
      stage.diagram?.let { diagram ->
        // A plain surface of its own, so the diagram stands apart from the card's text.
        Surface(
          color = MaterialTheme.colorScheme.surfaceContainerLowest,
          shape = MaterialTheme.shapes.small,
          modifier = Modifier.fillMaxWidth(),
        ) {
          DiagramCanvas(diagram, Modifier.padding(Spacing.sm))
        }
      }
    }
  }
}

@Composable
private fun CalloutRow(callout: Callout) {
  val ratings = LocalRatingColors.current
  val warning = callout.kind == CalloutKind.WARNING
  Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
    Icon(
      painterResource(if (warning) Res.drawable.ic_warning else Res.drawable.ic_info),
      contentDescription = if (warning) "Warning" else "Tip",
      tint = if (warning) ratings.caution else MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(CALLOUT_ICON),
    )
    Text(callout.text, style = MaterialTheme.typography.bodySmall)
  }
}
