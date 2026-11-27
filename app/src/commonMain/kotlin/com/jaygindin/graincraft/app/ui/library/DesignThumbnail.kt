package com.jaygindin.graincraft.app.ui.library

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.jaygindin.graincraft.app.ui.preview.drawFlatBoard
import com.jaygindin.graincraft.app.ui.theme.Spacing
import com.jaygindin.graincraft.core.compile.CompiledDesign
import com.jaygindin.graincraft.core.wood.Woods
import graincraft.app.generated.resources.Res
import graincraft.app.generated.resources.ic_error
import org.jetbrains.compose.resources.painterResource

/** A small flat top view of the board, or an error icon when the design does not compile. */
@Composable
internal fun DesignThumbnail(
  compiled: CompiledDesign?,
  woods: Woods,
  modifier: Modifier = Modifier,
) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier.testTag("thumbnail"),
  ) {
    if (compiled == null) {
      Icon(
        painterResource(Res.drawable.ic_error),
        contentDescription = "Cannot show this design",
        tint = MaterialTheme.colorScheme.error,
      )
    } else {
      val allWoods = woods.withExtra(compiled.design.embeddedCustomWoods)
      Canvas(Modifier.fillMaxSize().padding(Spacing.sm)) {
        drawFlatBoard(compiled.layout, allWoods)
      }
    }
  }
}
