package com.jaygindin.graincraft.app.ui.library

import com.jaygindin.graincraft.app.testutil.FakePlatform
import com.jaygindin.graincraft.core.design.DesignJson
import com.jaygindin.graincraft.core.design.GrainMode
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/** Library actions that fail show why, and the library keeps working. */
@OptIn(ExperimentalCoroutinesApi::class)
class LibraryFailureTest {
  @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

  @AfterTest fun tearDown() = Dispatchers.resetMain()

  @Test
  fun importWithUnsafeIdGetsNewId() = runTest {
    val platform = FakePlatform()
    platform.dialogs.openResult = DesignJson.encode(sampleDesign("walnut board", "Walnut board"))
    val vm = libraryViewModel(platform)

    vm.importFile()

    val imported = platform.store.designs.values.single()
    assertEquals("id-1", imported.id)
    assertEquals("Walnut board", imported.name)
    assertEquals(null, vm.state.value.message)
  }

  @Test
  fun anUnreadableImportFileSaysWhy() = runTest {
    val platform = FakePlatform()
    platform.dialogs.openFailure = IOException("Permission denied")
    val vm = libraryViewModel(platform)

    vm.importFile()

    assertEquals("Couldn't import the file: Permission denied", vm.state.value.message)
  }

  @Test
  fun aStoreThatCannotWriteShowsAMessage() = runTest {
    val platform = FakePlatform()
    platform.store.designs["a"] = sampleDesign("a", "Walnut board")
    val vm = libraryViewModel(platform)
    platform.store.writeFailure = IOException("No space left on device")

    vm.rename("a", "New")
    assertEquals("Couldn't rename the design: No space left on device", vm.state.value.message)
    vm.duplicate("a")
    assertEquals("Couldn't duplicate the design: No space left on device", vm.state.value.message)
    vm.delete("a")
    assertEquals("Couldn't delete the design: No space left on device", vm.state.value.message)
    vm.create(
      "Board",
      GrainMode.END_GRAIN,
      PatternKind.CHECKERBOARD,
      sampleDesign("x", "x").finished,
    )
    assertEquals("Couldn't create the design: No space left on device", vm.state.value.message)
    // The library still lists what is on disk.
    assertEquals(listOf("a"), vm.state.value.items.map { it.id })
  }

  @Test
  fun aFailedRestoreSaysWhy() = runTest {
    val platform = FakePlatform()
    platform.store.designs["a"] = sampleDesign("a", "Walnut board")
    val vm = libraryViewModel(platform)
    vm.delete("a")
    platform.store.writeFailure = IllegalArgumentException("Disk gone")

    vm.endUndo(restore = true)

    assertEquals("Couldn't restore the design: Disk gone", vm.state.value.message)
    assertEquals(null, vm.state.value.pendingUndo)
  }
}
