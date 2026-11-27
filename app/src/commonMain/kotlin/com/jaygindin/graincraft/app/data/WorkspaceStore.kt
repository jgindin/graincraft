package com.jaygindin.graincraft.app.data

import com.jaygindin.graincraft.core.design.BoardDesign
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.wood.WoodSpecies
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** One file in the designs folder: a design, or the reason it cannot be opened. */
sealed interface StoredDesign {
  val fileName: String

  data class Readable(override val fileName: String, val design: BoardDesign) : StoredDesign

  data class Unreadable(override val fileName: String, val reason: String) : StoredDesign
}

private val SAFE_ID = Regex("[A-Za-z0-9][A-Za-z0-9._-]*")

/**
 * True when [id] can name a design's file: letters, digits, `.`, `_` and `-`, starting with a
 * letter or digit, so it can never leave the designs folder.
 */
fun isValidDesignId(id: String): Boolean = SAFE_ID.matches(id)

/** Notices for the user from the store, each shown once and then dismissed. */
class StoreNotices {
  private val state = MutableStateFlow(emptyList<String>())
  val pending: StateFlow<List<String>> = state.asStateFlow()

  fun add(notice: String) = state.update { it + notice }

  fun dismiss(notice: String) = state.update { it - notice }
}

/** Everything the app keeps on disk. Files that cannot be understood are left untouched. */
interface WorkspaceStore {
  /** Where the data lives, for showing the user. */
  val rootDescription: String

  /** One sentence for each settings file that could not be read and was kept aside. */
  val notices: StoreNotices

  /**
   * Readable designs newest first, then unreadable files by name. A design is listed by its file
   * name, so a file whose id does not match its name (a copy made by hand) is unreadable, and no
   * two readable designs share an id.
   */
  suspend fun listDesigns(): List<StoredDesign>

  /**
   * Writes the design atomically.
   *
   * @throws IllegalStateException if the file on disk was made by a newer app version or holds
   *   another design.
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
