package com.jaygindin.graincraft.app.data

import com.jaygindin.graincraft.core.design.BoardDesign
import com.jaygindin.graincraft.core.design.DesignDecode
import com.jaygindin.graincraft.core.design.DesignJson
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.wood.WoodSpecies
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.attribute.FileTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer

private const val DESIGN_SUFFIX = ".graincraft.json"
private const val TRASH_MILLIS = 30L * 24 * 60 * 60 * 1000

private val SHOP_PROFILE =
  SettingsFile("shop-profile", "shop profile", ShopProfile.serializer()) { ShopProfile() }
private val CUSTOM_WOODS =
  SettingsFile(
    "custom-woods",
    "custom woods",
    ListSerializer(WoodSpecies.serializer()),
    listKey = "woods",
  ) {
    emptyList()
  }
private val SETTINGS =
  SettingsFile("settings", "settings", AppSettings.serializer()) { AppSettings() }

/**
 * Stores everything as files under [root]. Writes go to a temp file in the same folder and are
 * renamed into place, and no operation overwrites a file it cannot read.
 */
class FileWorkspaceStore(root: Path, private val clock: () -> Long) : WorkspaceStore {
  private val root: Path = root.toAbsolutePath()
  private val designsDir = this.root.resolve("designs")
  private val trashDir = this.root.resolve("trash")

  // Serialises every write so two saves never share one temp file.
  private val lock = Mutex()

  // Settings files that could neither be read nor moved aside, and so are never written.
  private val blocked = mutableSetOf<String>()

  override val rootDescription: String = this.root.toString()

  override val notices = StoreNotices()

  override suspend fun listDesigns(): List<StoredDesign> =
    withContext(Dispatchers.IO) {
      if (!Files.isDirectory(designsDir)) return@withContext emptyList()
      val stored =
        Files.newDirectoryStream(designsDir, "*$DESIGN_SUFFIX").use { dir ->
          dir.filter { Files.isRegularFile(it) }.map { readDesign(it) }
        }
      val readable =
        stored.filterIsInstance<StoredDesign.Readable>().sortedByDescending {
          it.design.updatedAtMillis
        }
      val unreadable = stored.filterIsInstance<StoredDesign.Unreadable>().sortedBy { it.fileName }
      readable + unreadable
    }

  override suspend fun saveDesign(design: BoardDesign) {
    val target = designsDir.resolve(designFileName(design.id))
    withContext(Dispatchers.IO) {
      lock.withLock {
        if (Files.exists(target)) {
          val existing = decodeOrNull(target)
          check(existing !is DesignDecode.NewerSchema) {
            "${target.fileName} was made by a newer GrainCraft Studio and is not overwritten"
          }
          check(existing !is DesignDecode.Ok || existing.design.id == design.id) {
            "${target.fileName} holds another design and is not overwritten"
          }
        }
        writeAtomically(target, DesignJson.encode(design))
      }
    }
  }

  override suspend fun trashDesign(id: String) {
    val name = designFileName(id)
    move(designsDir.resolve(name), trashDir.resolve(name), "The trash already holds $name") {
      Files.setLastModifiedTime(it, FileTime.fromMillis(clock()))
    }
  }

  override suspend fun restoreDesign(id: String) {
    val name = designFileName(id)
    move(trashDir.resolve(name), designsDir.resolve(name), "A design $name already exists") {}
  }

  override suspend fun purgeTrash(nowMillis: Long) {
    withContext(Dispatchers.IO) {
      lock.withLock {
        if (!Files.isDirectory(trashDir)) return@withLock
        Files.newDirectoryStream(trashDir, "*$DESIGN_SUFFIX").use { dir ->
          for (file in dir) {
            val trashedAt = Files.getLastModifiedTime(file).toMillis()
            if (nowMillis - trashedAt > TRASH_MILLIS) Files.deleteIfExists(file)
          }
        }
      }
    }
  }

  override suspend fun loadShopProfile(): ShopProfile = load(SHOP_PROFILE)

  override suspend fun saveShopProfile(p: ShopProfile) = save(SHOP_PROFILE, p)

  override suspend fun loadCustomWoods(): List<WoodSpecies> = load(CUSTOM_WOODS)

