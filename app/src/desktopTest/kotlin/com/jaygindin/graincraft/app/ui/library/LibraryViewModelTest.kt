package com.jaygindin.graincraft.app.ui.library

import com.jaygindin.graincraft.app.testutil.FakePlatform
import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.design.PatternSpec
import com.jaygindin.graincraft.core.units.Length
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
  private val size = Dimensions(Length.inches(18), Length.inches(12), Length.inches(1, 1, 2))

  @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

  @AfterTest fun tearDown() = Dispatchers.resetMain()

  @Test
  fun createSavesAndLists() = runTest {
    val platform = FakePlatform()
    val vm = libraryViewModel(platform)

    val id = vm.create("Cutting board", GrainMode.END_GRAIN, PatternKind.CHECKERBOARD, size)

    assertEquals("id-1", id)
    val saved = platform.store.designs.getValue(id)
    assertEquals(1, saved.revision)
    assertEquals(platform.nowMillis, saved.updatedAtMillis)
    assertEquals(defaultPattern(PatternKind.CHECKERBOARD), saved.pattern)
    val item = vm.state.value.items.single()
    assertEquals("Cutting board", item.name)
    assertEquals("18\" × 12\" · End-grain checkerboard", item.summary)
    assertNotNull(item.compiled)
  }

  @Test
  fun defaultPatternsMatchTheBrief() {
    val stripes = defaultPattern(PatternKind.STRIPES) as PatternSpec.Stripes
    assertEquals(7, stripes.strips.size)
    assertEquals(
      List(7) { if (it % 2 == 0) "black-walnut" else "hard-maple" },
      stripes.strips.map { it.speciesId },
    )
    assertTrue(stripes.strips.all { it.width == Length.inches(1, 1, 2) })
    assertEquals(
      PatternSpec.Brick(
        "american-cherry",
        "hard-maple",
        Length.inches(3),
        Length.inches(1, 1, 2),
        Length.inches(0, 1, 4),
      ),
      defaultPattern(PatternKind.BRICK),
    )
  }

  @Test
  fun duplicateGetsNewIdAndName() = runTest {
    val platform = FakePlatform()
    platform.store.designs["a"] = sampleDesign("a", "Walnut board")
    val vm = libraryViewModel(platform)

    vm.duplicate("a")

    val copy = platform.store.designs.getValue("id-1")
    assertEquals("Walnut board copy", copy.name)
    assertEquals(platform.store.designs.getValue("a").pattern, copy.pattern)
    assertEquals(2, vm.state.value.items.size)
  }

  @Test
  fun renameSavesNewNameAndRevision() = runTest {
    val platform = FakePlatform()
    platform.store.designs["a"] = sampleDesign("a", "Old")
    val vm = libraryViewModel(platform)

    vm.rename("a", "New")

    val saved = platform.store.designs.getValue("a")
    assertEquals("New", saved.name)
    assertEquals(2, saved.revision)
  }

  @Test
  fun deleteThenUndoRestores() = runTest {
    val platform = FakePlatform()
    platform.store.designs["a"] = sampleDesign("a", "Walnut board")
    val vm = libraryViewModel(platform)

    vm.delete("a")
    assertTrue(vm.state.value.items.isEmpty())
    assertEquals("Walnut board", vm.state.value.pendingUndo)
    assertTrue("a" in platform.store.trash)

    vm.endUndo(restore = true)
    assertEquals("a", vm.state.value.items.single().id)
    assertNull(vm.state.value.pendingUndo)
  }
}
