package com.jaygindin.graincraft.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import graincraft.app.generated.resources.Res
import graincraft.app.generated.resources.ic_arrow_back
import org.jetbrains.compose.resources.painterResource

/** Stands in for the design editor until it is built. */
@OptIn(ExperimentalMaterial3Api::class) // TopAppBar is still experimental in Material 3
@Composable
internal fun EditorStub(onBack: () -> Unit) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Design") },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("editor-back")) {
            Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = "Back")
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
      )
    },
    containerColor = MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxSize().testTag("screen-editor"),
  ) { padding ->
    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(padding).fillMaxSize()) {
      Text("Editor coming", style = MaterialTheme.typography.headlineMedium)
    }
  }
}
