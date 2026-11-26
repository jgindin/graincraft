package com.jaygindin.graincraft.core

const val CORE_VERSION_MARKER = "graincraft-core"

// Gives Kover a line to measure so the coverage gate is real from the first build.
fun coreVersionMarker(): String = CORE_VERSION_MARKER
