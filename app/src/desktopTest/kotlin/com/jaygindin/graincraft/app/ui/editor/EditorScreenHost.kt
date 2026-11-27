package com.jaygindin.graincraft.app.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import com.jaygindin.graincraft.app.platform.SkiaArgbImageFactory
import com.jaygindin.graincraft.app.testutil.FakeWorkspaceStore
import com.jaygindin.graincraft.app.ui.LocalUnitFormatter
import com.jaygindin.graincraft.app.ui.preview.LocalArgbImageFactory
import com.jaygindin.graincraft.app.ui.theme.GrainCraftTheme
import com.jaygindin.graincraft.core.design.BoardDesign
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.UnitFormatter
import com.jaygindin.graincraft.core.wood.Woods
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow

private const val WAIT_MILLIS = 10_000L

/** An editor on real dispatchers, as the app runs it; [close] stops its background work. */
class LiveEditor(
  design: BoardDesign,
  formatter: UnitFormatter = inchFormatter,
  val store: FakeWorkspaceStore = FakeWorkspaceStore(),
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  val viewModel =
    EditorViewModel(
      initial = design,
      store = store,
      woods = MutableStateFlow(Woods(emptyList())),
      profile = MutableStateFlow(ShopProfile()),
      formatter = MutableStateFlow(formatter),
      clock = { START_MILLIS },
      scope = scope,
    )

  fun close() = scope.cancel()
}

/** The editor screen in the app's theme, units and bitmap factory. */
@Composable
fun EditorHost(
  viewModel: EditorViewModel,
  formatter: UnitFormatter = inchFormatter,
  dark: Boolean = false,
) {
  GrainCraftTheme(dark) {
    CompositionLocalProvider(
      LocalUnitFormatter provides formatter,
      LocalArgbImageFactory provides SkiaArgbImageFactory,
    ) {
      EditorScreen(viewModel, onBack = {}, onExport = {})
    }
  }
}

/** Shows [design] in an editor, waits for its first compile, runs [block], then stops it. */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.withEditor(
  design: BoardDesign,
  formatter: UnitFormatter = inchFormatter,
  dark: Boolean = false,
  block: ComposeUiTest.(LiveEditor) -> Unit,
) {
  val editor = LiveEditor(design, formatter)
  try {
    setContent { EditorHost(editor.viewModel, formatter, dark) }
    waitUntil(timeoutMillis = WAIT_MILLIS) { editor.viewModel.state.value.compile != null }
    waitForIdle()
    block(editor)
  } finally {
    editor.close()
  }
}

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.awaitTag(tag: String) {
  waitUntil(timeoutMillis = WAIT_MILLIS) {
    onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
  }
}

@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.awaitUntil(condition: () -> Boolean) {
  waitUntil(timeoutMillis = WAIT_MILLIS, condition = condition)
}

/** The text a node shows, or for a text field the text in it. */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.textOf(tag: String): String {
  val config = onNodeWithTag(tag, useUnmergedTree = false).fetchSemanticsNode().config
  val edit = config.getOrNull(SemanticsProperties.EditableText)
  if (edit != null) return edit.text
  return config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString(" ") { it.text }
}

/** Every text on screen, joined, for checking that something shows anywhere. */
@OptIn(ExperimentalTestApi::class)
fun ComposeUiTest.allText(): String =
  onAllNodes(androidx.compose.ui.test.isRoot(), useUnmergedTree = true)
    .fetchSemanticsNodes()
    .flatMap { root -> root.allTexts() }
    .joinToString("\n")

private fun androidx.compose.ui.semantics.SemanticsNode.allTexts(): List<String> =
  config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
    children.flatMap { it.allTexts() }
