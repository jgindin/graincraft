package com.jaygindin.graincraft.app.platform

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.dialogs.openFileSaver
import java.awt.Desktop
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The desktop portal's file dialogs, through FileKit. */
class DesktopFileDialogs : FileDialogs {
  override suspend fun openDesignFile(): String? {
    // A design file ends in .graincraft.json, whose last extension is json.
    val picked = FileKit.openFilePicker(type = FileKitType.File("json")) ?: return null
    return withContext(Dispatchers.IO) { picked.file.readText() }
  }

  override suspend fun saveFile(
    suggestedName: String,
    extension: String,
    contents: String,
  ): SaveResult {
    val picked =
      FileKit.openFileSaver(suggestedName, defaultExtension = extension)
        ?: return SaveResult.Cancelled
    return withContext(Dispatchers.IO) { writeFile(picked.file, contents) }
  }
}

/** Writes [contents] to [file], reporting a failure (a full disk, no permission) as a result. */
internal fun writeFile(file: File, contents: String): SaveResult =
  try {
    file.writeText(contents)
    SaveResult.Saved(file.absolutePath)
  } catch (e: IOException) {
    SaveResult.Failed(e.message ?: e.javaClass.simpleName)
  }

/** Opens files and folders with `xdg-open`, falling back to AWT where that is missing. */
class DesktopExternalBrowser : ExternalBrowser {
  override fun openFile(path: String) = open(path)

  override fun openFolder(path: String) {
    // The folder may not exist until the first save; create it so the file manager can show it.
    File(path).mkdirs()
    open(path)
  }

  private fun open(path: String) {
    try {
      ProcessBuilder("xdg-open", path).start()
    } catch (_: IOException) {
      // Desktop.open blocks until the handler starts, so it runs off the UI thread.
      if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
        Thread {
          try {
            Desktop.getDesktop().open(File(path))
          } catch (_: IOException) {
            // No app on this desktop opens it; the user still has the path the app showed them.
          }
        }
          .apply { isDaemon = true }
          .start()
      }
    }
  }
}
