package com.jaygindin.graincraft.core.audit

import com.jaygindin.graincraft.core.Examples
import com.jaygindin.graincraft.core.Examples.inch
import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.design.EdgeTreatments
import com.jaygindin.graincraft.core.design.JuiceGroove
import com.jaygindin.graincraft.core.design.fitsOn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EdgeAuditorTest {
  private val board = Dimensions(inch(16), inch(4), inch(1, 1, 2))

  private fun groove(width: Int, inset: Int = 0, d: Int = 8) =
    JuiceGroove(inch(0, width, d), inch(0, 1, 4), inch(0, inset, d))

  @Test
  fun aGrooveFitsWhileWoodIsLeftInsideIt() {
    // 4" wide: 1/2" in and 1/2" wide leaves 2" inside.
    assertTrue(groove(width = 4, inset = 4).fitsOn(board.length, board.width))
    // 1/2" in and 3" wide meets in the middle, with nothing left inside.
    assertFalse(JuiceGroove(inch(3), inch(0, 1, 4), inch(0, 1, 2)).fitsOn(inch(16), inch(4)))
    assertFalse(JuiceGroove(inch(1, 1, 2), inch(0, 1, 4), inch(0, 1, 2)).fitsOn(inch(16), inch(4)))
  }

  @Test
  fun aGrooveThatDoesNotFitIsReported() {
    val edges = EdgeTreatments(juiceGroove = JuiceGroove(inch(3), inch(0, 1, 4), inch(0, 1, 2)))
    val issues = auditEdges(edges, board, Examples.fmt)
    val issue = issues.single()
    assertEquals(IssueCode.JUICE_GROOVE_DOES_NOT_FIT, issue.code)
    assertEquals(Severity.WARNING, issue.severity)
    assertEquals("The juice groove doesn't fit.", issue.title)
    assertTrue("4\"" in issue.detail && "7\"" in issue.detail, issue.detail)
  }

  @Test
  fun aFittingOrMissingGrooveIsNotReported() {
    assertTrue(auditEdges(EdgeTreatments(), board, Examples.fmt).isEmpty())
    val fits = EdgeTreatments(juiceGroove = groove(width = 4, inset = 4))
    assertTrue(auditEdges(fits, board, Examples.fmt).isEmpty())
  }
}
