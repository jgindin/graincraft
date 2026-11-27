package com.jaygindin.graincraft.app.ui.editor

import com.jaygindin.graincraft.app.ui.library.sampleDesign
import com.jaygindin.graincraft.core.units.Length
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.test.runTest

/** Typing into one field is one undo step, not one per keystroke. */
class EditorViewModelCoalesceTest {
  @Test
  fun typingANameIsOneUndoStep() = runTest {
    val vm = editor(sampleDesign("d1", "Board"))
    for (end in 1.."Walnut".length) vm.edit("name") { it.copy(name = "Walnut".take(end)) }
    assertEquals("Walnut", vm.state.value.design.name)

    vm.undo()
    assertEquals("Board", vm.state.value.design.name)
    assertFalse(vm.state.value.canUndo)
    vm.redo()
    assertEquals("Walnut", vm.state.value.design.name)
  }

  @Test
  fun typingALengthIsOneUndoStep() = runTest {
    val vm = editor(sampleDesign("d1", "Board"))
    for (inches in listOf(1, 14)) {
      vm.edit("dim-width") { it.copy(finished = it.finished.copy(width = Length.inches(inches))) }
    }
    vm.undo()
    assertEquals(Length.inches(12), vm.state.value.design.finished.width)
    assertFalse(vm.state.value.canUndo)
  }

  @Test
  fun anotherEditBetweenTypingsMakesTwoSteps() = runTest {
    val vm = editor(sampleDesign("d1", "Board"))
    vm.edit("name") { it.copy(name = "W") }
    vm.edit("name") { it.copy(name = "Wa") }
    vm.edit("dim-width") { it.copy(finished = it.finished.copy(width = Length.inches(14))) }
    vm.edit("name") { it.copy(name = "Wal") }
    vm.edit("name") { it.copy(name = "Waln") }

    vm.undo()
    assertEquals("Wa", vm.state.value.design.name)
    assertEquals(Length.inches(14), vm.state.value.design.finished.width)
    vm.undo()
    assertEquals(Length.inches(12), vm.state.value.design.finished.width)
    vm.undo()
    assertEquals("Board", vm.state.value.design.name)

    // Undo breaks a run too: typing after it starts a new step.
    vm.redo()
    vm.edit("name") { it.copy(name = "Wx") }
    vm.edit("name") { it.copy(name = "Wxy") }
    vm.undo()
    assertEquals("Wa", vm.state.value.design.name)
  }
}
