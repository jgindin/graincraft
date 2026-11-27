package com.jaygindin.graincraft.app.ui.editor

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.jaygindin.graincraft.app.ui.library.PatternKind
import com.jaygindin.graincraft.app.ui.library.defaultPattern
import com.jaygindin.graincraft.app.ui.preview.PreviewExamples
import com.jaygindin.graincraft.app.ui.preview.PreviewExamples.inch
import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.design.PatternSpec
import com.jaygindin.graincraft.core.design.Strip
import com.jaygindin.graincraft.core.units.FractionPrecision
import com.jaygindin.graincraft.core.units.LengthUnit
import com.jaygindin.graincraft.core.units.UnitFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private const val WIDE = 1400
private const val TALL = 900

@OptIn(ExperimentalTestApi::class)
class EditorScreenTest {
  @Test
  fun changingWidthUpdatesLumber() =
    runDesktopComposeUiTest(WIDE, TALL) {
      withEditor(PreviewExamples.a) {
        onNodeWithTag("tab-lumber").performClick()
        awaitTag("lumber-total")
        val before = textOf("lumber-total")
        assertTrue(before.contains("5.5 BF"), before)

        onNodeWithTag("dim-width").performTextReplacement("14")
        awaitUntil { textOf("lumber-total") != before }
        assertNotEquals(before, textOf("lumber-total"))
      }
    }

  @Test
  fun addingRedOakShowsAvoid() =
    runDesktopComposeUiTest(WIDE, TALL) {
      val stripes =
        PreviewExamples.a.copy(
          finished = Dimensions(inch(16), inch(10, 1, 2), inch(1, 1, 2)),
          pattern = defaultPattern(PatternKind.STRIPES),
        )
      withEditor(stripes) {
        onNodeWithTag("tab-issues").performClick()
        onNodeWithTag("issue-SPECIES_NOT_SUITABLE").assertDoesNotExist()

        onNodeWithTag("strip-add").performScrollTo().performClick()
        onNodeWithTag("strip-7-species").performScrollTo().performClick()
        onNodeWithTag("strip-7-species-red-oak").performScrollTo().performClick()

        awaitTag("issue-SPECIES_NOT_SUITABLE")
        val strips = (it.viewModel.state.value.design.pattern as PatternSpec.Stripes).strips
        assertEquals("red-oak", strips.last().speciesId)
      }
    }

  @Test
  fun buildTabListsStages() =
    runDesktopComposeUiTest(WIDE, TALL) {
      withEditor(PreviewExamples.a) {
        onNodeWithTag("tab-build").performClick()
        // Ten stages, less edge details, which this board has none of.
        for (n in 1..9) onNodeWithTag("stage-$n").assertExists()
        onNodeWithTag("stage-10").assertDoesNotExist()
      }
    }

  @Test
  fun undoRestoresWidth() =
    runDesktopComposeUiTest(WIDE, TALL) {
      withEditor(PreviewExamples.a) {
        onNodeWithTag("dim-width").performTextReplacement("14")
        assertEquals(inch(14), it.viewModel.state.value.design.finished.width)

        onNodeWithTag("editor-undo").performClick()
        waitForIdle()
        assertEquals(inch(12), it.viewModel.state.value.design.finished.width)
        assertEquals("12\"", textOf("dim-width"))

        onNodeWithTag("editor-redo").performClick()
        waitForIdle()
        assertEquals("14\"", textOf("dim-width"))
      }
    }

  @Test
  fun undoTakesBackWholeTypings() =
    runDesktopComposeUiTest(WIDE, TALL) {
      withEditor(PreviewExamples.a) {
        onNodeWithTag("editor-name").performTextClearance()
        "Walnut".forEach { c -> onNodeWithTag("editor-name").performTextInput(c.toString()) }
        onNodeWithTag("dim-width").performTextReplacement("1")
        onNodeWithTag("dim-width").performTextInput("4")
        assertEquals("Walnut", it.viewModel.state.value.design.name)
        assertEquals(inch(14), it.viewModel.state.value.design.finished.width)

        // One undo per field typed in, not one per keystroke.
        onNodeWithTag("editor-undo").performClick()
        waitForIdle()
        assertEquals(inch(12), it.viewModel.state.value.design.finished.width)
        assertEquals("Walnut", it.viewModel.state.value.design.name)
        onNodeWithTag("editor-undo").performClick()
        waitForIdle()
        assertEquals("Example", it.viewModel.state.value.design.name)
        assertEquals("Example", textOf("editor-name"))
        assertTrue(!it.viewModel.state.value.canUndo)
      }
    }

  @Test
  fun grainSwitchConfirmation() =
    runDesktopComposeUiTest(WIDE, TALL) {
      withEditor(PreviewExamples.a) {
        onNodeWithTag("grain-edge").performClick()
        onNodeWithTag("grain-switch-cancel").performClick()
        assertEquals(GrainMode.END_GRAIN, it.viewModel.state.value.design.grainMode)
        onNodeWithTag("grain-switch-confirm").assertDoesNotExist()

        onNodeWithTag("grain-edge").performClick()
        onNodeWithTag("grain-switch-confirm").performClick()
        waitForIdle()
        val design = it.viewModel.state.value.design
        assertEquals(GrainMode.EDGE_GRAIN, design.grainMode)
        assertTrue(design.pattern is PatternSpec.Stripes)
        onNodeWithTag("pattern-stripes").assertIsSelected()
        onNodeWithTag("strip-0-species").assertExists()
      }
    }

  @Test
  fun metricShowsMillimetresEverywhere() =
    runDesktopComposeUiTest(WIDE, TALL) {
      val metric = UnitFormatter(LengthUnit.MILLIMETRE, FractionPrecision.SIXTEENTH)
      withEditor(PreviewExamples.a, formatter = metric) {
        assertTrue(textOf("dim-length").contains("457.2"), textOf("dim-length"))

        onNodeWithTag("tab-build").performClick()
        awaitTag("stage-1")
        val build = allText()
        assertTrue(build.contains(" mm"), build)
        assertTrue(!build.contains("\""), "an inch mark in the build tab: $build")

        onNodeWithTag("tab-lumber").performClick()
        awaitTag("lumber-total")
        val lumber = allText()
        assertTrue(lumber.contains(" mm"), lumber)

        assertEquals(PreviewExamples.a, it.viewModel.state.value.design)
        assertTrue(it.store.designs.isEmpty(), "showing in mm saved the design")
      }
    }

  @Test
  fun compileErrorShowsCard() =
    runDesktopComposeUiTest(WIDE, TALL) {
      withEditor(PreviewExamples.a) {
        onNodeWithTag("compile-error").assertDoesNotExist()
        onNodeWithTag("dim-length").performTextReplacement("40")
        awaitTag("compile-error")
        assertEquals(
          "The length must be between 6\" and 36\".",
          textOf("compile-error-0"),
        )
      }
    }

  @Test
  fun pinstripesOnOneWoodKeepTheStrips() =
    runDesktopComposeUiTest(WIDE, TALL) {
      val maple =
        PreviewExamples.b.copy(
          pattern = PatternSpec.Stripes(List(5) { Strip("hard-maple", inch(1, 1, 2)) }),
        )
      withEditor(maple) {
        onNodeWithTag("stripe-presets").performScrollTo().performClick()
        onNodeWithTag("stripe-preset-pinstripe").performClick()
        val strips = (it.viewModel.state.value.design.pattern as PatternSpec.Stripes).strips
        assertEquals(9, strips.size)
        assertEquals("hard-maple", strips.first().speciesId)
        assertNotEquals("hard-maple", strips[1].speciesId)
      }
    }
}
