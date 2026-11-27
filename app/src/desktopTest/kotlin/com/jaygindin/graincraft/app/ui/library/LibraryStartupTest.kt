package com.jaygindin.graincraft.app.ui.library

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import com.jaygindin.graincraft.app.App
import com.jaygindin.graincraft.app.AppGraph
import com.jaygindin.graincraft.app.data.FileWorkspaceStore
import com.jaygindin.graincraft.app.platform.SkiaArgbImageFactory
import com.jaygindin.graincraft.app.testutil.FakeExternalBrowser
import com.jaygindin.graincraft.app.testutil.FakeFileDialogs
import com.jaygindin.graincraft.core.design.BoardDesign
import com.jaygindin.graincraft.core.design.DesignJson
import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.design.PatternSpec
import com.jaygindin.graincraft.core.design.Strip
import com.jaygindin.graincraft.core.units.Length
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test

/** The library is the first screen, so files on disk that would break it must not stop the app. */
@OptIn(ExperimentalTestApi::class)
class LibraryStartupTest {
  private val root: Path = Files.createTempDirectory("graincraft-startup")
  private val designs = root.resolve("designs").createDirectories()

  @AfterTest fun cleanUp() = root.toFile().deleteRecursively().let {}

  private fun write(fileName: String, design: BoardDesign) =
    designs.resolve(fileName).writeText(DesignJson.encode(design))

  private fun startApp(vararg expectedTags: String) = runComposeUiTest {
    val graph =
      AppGraph(
        FileWorkspaceStore(root) { 0L },
        FakeFileDialogs(),
        FakeExternalBrowser(),
        clock = { 0L },
        newId = { "fresh" },
        images = SkiaArgbImageFactory,
      )
    setContent { App(graph) }
    for (tag in expectedTags) {
      waitUntil(timeoutMillis = 10_000) {
        onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
      }
    }
  }

  @Test
  fun twoFilesWithTheSameIdStillStart() {
    val design = sampleDesign("a", "Walnut board")
    write("a.graincraft.json", design)
    write("a copy.graincraft.json", design)
    startApp("design-card-a", "unreadable-a copy.graincraft.json")
  }

  @Test
  fun savedDesignsWithExtremeSizesStillStart() {
    val board = Dimensions(Length.inches(36), Length.inches(24), Length.inches(1, 1, 2))
    val cell =
      sampleDesign("cell", "Tiny cells")
        .copy(
          finished = board,
          pattern = PatternSpec.Checkerboard("black-walnut", "hard-maple", Length.millimetres(1.0)),
        )
    fun stripes(id: String, width: Length) =
      sampleDesign(id, id, PatternKind.STRIPES)
        .copy(
          grainMode = GrainMode.EDGE_GRAIN,
          finished = board,
          pattern = PatternSpec.Stripes(listOf(Strip("black-walnut", width))),
        )
    write("cell.graincraft.json", cell)
    write("thin.graincraft.json", stripes("thin", Length.inchesApprox(1e-6)))
    write("wide.graincraft.json", stripes("wide", Length.inches(300)))
    startApp("design-card-cell", "design-card-thin", "design-card-wide")
  }
}
