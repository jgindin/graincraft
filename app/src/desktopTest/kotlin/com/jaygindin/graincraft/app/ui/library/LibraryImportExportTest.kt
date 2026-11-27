package com.jaygindin.graincraft.app.ui.library

import com.jaygindin.graincraft.app.testutil.FakePlatform
import com.jaygindin.graincraft.core.design.DesignJson
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryImportExportTest {
  @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

  @AfterTest fun tearDown() = Dispatchers.resetMain()

  @Test
  fun importCollisionGetsNewIdAndSuffix() = runTest {
    val platform = FakePlatform()
    val existing = sampleDesign("a", "Walnut board")
    platform.store.designs["a"] = existing
    platform.dialogs.openResult = DesignJson.encode(existing)
    val vm = libraryViewModel(platform)

    vm.importFile()

    assertEquals(2, platform.store.designs.size)
    val imported = platform.store.designs.getValue("id-1")
    assertEquals("Walnut board (imported)", imported.name)
    assertEquals("Walnut board", platform.store.designs.getValue("a").name)
  }

  @Test
  fun importNewIdKeepsNameAndCancelDoesNothing() = runTest {
    val platform = FakePlatform()
    val vm = libraryViewModel(platform)
    vm.importFile()
    assertTrue(platform.store.designs.isEmpty())

    platform.dialogs.openResult = DesignJson.encode(sampleDesign("z", "Fresh"))
    vm.importFile()
    assertEquals("Fresh", platform.store.designs.getValue("z").name)
  }

  @Test
  fun importNewerSchemaShowsError() = runTest {
    val platform = FakePlatform()
    platform.dialogs.openResult = """{"schemaVersion": 99, "id": "x"}"""
    val vm = libraryViewModel(platform)

    vm.importFile()

    assertTrue(platform.store.designs.isEmpty())
    assertTrue(vm.state.value.message.orEmpty().contains("newer"))
  }

  @Test
  fun importGarbageShowsError() = runTest {
    val platform = FakePlatform()
    platform.dialogs.openResult = "not json"
    val vm = libraryViewModel(platform)
    vm.importFile()
    assertNotEquals(null, vm.state.value.message)
  }

  @Test
  fun exportWritesTheDesignJson() = runTest {
    val platform = FakePlatform()
    val design = sampleDesign("a", "Walnut board")
    platform.store.designs["a"] = design
    val vm = libraryViewModel(platform)

    vm.exportFile("a")

    val save = platform.dialogs.saves.single()
    assertEquals("Walnut board.graincraft", save.suggestedName)
    assertEquals("json", save.extension)
    assertEquals(DesignJson.encode(design), save.contents)
  }
}
