package com.jaygindin.graincraft.app.testutil

import com.jaygindin.graincraft.app.AppGraph
import com.jaygindin.graincraft.app.platform.ExternalBrowser
import com.jaygindin.graincraft.app.platform.FileDialogs
import com.jaygindin.graincraft.app.platform.SaveResult
import com.jaygindin.graincraft.app.platform.SkiaArgbImageFactory
import kotlinx.coroutines.Dispatchers

/** File dialogs that return scripted answers and record what they were asked. */
class FakeFileDialogs : FileDialogs {
  data class Save(val suggestedName: String, val extension: String, val contents: String)

  /** What the next "open" returns: the file's text, or null for a cancelled dialog. */
  var openResult: String? = null

  /** What every "save" returns. */
  var saveResult: SaveResult = SaveResult.Saved("/home/maker/Documents/saved")

  /** When set, the next "open" throws it, as reading an unreadable file would. */
  var openFailure: Exception? = null

  var openCount = 0
    private set

  val saves = mutableListOf<Save>()

  override suspend fun openDesignFile(): String? {
    openCount++
    openFailure?.let { throw it }
    return openResult
  }

  override suspend fun saveFile(
    suggestedName: String,
    extension: String,
    contents: String,
  ): SaveResult {
    saves += Save(suggestedName, extension, contents)
    return saveResult
  }
}

/** Records every file and folder the app asks to show. */
class FakeExternalBrowser : ExternalBrowser {
  val openedFiles = mutableListOf<String>()
  val openedFolders = mutableListOf<String>()

  override fun openFile(path: String) {
    openedFiles += path
  }

  override fun openFolder(path: String) {
    openedFolders += path
  }
}

/** The platform fakes together, and an [AppGraph] built from them. */
class FakePlatform(
  val store: FakeWorkspaceStore = FakeWorkspaceStore(),
  var nowMillis: Long = 1_700_000_000_000L,
) {
  val dialogs = FakeFileDialogs()
  val browser = FakeExternalBrowser()
  private var nextId = 0

  /** Ids `id-1`, `id-2`, ... in order. */
  val graph: AppGraph =
    AppGraph(
      store,
      dialogs,
      browser,
      clock = { nowMillis },
      newId = { "id-${++nextId}" },
      images = SkiaArgbImageFactory,
      compute = Dispatchers.Unconfined,
    )
}
