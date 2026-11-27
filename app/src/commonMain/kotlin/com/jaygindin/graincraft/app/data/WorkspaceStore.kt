package com.jaygindin.graincraft.app.data

import com.jaygindin.graincraft.core.design.BoardDesign
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.wood.WoodSpecies

/** One file in the designs folder: a design, or the reason it cannot be opened. */
sealed interface StoredDesign {
  val fileName: String

  data class Readable(override val fileName: String, val design: BoardDesign) : StoredDesign

  data class Unreadable(override val fileName: String, val reason: String) : StoredDesign
}

/** Everything the app keeps on disk. Files that cannot be understood are left untouched. */
interface WorkspaceStore {
  /** Where the data lives, for showing the user. */
  val rootDescription: String

  /** Readable designs newest first, then unreadable files by name. */
  suspend fun listDesigns(): List<StoredDesign>

  /**
   * Writes the design atomically.
   *
   * @throws IllegalStateException if the file on disk was made by a newer app version.
   */
  suspend fun saveDesign(design: BoardDesign)

  /**
   * Moves a design to the trash.
   *
   * @throws IllegalStateException if the trash already holds a design with this id.
   */
  suspend fun trashDesign(id: String)

  /**
   * Moves a design out of the trash.
   *
   * @throws IllegalStateException if a design with this id already exists.
   */
  suspend fun restoreDesign(id: String)

  /** Deletes trashed designs that have been in the trash for more than 30 days. */
  suspend fun purgeTrash(nowMillis: Long)

  suspend fun loadShopProfile(): ShopProfile

  suspend fun saveShopProfile(p: ShopProfile)

  suspend fun loadCustomWoods(): List<WoodSpecies>

  suspend fun saveCustomWoods(w: List<WoodSpecies>)

  suspend fun loadSettings(): AppSettings

  suspend fun saveSettings(s: AppSettings)
}
