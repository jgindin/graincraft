package com.jaygindin.graincraft.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.jaygindin.graincraft.app.ui.theme.GrainCraftTheme

@Composable
fun App() {
  GrainCraftTheme(dark = isSystemInDarkTheme()) { Text("GrainCraft Studio") }
}
