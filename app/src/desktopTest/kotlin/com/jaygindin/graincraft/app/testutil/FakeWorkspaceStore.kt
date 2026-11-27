package com.jaygindin.graincraft.app.testutil

import com.jaygindin.graincraft.app.data.AppSettings
import com.jaygindin.graincraft.app.data.StoreNotices
import com.jaygindin.graincraft.app.data.StoredDesign
import com.jaygindin.graincraft.app.data.WorkspaceStore
import com.jaygindin.graincraft.core.design.BoardDesign
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.wood.WoodSpecies

private const val TRASH_MILLIS = 30L * 24 * 60 * 60 * 1000

/**
 * An in-memory [WorkspaceStore] that keeps every value it is given and records every save, so tests
 * can both script what is "on disk" and check what the app wrote.
 */
class FakeWorkspaceStore(
  var settings: AppSettings = AppSettings(),
  var shopProfile: ShopProfile = ShopProfile(),
  var customWoods: List<WoodSpecies> = emptyList(),
  private val clock: () -> Long = { 0L },
  override val rootDescription: String = "/home/maker/.local/share/graincraft-studio",
) : WorkspaceStore {
  override val notices = StoreNotices()

  /** Designs by id. */
  val designs = linkedMapOf<String, BoardDesign>()

  /** Trashed designs by id, with the time they were trashed. */
  val trash = linkedMapOf<String, Pair<BoardDesign, Long>>()

  /** Files that cannot be opened, listed after the readable designs. */
  val unreadable = mutableListOf<StoredDesign.Unreadable>()

  /** When set, every design write (save, trash, restore) throws it, as a full disk would. */
  var writeFailure: Exception? = null

  val savedSettings = mutableListOf<AppSettings>()
  val savedDesigns = mutableListOf<BoardDesign>()
  val savedShopProfiles = mutableListOf<ShopProfile>()
  val savedCustomWoods = mutableListOf<List<WoodSpecies>>()

  /** How many times the designs were listed, once for each library refresh. */
  var listCount = 0
    private set

  override suspend fun listDesigns(): List<StoredDesign> =
    also { listCount++ }
      .designs
      .values
      .sortedByDescending { it.updatedAtMillis }
      .map { StoredDesign.Readable("${it.id}.graincraft.json", it) } +
      unreadable.sortedBy { it.fileName }

  override suspend fun saveDesign(design: BoardDesign) {
    writeFailure?.let { throw it }
    savedDesigns += design
    designs[design.id] = design
  }

  override suspend fun trashDesign(id: String) {
    writeFailure?.let { throw it }
    val design = designs.remove(id) ?: return
    check(id !in trash) { "The trash already holds $id" }
    trash[id] = design to clock()
  }

  override suspend fun restoreDesign(id: String) {
    writeFailure?.let { throw it }
    val (design, _) = trash[id] ?: return
    check(id !in designs) { "A design $id already exists" }
    trash.remove(id)
    designs[id] = design
  }

  override suspend fun purgeTrash(nowMillis: Long) {
    trash.entries.removeAll { (_, entry) -> nowMillis - entry.second > TRASH_MILLIS }
  }

  override suspend fun loadShopProfile(): ShopProfile = shopProfile

  override suspend fun saveShopProfile(p: ShopProfile) {
    savedShopProfiles += p
    shopProfile = p
  }

  override suspend fun loadCustomWoods(): List<WoodSpecies> = customWoods

  override suspend fun saveCustomWoods(w: List<WoodSpecies>) {
    savedCustomWoods += w
    customWoods = w
  }

  override suspend fun loadSettings(): AppSettings = settings

  override suspend fun saveSettings(s: AppSettings) {
    savedSettings += s
    settings = s
  }
}
