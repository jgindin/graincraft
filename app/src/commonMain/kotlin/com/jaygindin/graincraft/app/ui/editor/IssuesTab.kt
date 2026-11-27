package com.jaygindin.graincraft.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.jaygindin.graincraft.app.ui.LocalUnitFormatter
import com.jaygindin.graincraft.app.ui.theme.LocalRatingColors
import com.jaygindin.graincraft.app.ui.theme.Spacing
import com.jaygindin.graincraft.core.audit.Severity
import com.jaygindin.graincraft.core.compile.CompiledDesign
import com.jaygindin.graincraft.core.compile.message
import com.jaygindin.graincraft.core.fabrication.CompileError
import graincraft.app.generated.resources.Res
import graincraft.app.generated.resources.ic_error
import graincraft.app.generated.resources.ic_info
import graincraft.app.generated.resources.ic_warning
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * The woodworking warnings, worst first, then the notes on how the pattern was fitted. While the
 * design does not compile, its errors are listed instead.
 */
@Composable
internal fun IssuesTab(compiled: CompiledDesign?, errors: List<CompileError>, modifier: Modifier) {
  val formatter = LocalUnitFormatter.current
  Column(
    modifier.verticalScroll(rememberScrollState()).padding(Spacing.lg),
    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
  ) {
    errors.forEachIndexed { i, error ->
      IssueCard(
        Severity.AVOID,
        "The design can't be built",
        error.message(formatter),
        Modifier.testTag("issue-error-$i"),
      )
    }
    if (compiled == null) return@Column
    compiled.issues.forEach { issue ->
      IssueCard(
        issue.severity,
        issue.title,
        issue.detail,
        Modifier.testTag("issue-${issue.code.name}"),
      )
    }
    compiled.fit.notes.forEachIndexed { i, note ->
      IssueCard(Severity.INFO, "Fitted to size", note, Modifier.testTag("fit-note-$i"))
    }
    if (compiled.issues.isEmpty() && compiled.fit.notes.isEmpty()) {
      Text(
        "No issues: these woods and sizes suit this board.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("issues-none"),
      )
    }
  }
}

@Composable
private fun IssueCard(severity: Severity, title: String, detail: String, modifier: Modifier) {
  val (icon, tint, description) = severity.look()
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    modifier = modifier.fillMaxWidth(),
  ) {
    Row(Modifier.padding(Spacing.md), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
      Icon(painterResource(icon), contentDescription = description, tint = tint)
      Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        if (detail.isNotBlank()) {
          Text(
            detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

private data class SeverityLook(val icon: DrawableResource, val tint: Color, val text: String)

@Composable
private fun Severity.look(): SeverityLook {
  val ratings = LocalRatingColors.current
  return when (this) {
    Severity.AVOID -> SeverityLook(Res.drawable.ic_error, ratings.notSuitable, "Avoid")
    Severity.WARNING -> SeverityLook(Res.drawable.ic_warning, ratings.caution, "Warning")
    Severity.INFO -> SeverityLook(Res.drawable.ic_info, MaterialTheme.colorScheme.primary, "Note")
  }
}
