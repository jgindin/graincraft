package com.jaygindin.graincraft.app.testutil

import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RoborazziOptions

/** The default window size, which every golden is taken at. */
const val SCREENSHOT_WIDTH = 1280
const val SCREENSHOT_HEIGHT = 800

/** A version that never changes, so goldens do not move with every release. */
const val SCREENSHOT_VERSION = "1.0"

/** Tolerates the few anti-aliased pixels that differ between machines. */
@OptIn(ExperimentalRoborazziApi::class)
val screenshotOptions =
  RoborazziOptions(
    compareOptions = RoborazziOptions.CompareOptions(changeThreshold = 0.001f),
  )
