package com.jaygindin.graincraft.app.data

import com.jaygindin.graincraft.app.ui.theme.ThemeChoice
import com.jaygindin.graincraft.core.design.BoardDesign
import com.jaygindin.graincraft.core.design.DesignJson
import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.design.EdgeTreatments
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.design.PatternSpec
import com.jaygindin.graincraft.core.design.WorkspaceJson
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.FractionPrecision
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.LengthUnit
import com.jaygindin.graincraft.core.wood.WoodCatalog
import com.jaygindin.graincraft.core.wood.WoodSpecies
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.ListSerializer

abstract class StoreTestBase {
  protected val root: Path = Files.createTempDirectory("graincraft-store")
  protected var now = 1_000_000_000L
  protected val store = FileWorkspaceStore(root) { now }

  @AfterTest fun cleanUp() = root.toFile().deleteRecursively().let {}

  protected fun design(id: String, updated: Long = 1, name: String = "Board $id") =
    BoardDesign(
      id = id,
      name = name,
      revision = 1,
      updatedAtMillis = updated,
      grainMode = GrainMode.END_GRAIN,
      finished = Dimensions(Length.inches(18), Length.inches(12), Length.inches(1, 1, 2)),
      pattern = PatternSpec.Checkerboard("black-walnut", "hard-maple", Length.inches(1, 1, 2)),
      edges = EdgeTreatments(),
    )

  protected fun file(dir: String, id: String) = root.resolve(dir).resolve("$id.graincraft.json")

  protected fun newerFileText(version: Int) = """{"schemaVersion": $version, "id": "n"}"""
}

class FileWorkspaceStoreTest : StoreTestBase() {
  @Test
  fun savesAndListsDesigns() =
    runBlocking<Unit> {
      store.saveDesign(design("a", updated = 10))
      store.saveDesign(design("b", updated = 20))
      val listed = store.listDesigns()
      assertEquals(listOf("b.graincraft.json", "a.graincraft.json"), listed.map { it.fileName })
      assertEquals(design("b", updated = 20), (listed[0] as StoredDesign.Readable).design)
      assertEquals(root.toAbsolutePath().toString(), store.rootDescription)
    }

  @Test
  fun listSortsUnreadableAfterReadableByFileName() =
    runBlocking<Unit> {
      store.saveDesign(design("a", updated = 10))
      root.resolve("designs").resolve("z.graincraft.json").writeText("{")
      root.resolve("designs").resolve("m.graincraft.json").writeText("{")
      assertEquals(
        listOf("a.graincraft.json", "m.graincraft.json", "z.graincraft.json"),
        store.listDesigns().map { it.fileName },
      )
    }

  @Test
  fun writeIsAtomic() =
    runBlocking<Unit> {
      store.saveDesign(design("a", name = "First"))
      store.saveDesign(design("a", name = "Second"))
      val names = root.resolve("designs").listDirectoryEntries().map { it.fileName.toString() }
      assertEquals(listOf("a.graincraft.json"), names)
      assertTrue(file("designs", "a").readText().contains("Second"))
      assertFalse(file("designs", "a").readText().contains("First"))
    }

  @Test
  fun unparseableFileIsListedNotDeleted() =
    runBlocking<Unit> {
      val f = file("designs", "x")
      f.parent.createDirectories()
      f.writeText("{")
      val before = f.readBytes()
      val listed = store.listDesigns()
      assertEquals(1, listed.size)
      assertIs<StoredDesign.Unreadable>(listed[0])
      assertEquals("x.graincraft.json", listed[0].fileName)
      assertContentEquals(before, f.readBytes())
    }

  @Test
  fun newerSchemaIsListedUnreadableAndNotOverwritten() =
    runBlocking<Unit> {
      val f = file("designs", "n")
      f.parent.createDirectories()
      val text = newerFileText(DesignJson.SCHEMA_VERSION + 1)
      f.writeText(text)
      val listed = store.listDesigns().single()
      assertEquals(
        "made by a newer GrainCraft Studio (file version ${DesignJson.SCHEMA_VERSION + 1})",
        (listed as StoredDesign.Unreadable).reason,
      )
      assertFailsWith<IllegalStateException> { store.saveDesign(design("n")) }
      assertEquals(text, f.readText())
      assertEquals(
        listOf("n.graincraft.json"),
        f.parent.listDirectoryEntries().map { it.fileName.toString() },
      )
    }

  @Test
  fun saveOverUnparseableFileIsAllowedButNewerIsNot() =
    runBlocking<Unit> {
      val f = file("designs", "x")
      f.parent.createDirectories()
      f.writeText("{")
      store.saveDesign(design("x"))
      assertIs<StoredDesign.Readable>(store.listDesigns().single())
    }

