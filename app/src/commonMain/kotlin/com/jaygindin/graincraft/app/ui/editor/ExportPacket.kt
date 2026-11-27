package com.jaygindin.graincraft.app.ui.editor

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.jaygindin.graincraft.app.platform.ExternalBrowser
import com.jaygindin.graincraft.app.platform.FileDialogs
import com.jaygindin.graincraft.app.platform.SaveResult
import com.jaygindin.graincraft.core.compile.CompiledDesign
import com.jaygindin.graincraft.core.packet.ShopPacketExporter
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.UnitFormatter
import com.jaygindin.graincraft.core.wood.Woods
import kotlinx.coroutines.launch

private const val PACKET_EXTENSION = "html"

/**
 * Builds the shop packet for [compiled] and lets the user save it, starting from `<design name> -
 * shop packet.html`.
 */
suspend fun exportPacket(
  compiled: CompiledDesign,
  woods: Woods,
  profile: ShopProfile,
  formatter: UnitFormatter,
  dialogs: FileDialogs,
): SaveResult {
  val html = ShopPacketExporter.html(compiled, woods, profile, formatter)
  // The save dialog adds the extension itself.
  val name =
    ShopPacketExporter.defaultFileName(compiled.design.name).removeSuffix(".$PACKET_EXTENSION")
  return dialogs.saveFile(name, PACKET_EXTENSION, html)
}

/**
 * Export packet for [viewModel]'s design: the save dialog, then a "Packet saved" snackbar on
 * [snackbars] whose "Open in browser" shows the file, from which the user prints it, or a "Couldn't
 * save" snackbar with the reason.
 */
@Composable
internal fun rememberPacketExport(
  viewModel: EditorViewModel,
  dialogs: FileDialogs,
  browser: ExternalBrowser,
  snackbars: SnackbarHostState,
): () -> Unit {
  val scope = rememberCoroutineScope()
  // A second press while the save dialog is up would open another one.
  var exporting by remember { mutableStateOf(false) }
  return remember(viewModel, dialogs, browser, snackbars) {
    {
      if (!exporting) {
        exporting = true
        scope.launch {
          val saved =
            try {
              viewModel.exportPacket(dialogs)
            } finally {
              exporting = false
            }
          when (saved) {
            is SaveResult.Saved -> {
              val result =
                snackbars.showSnackbar(
                  "Packet saved",
                  actionLabel = "Open in browser",
                  duration = SnackbarDuration.Long,
                )
              if (result == SnackbarResult.ActionPerformed) browser.openFile(saved.path)
            }
            is SaveResult.Failed -> snackbars.showSnackbar("Couldn't save: ${saved.reason}")
            SaveResult.Cancelled -> Unit
          }
        }
      }
    }
  }
}
