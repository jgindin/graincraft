package com.jaygindin.graincraft.app

import com.jaygindin.graincraft.app.data.WindowPlacement
import java.awt.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WindowPlacementTest {
  private val screens = listOf(Rectangle(0, 0, 1920, 1080), Rectangle(1920, 0, 2560, 1440))

  @Test
  fun noSavedPlacementGivesNone() {
    assertNull(restorablePlacement(null, screens))
  }

  @Test
  fun placementOnAScreenIsKept() {
    val onSecond = WindowPlacement(2000, 100, 1280, 800)
    assertEquals(onSecond, restorablePlacement(onSecond, screens))
  }

  @Test
  fun placementPartlyOnAScreenIsKept() {
    val overlapping = WindowPlacement(-600, -200, 1280, 800)
    assertEquals(overlapping, restorablePlacement(overlapping, screens))
  }

  @Test
  fun placementOffEveryScreenIsIgnored() {
    // Left over from a third monitor that is unplugged.
    assertNull(restorablePlacement(WindowPlacement(4600, 0, 1280, 800), screens))
    assertNull(restorablePlacement(WindowPlacement(0, 1500, 1280, 800), screens))
  }
}
