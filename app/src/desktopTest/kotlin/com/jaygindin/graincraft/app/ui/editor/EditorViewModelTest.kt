package com.jaygindin.graincraft.app.ui.editor

import com.jaygindin.graincraft.app.testutil.FakeWorkspaceStore
import com.jaygindin.graincraft.app.ui.library.PatternKind
import com.jaygindin.graincraft.app.ui.library.defaultPattern
import com.jaygindin.graincraft.app.ui.library.sampleDesign
import com.jaygindin.graincraft.core.compile.CompileResult
import com.jaygindin.graincraft.core.compile.DesignCompiler
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.design.PatternSpec
import com.jaygindin.graincraft.core.design.Strip
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.wood.Woods
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/** Editing, undo, grain switching and compiling. */
class EditorViewModelTest {
  @Test
  fun editCompilesAfterDebounce() = runTest {
    val vm = editor(sampleDesign("d1", "Board"))
    assertEquals("Board", vm.compiledName())

    vm.edit { it.copy(name = "Renamed") }
    assertEquals("Renamed", vm.state.value.design.name)
    advance(100)
    assertEquals("Board", vm.compiledName())
    advance(50)
    assertEquals("Renamed", vm.compiledName())
  }

  @Test
  fun undoRedo() = runTest {
    val store = FakeWorkspaceStore()
    val vm = editor(sampleDesign("d1", "Board"), EditorInputs(store = store))
    assertFalse(vm.state.value.canUndo)
    assertFalse(vm.state.value.canRedo)

    vm.edit { it.copy(name = "A") }
    vm.edit { it.copy(name = "B") }
    vm.undo()
    assertEquals("A", vm.state.value.design.name)
    assertTrue(vm.state.value.canRedo)
    vm.undo()
    assertEquals("Board", vm.state.value.design.name)
    assertFalse(vm.state.value.canUndo)
    vm.redo()
    assertEquals("A", vm.state.value.design.name)
    assertTrue(vm.state.value.canUndo)

    advance(1000)
    assertEquals("A", store.savedDesigns.single().name)
    assertEquals("A", vm.compiledName())

    // Undo keeps counting revisions up, so a later save never goes back in time.
    vm.undo()
    advance(1000)
    assertEquals("Board", store.savedDesigns.last().name)
    assertEquals(3, store.savedDesigns.last().revision)

    vm.edit { it.copy(name = "C") }
    assertFalse(vm.state.value.canRedo)
  }

  @Test
  fun historyHoldsOneHundredSteps() = runTest {
    val vm = editor(sampleDesign("d1", "Board"))

    repeat(101) { i -> vm.edit { it.copy(name = "$i") } }
    repeat(100) { vm.undo() }

    assertFalse(vm.state.value.canUndo)
    assertEquals("0", vm.state.value.design.name)
  }

  @Test
  fun embeddedWoodsCompileWhenTheUserLacksThem() = runTest {
    val mystery = customWood("custom-mystery")
    val design =
      sampleDesign("d1", "Board", PatternKind.STRIPES)
        .copy(
          pattern = PatternSpec.Stripes(List(2) { Strip("custom-mystery", Length.inches(4)) }),
          embeddedCustomWoods = listOf(mystery),
        )
    val vm = editor(design)

    assertIs<CompileResult.Ok>(vm.state.value.compile)
    assertEquals(mystery, vm.state.value.woods.byId("custom-mystery"))
  }

  @Test
  fun grainSwitchAsksForNonStripes() = runTest {
    val vm = editor(sampleDesign("d1", "Board", PatternKind.CHECKERBOARD))
    val original = vm.state.value.design

    vm.requestGrainMode(GrainMode.EDGE_GRAIN)
    assertTrue(vm.state.value.pendingGrainSwitch)
    assertEquals(original, vm.state.value.design)
    vm.cancelGrainSwitch()
    assertFalse(vm.state.value.pendingGrainSwitch)
    assertEquals(original, vm.state.value.design)

    vm.requestGrainMode(GrainMode.EDGE_GRAIN)
    vm.confirmGrainSwitch()
    assertFalse(vm.state.value.pendingGrainSwitch)
    assertEquals(GrainMode.EDGE_GRAIN, vm.state.value.design.grainMode)
    assertEquals(defaultPattern(PatternKind.STRIPES), vm.state.value.design.pattern)

    // Stripes switch freely, and the confirmed switch was one undo step.
    vm.requestGrainMode(GrainMode.END_GRAIN)
    assertFalse(vm.state.value.pendingGrainSwitch)
    assertEquals(GrainMode.END_GRAIN, vm.state.value.design.grainMode)
    vm.undo()
    vm.undo()
    assertEquals(original, vm.state.value.design)
  }

  @Test
  fun profileChangeRecompiles() = runTest {
    val store = FakeWorkspaceStore()
    val inputs = EditorInputs(store = store)
    val design = sampleDesign("d1", "Board")
    val vm = editor(design, inputs)
    val before = vm.state.value.compile

    val wasteful = ShopProfile(millingWaste = 0.5)
    inputs.profile.value = wasteful
    advance(150)

    val expected = DesignCompiler.compile(design, Woods(emptyList()), wasteful, inchFormatter)
    assertEquals(expected, vm.state.value.compile)
    assertTrue(expected != before)
    advance(2000)
    assertTrue(store.savedDesigns.isEmpty())
  }

  @Test
  fun finishIsNotAnEdit() = runTest {
    val vm = editor(sampleDesign("d1", "Board"))
    assertEquals(Finish.RAW, vm.state.value.finish)

    vm.setFinish(Finish.OILED)

    assertEquals(Finish.OILED, vm.state.value.finish)
    assertFalse(vm.state.value.canUndo)
  }
}
