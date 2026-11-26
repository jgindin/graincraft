package com.jaygindin.graincraft.core

import kotlin.test.Test
import kotlin.test.assertEquals

class CoreSmokeTest {
  @Test
  fun markerIsSet() {
    assertEquals("graincraft-core", CORE_VERSION_MARKER)
    assertEquals("graincraft-core", coreVersionMarker())
  }
}