  @Test
  fun copiedFileIsListedAsUnreadable() =
    runBlocking<Unit> {
      store.saveDesign(design("a"))
      val copy = root.resolve("designs").resolve("a copy.graincraft.json")
      Files.copy(file("designs", "a"), copy)
      val listed = store.listDesigns()
      assertEquals(
        listOf("a.graincraft.json", "a copy.graincraft.json"),
        listed.map { it.fileName },
      )
      assertEquals(design("a"), (listed[0] as StoredDesign.Readable).design)
      val reason = (listed[1] as StoredDesign.Unreadable).reason
      assertTrue("does not match its file name" in reason, reason)
      assertEquals(file("designs", "a").readText(), copy.readText())
    }

  @Test
  fun saveNeverReplacesAFileHoldingAnotherDesign() =
    runBlocking<Unit> {
      store.saveDesign(design("b"))
      val text = file("designs", "b").readText()
      file("designs", "a").writeText(text)
      assertFailsWith<IllegalStateException> { store.saveDesign(design("a")) }
      assertEquals(text, file("designs", "a").readText())
    }

  @Test
  fun unsafeIdsAreRejected() =
    runBlocking<Unit> {
      assertFailsWith<IllegalArgumentException> { store.saveDesign(design("../evil")) }
      assertFailsWith<IllegalArgumentException> { store.trashDesign("a/b") }
      assertFailsWith<IllegalArgumentException> { store.restoreDesign("") }
      assertFalse(root.parent.resolve("evil.graincraft.json").exists())
    }
}

class WorkspaceStoreTrashTest : StoreTestBase() {
  @Test
  fun trashAndRestore() =
    runBlocking<Unit> {
      store.saveDesign(design("a"))
      store.trashDesign("a")
      assertTrue(store.listDesigns().isEmpty())
      assertTrue(file("trash", "a").exists())
      assertEquals(now, file("trash", "a").getLastModifiedTime().toMillis())
      store.restoreDesign("a")
      assertFalse(file("trash", "a").exists())
      assertEquals(1, store.listDesigns().size)
    }

  @Test
  fun restoreNeverOverwritesALiveDesign() =
    runBlocking<Unit> {
      store.saveDesign(design("a", name = "Old"))
      store.trashDesign("a")
      store.saveDesign(design("a", name = "New"))
      assertFailsWith<IllegalStateException> { store.restoreDesign("a") }
      assertTrue(file("trash", "a").readText().contains("Old"))
      assertTrue(file("designs", "a").readText().contains("New"))
    }

  @Test
  fun trashOverExistingTrashDoesNotLoseEither() =
    runBlocking<Unit> {
      store.saveDesign(design("a", name = "First"))
      store.trashDesign("a")
      store.saveDesign(design("a", name = "Second"))
      assertFailsWith<IllegalStateException> { store.trashDesign("a") }
      assertTrue(file("trash", "a").readText().contains("First"))
      assertTrue(file("designs", "a").readText().contains("Second"))
    }

  @Test
  fun trashingOrRestoringMissingDesignDoesNothing() =
    runBlocking<Unit> {
      store.trashDesign("nope")
      store.restoreDesign("nope")
      assertTrue(store.listDesigns().isEmpty())
    }

  @Test
  fun purgeRemovesOnlyOldTrash() =
    runBlocking<Unit> {
      val day = 24L * 60 * 60 * 1000
      store.saveDesign(design("old"))
      store.saveDesign(design("recent"))
      now = 100 * day
      store.trashDesign("old")
      now = 125 * day
      store.trashDesign("recent")
      store.purgeTrash(131 * day) // old is 31 days, recent is 6
      assertFalse(file("trash", "old").exists())
      assertTrue(file("trash", "recent").exists())
      store.purgeTrash(155 * day) // recent is exactly 30 days: kept
      assertTrue(file("trash", "recent").exists())
      store.purgeTrash(155 * day + 1)
      assertFalse(file("trash", "recent").exists())
    }
}

class WorkspaceStoreFilesTest : StoreTestBase() {
  @Test
  fun defaultsWhenMissing() =
    runBlocking<Unit> {
      assertEquals(ShopProfile(), store.loadShopProfile())
      assertEquals(emptyList(), store.loadCustomWoods())
      assertEquals(AppSettings(), store.loadSettings())
      assertEquals(
        AppSettings(LengthUnit.INCH, FractionPrecision.SIXTEENTH, ThemeChoice.SYSTEM, null),
        AppSettings(),
      )
      assertTrue(store.listDesigns().isEmpty())
      assertFalse(root.resolve("settings.json").exists())
    }

