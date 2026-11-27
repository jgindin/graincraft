package com.jaygindin.graincraft.app.data

import com.jaygindin.graincraft.core.design.BoardDesign
import com.jaygindin.graincraft.core.design.DesignDecode
import com.jaygindin.graincraft.core.design.DesignJson
import com.jaygindin.graincraft.core.design.WorkspaceJson
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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer

private const val DESIGN_SUFFIX = ".graincraft.json"
private const val TRASH_MILLIS = 30L * 24 * 60 * 60 * 1000
private val SAFE_ID = Regex("[A-Za-z0-9][A-Za-z0-9._-]*")

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

  override val rootDescription: String = this.root.toString()

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

  override suspend fun loadShopProfile(): ShopProfile =
    load("shop-profile.json", ShopProfile.serializer()) { ShopProfile() }

  override suspend fun saveShopProfile(p: ShopProfile) =
    save("shop-profile.json", ShopProfile.serializer(), p)

  override suspend fun loadCustomWoods(): List<WoodSpecies> =
    load("custom-woods.json", ListSerializer(WoodSpecies.serializer())) { emptyList() }

  override suspend fun saveCustomWoods(w: List<WoodSpecies>) =
    save("custom-woods.json", ListSerializer(WoodSpecies.serializer()), w)

  override suspend fun loadSettings(): AppSettings =
    load("settings.json", AppSettings.serializer()) { AppSettings() }

  override suspend fun saveSettings(s: AppSettings) =
    save("settings.json", AppSettings.serializer(), s)

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

  private suspend fun <T> load(name: String, serializer: KSerializer<T>, default: () -> T): T =
    withContext(Dispatchers.IO) {
      val file = root.resolve(name)
      try {
        if (Files.exists(file)) {
          WorkspaceJson.json.decodeFromString(serializer, Files.readString(file))
        } else {
          default()
        }
      } catch (_: IllegalArgumentException) {
        // Unparseable: fall back to defaults and leave the file for the next explicit save.
        default()
      } catch (_: IOException) {
        default()
      }
    }

  private suspend fun <T> save(name: String, serializer: KSerializer<T>, value: T) {
    withContext(Dispatchers.IO) {
      lock.withLock {
        writeAtomically(root.resolve(name), WorkspaceJson.json.encodeToString(serializer, value))
      }
    }
  }

  private fun readDesign(file: Path): StoredDesign {
    val name = file.fileName.toString()
    return when (val decoded = decodeOrNull(file)) {
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

  // Ids become file names, so anything that could leave the folder is refused.
  private fun designFileName(id: String): String {
    require(SAFE_ID.matches(id)) { "Not a valid design id: $id" }
    return "$id$DESIGN_SUFFIX"
  }
}
