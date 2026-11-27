package com.jaygindin.graincraft.app.ui.editor

import com.jaygindin.graincraft.app.testutil.FakeWorkspaceStore
import com.jaygindin.graincraft.app.ui.library.PatternKind
import com.jaygindin.graincraft.app.ui.library.sampleDesign
import com.jaygindin.graincraft.core.compile.CompileResult
import com.jaygindin.graincraft.core.design.PatternSpec
import com.jaygindin.graincraft.core.design.Strip
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.wood.Woods
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/** Autosave, embedding custom woods, flushing on close and save failures. */
@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelSaveTest {
  @Test
  fun autosaveAfterOneSecond() = runTest {
    val store = FakeWorkspaceStore()
    val vm = editor(sampleDesign("d1", "Board"), EditorInputs(store = store))
    assertEquals(SaveStatus.SAVED, vm.state.value.saveStatus)

    vm.edit { it.copy(name = "Renamed") }
    assertEquals(SaveStatus.SAVING, vm.state.value.saveStatus)
    advance(999)
    assertTrue(store.savedDesigns.isEmpty())
    advance(1)

    val saved = store.savedDesigns.single()
    assertEquals("Renamed", saved.name)
    assertEquals(2, saved.revision)
    assertEquals(START_MILLIS + 1000, saved.updatedAtMillis)
    assertEquals(SaveStatus.SAVED, vm.state.value.saveStatus)
    assertEquals(2, vm.state.value.design.revision)
  }

  @Test
  fun rapidEditsSaveOnce() = runTest {
    val store = FakeWorkspaceStore()
    val vm = editor(sampleDesign("d1", "Board"), EditorInputs(store = store))

    vm.edit { it.copy(name = "A") }
    advance(500)
    vm.edit { it.copy(name = "AB") }
    advance(500)
    vm.edit { it.copy(name = "ABC") }
    advance(999)
    assertTrue(store.savedDesigns.isEmpty())
    advance(1)

    val saved = store.savedDesigns.single()
    assertEquals("ABC", saved.name)
    assertEquals(2, saved.revision)
    advance(5000)
    assertEquals(1, store.savedDesigns.size)
  }

  @Test
  fun saveEmbedsUsedCustomWoods() = runTest {
    val store = FakeWorkspaceStore()
    val used = customWood("custom-used")
    val unused = customWood("custom-unused")
    val inputs = EditorInputs(store = store, woods = MutableStateFlow(Woods(listOf(used, unused))))
    val vm = editor(sampleDesign("d1", "Board", PatternKind.STRIPES), inputs)

    vm.edit {
      it.copy(
        pattern =
          PatternSpec.Stripes(
            listOf(
              Strip("black-walnut", Length.inches(4)),
              Strip("custom-used", Length.inches(4)),
            ),
          ),
      )
    }
    advance(1000)

    assertEquals(listOf(used), store.savedDesigns.single().embeddedCustomWoods)
    assertEquals(listOf(used), vm.state.value.design.embeddedCustomWoods)
    assertIs<CompileResult.Ok>(vm.state.value.compile)
  }

  @Test
  fun closingFlushesPendingSave() = runTest {
    val store = FakeWorkspaceStore()
    val vm = editor(sampleDesign("d1", "Board"), EditorInputs(store = store))

    vm.flush()
    assertTrue(store.savedDesigns.isEmpty())

    vm.edit { it.copy(name = "Renamed") }
    advance(200)
    vm.flush()

    assertEquals("Renamed", store.savedDesigns.single().name)
    assertEquals(SaveStatus.SAVED, vm.state.value.saveStatus)
    advance(2000)
    assertEquals(1, store.savedDesigns.size)
  }

  @Test
  fun saveFailureShowsError() = runTest {
    val store = FlakyStore()
    val vm = editor(sampleDesign("d1", "Board"), EditorInputs(store = store))

    vm.edit { it.copy(name = "A") }
    advance(1000)
    assertEquals(SaveStatus.ERROR, vm.state.value.saveStatus)
    assertTrue(store.inner.savedDesigns.isEmpty())

    store.failing = false
    vm.edit { it.copy(name = "AB") }
    advance(1000)
    assertEquals("AB", store.inner.savedDesigns.single().name)
    assertEquals(SaveStatus.SAVED, vm.state.value.saveStatus)
  }

  @Test
  fun editDuringSaveKeepsEmbeddedOnlyWood() = runTest {
    val store = GatedStore()
    val mystery = customWood("custom-mystery")
    // The user has deleted this wood; only the design's embedded copy is left.
    val design =
      sampleDesign("d1", "Board", PatternKind.STRIPES).copy(embeddedCustomWoods = listOf(mystery))
    val vm = editor(design, EditorInputs(store = store))

    vm.edit { it.copy(name = "A") }
    advance(1000) // The first save is now writing, without the unused mystery wood.
    vm.edit {
      it.copy(pattern = PatternSpec.Stripes(List(2) { Strip("custom-mystery", Length.inches(4)) }))
    }
    store.gate.complete(Unit)
    runCurrent()

    assertEquals(emptyList(), store.inner.savedDesigns.single().embeddedCustomWoods)
    assertEquals(SaveStatus.SAVING, vm.state.value.saveStatus)
    assertEquals(listOf(mystery), vm.state.value.design.embeddedCustomWoods)
    advance(1000)
    val next = store.inner.savedDesigns.last()
    assertEquals(3, next.revision)
    assertEquals(listOf(mystery), next.embeddedCustomWoods)
    assertIs<CompileResult.Ok>(vm.state.value.compile)
  }
}
