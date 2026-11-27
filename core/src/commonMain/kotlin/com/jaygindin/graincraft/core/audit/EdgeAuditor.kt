package com.jaygindin.graincraft.core.audit

import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.design.EdgeTreatments
import com.jaygindin.graincraft.core.design.fitsOn
import com.jaygindin.graincraft.core.design.minimumSide
import com.jaygindin.graincraft.core.units.UnitFormatter
import com.jaygindin.graincraft.core.units.min

/**
 * Warnings about the edge details on a board of size [finished]. A groove that does not fit is a
 * warning, not an error, so the rest of the build plan still shows while the user fixes it.
 */
fun auditEdges(edges: EdgeTreatments, finished: Dimensions, formatter: UnitFormatter): List<Issue> {
  val groove =
    edges.juiceGroove?.takeUnless { it.fitsOn(finished.length, finished.width) }
      ?: return emptyList()
  val side = min(finished.length, finished.width)
  return listOf(
    Issue(
      IssueCode.JUICE_GROOVE_DOES_NOT_FIT,
      Severity.WARNING,
      "The juice groove doesn't fit.",
      "${formatter.format(groove.inset)} in and ${formatter.format(groove.width)} wide, it needs " +
        "a board wider than ${formatter.format(groove.minimumSide())}, and this one is " +
        "${formatter.format(side)}. The preview leaves it out; make it narrower or closer to " +
        "the edge.",
    ),
  )
}