  @Test
  fun profileWoodsAndSettingsRoundTrip() =
    runBlocking<Unit> {
      val profile = ShopProfile()
      store.saveShopProfile(profile)
      assertEquals(profile, store.loadShopProfile())
      val woods = listOf(WoodCatalog.builtIns.first().copy(id = "mine", custom = true))
      store.saveCustomWoods(woods)
      assertEquals(woods, store.loadCustomWoods())
      val settings =
        AppSettings(
          LengthUnit.MILLIMETRE,
          FractionPrecision.THIRTY_SECOND,
          ThemeChoice.DARK,
          WindowPlacement(1, 2, 3, 4),
        )
      store.saveSettings(settings)
      assertEquals(settings, store.loadSettings())
      assertTrue(root.listDirectoryEntries().none { it.fileName.toString().endsWith(".tmp") })
    }

  @Test
  fun everySettingsFileCarriesItsSchemaVersion() =
    runBlocking<Unit> {
      store.saveSettings(AppSettings(theme = ThemeChoice.DARK))
      store.saveShopProfile(ShopProfile())
      store.saveCustomWoods(listOf(WoodCatalog.builtIns.first().copy(id = "mine", custom = true)))
      for (name in listOf("settings.json", "shop-profile.json", "custom-woods.json")) {
        val text = root.resolve(name).readText()
        assertTrue(text.contains("\"schemaVersion\": 1"), "$name: $text")
      }
      assertTrue(root.resolve("custom-woods.json").readText().contains("\"woods\""))
      assertEquals("mine", store.loadCustomWoods().single().id)
      assertEquals(ThemeChoice.DARK, store.loadSettings().theme)
    }

  @Test
  fun filesWithoutASchemaVersionAreVersionOne() =
    runBlocking<Unit> {
      root.resolve("settings.json").writeText("""{"theme": "LIGHT"}""")
      val wood = WoodCatalog.builtIns.first().copy(id = "mine", custom = true)
      root
        .resolve("custom-woods.json")
        .writeText(
          WorkspaceJson.json.encodeToString(ListSerializer(WoodSpecies.serializer()), listOf(wood)),
        )
      assertEquals(ThemeChoice.LIGHT, store.loadSettings().theme)
      assertEquals(listOf(wood), store.loadCustomWoods())
      assertTrue(store.notices.pending.value.isEmpty())
    }

  private fun keptAside(stem: String) = root.resolve("$stem.unreadable-$now.json")

  @Test
  fun newerSchemaProfileIsNotOverwritten() =
    runBlocking<Unit> {
      val text = """{"schemaVersion": 2, "kerf": 1}"""
      root.resolve("shop-profile.json").writeText(text)
      assertEquals(ShopProfile(), store.loadShopProfile())
      assertEquals(text, keptAside("shop-profile").readText())
      assertEquals(
        listOf(
          "Couldn't read your shop profile; it was kept as shop-profile.unreadable-$now.json",
        ),
        store.notices.pending.value,
      )
      store.saveShopProfile(ShopProfile(kerf = Length.inches(0, 3, 32)))
      assertEquals(text, keptAside("shop-profile").readText())
      assertEquals(Length.inches(0, 3, 32), store.loadShopProfile().kerf)
    }

  @Test
  fun corruptCustomWoodsArePreserved() =
    runBlocking<Unit> {
      root.resolve("custom-woods.json").writeText("{")
      assertEquals(emptyList(), store.loadCustomWoods())
      assertFalse(root.resolve("custom-woods.json").exists())
      assertEquals("{", keptAside("custom-woods").readText())
      assertEquals(
        listOf("Couldn't read your custom woods; it was kept as custom-woods.unreadable-$now.json"),
        store.notices.pending.value,
      )
    }

  @Test
  fun corruptOrNewerSettingsArePreserved() =
    runBlocking<Unit> {
      root.resolve("settings.json").writeText("""{"schemaVersion": 7}""")
      assertEquals(AppSettings(), store.loadSettings())
      assertEquals("""{"schemaVersion": 7}""", keptAside("settings").readText())
      store.saveSettings(AppSettings(theme = ThemeChoice.LIGHT))
      assertEquals(ThemeChoice.LIGHT, store.loadSettings().theme)
      assertEquals(
        listOf("Couldn't read your settings; it was kept as settings.unreadable-$now.json"),
        store.notices.pending.value,
      )
      store.notices.dismiss(store.notices.pending.value.single())
      assertTrue(store.notices.pending.value.isEmpty())
    }
}