  override suspend fun saveCustomWoods(w: List<WoodSpecies>) = save(CUSTOM_WOODS, w)

  override suspend fun loadSettings(): AppSettings = load(SETTINGS)

  override suspend fun saveSettings(s: AppSettings) = save(SETTINGS, s)

  /** Moves [from] to [to] unless [to] exists, then runs [after] on the moved file. */
  private suspend fun move(from: Path, to: Path, clash: String, after: (Path) -> Unit) {
    withContext(Dispatchers.IO) {
      lock.withLock {
        if (!Files.exists(from)) return@withLock
        // A rename would silently replace the target, so refuse first.
        check(!Files.exists(to)) { clash }
        Files.createDirectories(to.parent)
        Files.move(from, to, ATOMIC_MOVE)
        after(to)
      }
    }
  }

  /**
   * The file's value, or [SettingsFile.default] when it is missing. A file that cannot be read, or
   * that a newer version wrote, is renamed aside first, so the next save cannot overwrite it, and
   * the user is told where it went.
   */
  private suspend fun <T> load(kind: SettingsFile<T>): T =
    withContext(Dispatchers.IO) {
      lock.withLock {
        val file = root.resolve(kind.fileName)
        if (!Files.exists(file)) return@withLock kind.default()
        val text =
          try {
            Files.readString(file)
          } catch (_: IOException) {
            null
          }
        text?.let { kind.decode(it) } ?: kind.default().also { keepAside(file, kind) }
      }
    }

  private fun keepAside(file: Path, kind: SettingsFile<*>) {
    val kept = file.resolveSibling("${kind.stem}.unreadable-${clock()}.json")
    val notice =
      try {
        Files.move(file, kept, ATOMIC_MOVE)
        "Couldn't read your ${kind.label}; it was kept as ${kept.fileName}"
      } catch (_: IOException) {
        // It cannot be moved, so it must not be written over either.
        blocked += kind.fileName
        "Couldn't read your ${kind.label} (${file.fileName}); changes to it won't be saved"
      }
    notices.add(notice)
  }

  private suspend fun <T> save(kind: SettingsFile<T>, value: T) {
    withContext(Dispatchers.IO) {
      lock.withLock {
        if (kind.fileName in blocked) return@withLock
        writeAtomically(root.resolve(kind.fileName), kind.encode(value))
      }
    }
  }

  private fun readDesign(file: Path): StoredDesign {
    val name = file.fileName.toString()
    return when (val decoded = decodeOrNull(file)) {
      // The file name is the design's identity; a copy keeps the original's id inside it.
      is DesignDecode.Ok if !isNamedFor(name, decoded.design.id) ->
        StoredDesign.Unreadable(
          name,
          "Its id \"${decoded.design.id}\" does not match its file name, so it may be a copy " +
            "of another design. Import it to add it as a new design.",
        )
      is DesignDecode.Ok -> StoredDesign.Readable(name, decoded.design)
      is DesignDecode.NewerSchema ->
        StoredDesign.Unreadable(
          name,
          "made by a newer GrainCraft Studio (file version ${decoded.version})",
        )
      is DesignDecode.Invalid -> StoredDesign.Unreadable(name, decoded.reason)
      null -> StoredDesign.Unreadable(name, "The file could not be read")
    }
  }

  /** Null when the file cannot be read at all. */
  private fun decodeOrNull(file: Path): DesignDecode? =
    try {
      DesignJson.decode(Files.readString(file))
    } catch (_: IOException) {
      null
    }

  private fun writeAtomically(target: Path, text: String) {
    Files.createDirectories(target.parent)
    val temp = target.resolveSibling("${target.fileName}.tmp")
    try {
      Files.writeString(temp, text)
      Files.move(temp, target, ATOMIC_MOVE, REPLACE_EXISTING)
    } finally {
      Files.deleteIfExists(temp)
    }
  }

  private fun isNamedFor(fileName: String, id: String): Boolean =
    isValidDesignId(id) && fileName == "$id$DESIGN_SUFFIX"

  // Ids become file names, so anything that could leave the folder is refused.
  private fun designFileName(id: String): String {
    require(isValidDesignId(id)) { "Not a valid design id: $id" }
    return "$id$DESIGN_SUFFIX"
  }
}
