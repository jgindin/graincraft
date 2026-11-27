package com.jaygindin.graincraft.app.ui.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import com.jaygindin.graincraft.app.ui.theme.Spacing
import com.jaygindin.graincraft.core.compile.CompiledDesign
import com.jaygindin.graincraft.core.fabrication.CompileError

@Composable
internal fun ResultTabs(tabs: List<EditorTab>, selected: EditorTab, onSelect: (EditorTab) -> Unit) {
  PrimaryTabRow(
    selectedTabIndex = tabs.indexOf(selected).coerceAtLeast(0),
    containerColor = MaterialTheme.colorScheme.background,
  ) {
    tabs.forEach { tab ->
      Tab(
        selected = tab == selected,
        onClick = { onSelect(tab) },
        text = { Text(tab.label) },
        modifier = Modifier.testTag(tab.tag),
      )
    }
  }
}

/**
 * The Issues, Build or Lumber tab. Issues always shows, listing the compile errors when there are
 * any; Build and Lumber wait for a design that compiles, rather than show a stale plan.
 */
@Composable
internal fun ResultContent(
  tab: EditorTab,
  state: EditorState,
  compiled: CompiledDesign?,
  errors: List<CompileError>,
  modifier: Modifier,
) {
  val current = compiled.takeIf { errors.isEmpty() && state.compile != null }
  Box(modifier.fillMaxWidth()) {
    when {
      tab == EditorTab.ISSUES -> IssuesTab(current, errors, Modifier.fillMaxSize())
      current == null && errors.isNotEmpty() ->
        Waiting("Fix the errors in the design to see its ${tab.label.lowercase()}.")
      current == null -> Waiting("Working it out…")
      tab == EditorTab.BUILD -> BuildTab(current, Modifier.fillMaxSize())
      else -> LumberTab(current, state.woods, Modifier.fillMaxSize())
    }
  }
}

@Composable
private fun Waiting(text: String) {
  Box(Modifier.fillMaxSize().padding(Spacing.xl), contentAlignment = Alignment.Center) {
    Text(
      text,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}
