package com.jaygindin.graincraft.app.data

import kotlin.test.Test
import kotlin.test.assertEquals

class XdgPathsTest {
  @Test
  fun usesXdgDataHomeWhenSet() {
    assertEquals(
      "/data/x/graincraft-studio",
      xdgDataRoot(mapOf("XDG_DATA_HOME" to "/data/x"), "/home/me"),
    )
  }

  @Test
  fun fallsBackToHomeLocalShare() {
    assertEquals(
      "/home/me/.local/share/graincraft-studio",
      xdgDataRoot(emptyMap(), "/home/me"),
    )
  }

  @Test
  fun emptyOrBlankXdgDataHomeCountsAsUnset() {
    val expected = "/home/me/.local/share/graincraft-studio"
    assertEquals(expected, xdgDataRoot(mapOf("XDG_DATA_HOME" to ""), "/home/me"))
    assertEquals(expected, xdgDataRoot(mapOf("XDG_DATA_HOME" to "  "), "/home/me"))
  }

  @Test
  fun trailingSlashIsNotDoubled() {
    assertEquals(
      "/data/x/graincraft-studio",
      xdgDataRoot(mapOf("XDG_DATA_HOME" to "/data/x/"), "/home/me"),
    )
  }
}
