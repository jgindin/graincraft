package com.jaygindin.graincraft.app

import graincraft.app.generated.resources.Res
import graincraft.app.generated.resources.ic_carpenter
import graincraft.app.generated.resources.ic_forest
import graincraft.app.generated.resources.ic_grid_view
import graincraft.app.generated.resources.ic_settings
import org.jetbrains.compose.resources.DrawableResource

/** The four places the rail switches between, top to bottom. */
enum class Destination(val label: String) {
  DESIGNS("Designs"),
  WOODS("Woods"),
  SHOP("Shop"),
  SETTINGS("Settings"),
}

/** `designs`, `woods`, ...: the suffix of the rail item's and the screen's test tags. */
internal val Destination.tagName: String
  get() = name.lowercase()

internal val Destination.icon: DrawableResource
  get() =
    when (this) {
      Destination.DESIGNS -> Res.drawable.ic_grid_view
      Destination.WOODS -> Res.drawable.ic_forest
      Destination.SHOP -> Res.drawable.ic_carpenter
      Destination.SETTINGS -> Res.drawable.ic_settings
    }
