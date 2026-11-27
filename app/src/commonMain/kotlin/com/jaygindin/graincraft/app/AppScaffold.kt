package com.jaygindin.graincraft.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.jaygindin.graincraft.app.ui.theme.Spacing
import org.jetbrains.compose.resources.painterResource

/**
 * The navigation rail on the left and the current destination's screen beside it. Each screen
 * brings its own top bar, since its actions differ.
 */
@Composable
internal fun AppScaffold(
  current: Destination,
  onSelect: (Destination) -> Unit,
  content: @Composable () -> Unit,
) {
  Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
    Row(modifier = Modifier.fillMaxSize()) {
      NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxHeight(),
      ) {
        Spacer(Modifier.height(Spacing.md))
        Destination.entries.forEach { destination ->
          NavigationRailItem(
            selected = destination == current,
            onClick = { onSelect(destination) },
            icon = { Icon(painterResource(destination.icon), contentDescription = null) },
            label = { Text(destination.label) },
            modifier = Modifier.testTag("rail-${destination.tagName}"),
          )
        }
      }
      Box(modifier = Modifier.weight(1f).fillMaxHeight()) { content() }
    }
  }
}

/** Stands in for a destination's screen until it is built: just its name, centred. */
@Composable
internal fun PlaceholderScreen(destination: Destination) {
  Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier.fillMaxSize().testTag("screen-${destination.tagName}"),
  ) {
    Text(destination.label, style = MaterialTheme.typography.headlineMedium)
  }
}
