package com.jaygindin.graincraft.app

import com.jaygindin.graincraft.app.data.WorkspaceStore
import com.jaygindin.graincraft.app.platform.Clock
import com.jaygindin.graincraft.app.platform.ExternalBrowser
import com.jaygindin.graincraft.app.platform.FileDialogs
import com.jaygindin.graincraft.app.ui.preview.ArgbImageFactory

/**
 * The platform services every screen may need, built once by the entry point and passed down; tests
 * build one from fakes.
 *
 * @param newId a fresh design id, safe to use as a file name.
 * @param images turns grain texture pixels into bitmaps for the board preview.
 */
class AppGraph(
  val store: WorkspaceStore,
  val dialogs: FileDialogs,
  val browser: ExternalBrowser,
  val clock: Clock,
  val newId: () -> String,
  val images: ArgbImageFactory,
)
