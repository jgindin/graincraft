package com.jaygindin.graincraft.core.guide

import com.jaygindin.graincraft.core.design.EdgeProfile
import com.jaygindin.graincraft.core.design.FlatteningMethod
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.fabrication.RowKind
import com.jaygindin.graincraft.core.units.Length

// The stages after the panels are glued and flattened: slicing an end-grain board, then trimming,
// shaping and finishing any board.

internal fun crosscut(g: GuideContext): Stage {
  val t = checkNotNull(g.fabrication.sliceThickness) { "end grain is sliced" }
  val cuts =
    g.fabrication.panels.mapNotNull { panel ->
      g.fabrication.slicesPerPanel[panel.name]?.let { n ->
        "Cut ${plural(n, "slice")} ${g.f(t)} thick from ${g.label(panel)}."
      }
    }
  val steps =
    listOf(
      "Square one end of each panel first, taking off as little as you can.",
      "Set a stop block ${g.f(t)} from the blade, so every slice is the same thickness.",
    ) + cuts + "Keep each panel's slices stacked in cutting order."
  val callouts =
    listOf(
      Callout(
        CalloutKind.WARNING,
        "Cut on a crosscut sled or miter saw with the stop block before the blade. Never use " +
          "the rip fence as a stop: a slice trapped between fence and blade kicks back.",
      ),
      Callout(
        CalloutKind.TIP,
        "Mark the top face of the panel before cutting, so no slice gets turned by mistake.",
      ),
    )
  return stage(StageKind.CROSSCUT, "Crosscut the slices", steps, callouts, crosscutDiagram(g))
}

internal fun arrangeAndGlue(g: GuideContext): Stage {
  val rows = g.fabrication.rows
  val slices = rows.filter { it.kind != RowKind.MORTAR }
  val steps = mutableListOf("Turn every slice a quarter turn so its end grain faces up.")
  if (g.panel1.size > 1) {
    steps +=
      "Make each row from one slice of each sub-panel, side by side in order: " +
        "${joinAnd(g.panel1.map { it.name })}."
  }
  val shift = slices.firstOrNull { it.shift > Length.ZERO }?.shift
  when {
    slices.any { it.rotated } -> {
      val reversed =
        if (g.panel1.size > 1) {
          ", taking its slices in reverse order (${joinAnd(g.panel1.reversed().map { it.name })})"
        } else ""
      steps +=
        "Rotate every other row 180° (rows 2, 4, 6, …): turn the whole row end for end$reversed. " +
          "This moves the colours over by one square."
    }
    shift != null -> {
      steps +=
        "Shift every other brick row (brick rows 2, 4, 6, …) ${g.f(shift)} across the width, " +
          "so the joints between bricks stagger."
      val mortar =
        if (g.mortar.size > 1) "one slice from each of ${joinAnd(g.mortar.map { it.name })}"
        else "one slice of the mortar panel"
      steps +=
        "Glue a mortar row between each pair of brick rows: $mortar, side by side. That makes " +
          "${plural(slices.size, "brick row")} and ${plural(slices.size - 1, "mortar row")}."
    }
    else -> steps += "Lay every row the same way round."
  }
  steps +=
    "Glue the rows into a block ${g.f(g.fabrication.glueUpLength)} long and " +
      "${g.f(g.fabrication.glueUpWidth)} wide; the sides are trimmed later."
  val callouts =
    listOf(
      Callout(
        CalloutKind.TIP,
        "Dry-fit and number the rows first. End grain soaks up glue, so spread it on both faces, " +
          "and glue a few rows at a time if your glue's open time is short.",
      ),
      Callout(
        CalloutKind.TIP,
        "Clamp along the rows and lightly across them, checking that the block stays flat.",
      ),
    )
  return stage(
    StageKind.ARRANGE_AND_GLUE,
    "Arrange and glue the slices",
    steps,
    callouts,
    arrangeDiagram(g),
  )
}

internal fun flattenBoard(g: GuideContext): Stage {
  val method = checkNotNull(g.fabrication.finalFlatten) { "end grain has a final flatten" }
  val how =
    when (method) {
      FlatteningMethod.DRUM_SANDER -> "drum sander, taking very light passes"
      FlatteningMethod.ROUTER_SLED -> "router sled, then sand out the router marks"
      FlatteningMethod.BELT_AND_ORBITAL ->
        "belt sander, checking often with a straightedge, then a random-orbit sander"
    }
  val steps =
    listOf(
      "Flatten both faces to ${g.f(g.design.finished.thickness)} with the $how. That removes " +
        "about ${g.f(g.fabrication.finalAllowance)} in all.",
    )
  val callouts =
    listOfNotNull(
      Callout(
        CalloutKind.WARNING,
        "Never put an end-grain board through a thickness planer, however wide: the knives " +
          "catch the end grain and can break the board apart and throw the pieces.",
      ),
      if (method == FlatteningMethod.ROUTER_SLED) {
        Callout(
          CalloutKind.TIP,
          "Clamp sacrificial strips along the edges, or chamfer them first, so the bit doesn't " +
            "chip out the end grain where it leaves the board.",
        )
      } else null,
    )
  return stage(StageKind.FLATTEN_BOARD, "Flatten the board", steps, callouts)
}

internal fun trim(g: GuideContext): Stage {
  val finished = g.design.finished
  val f = g.fabrication
  val side =
    "Trim both sides to make the board ${g.f(finished.width)} wide, taking " +
      "${g.f((f.glueUpWidth - finished.width) / 2)} from each side."
  val steps =
    if (f.mode == GrainMode.EDGE_GRAIN) {
      listOf(
        "Trim both ends to make the board ${g.f(finished.length)} long, taking " +
          "${g.f((f.glueUpLength - finished.length) / 2)} from each end.",
        side,
      )
    } else {
      listOf(
        side,
        "Leave the ends: they are the long-grain faces of the outer rows, already flat and square.",
      )
    }
  val tip =
    Callout(
      CalloutKind.TIP,
      "Back each cut with a scrap so the wood doesn't chip out where the blade leaves it.",
    )
  return stage(StageKind.TRIM, "Trim to size", steps, listOf(tip))
}

/**
 * The groove goes first, while the edges are square and give the router a flat fence. Feet go on
 * after the finish, so they are the finish stage's last step.
 */
internal fun edgeDetails(g: GuideContext): Stage? {
  val e = g.design.edges
  val steps =
    listOfNotNull(
      e.juiceGroove?.let {
        "Rout the juice groove ${g.f(it.width)} wide and ${g.f(it.depth)} deep, ${g.f(it.inset)} " +
          "in from the edge, against an edge guide or a template."
      },
      if (e.cornerRadius > Length.ZERO) {
        "Round the corners to a ${g.f(e.cornerRadius)} radius, routing or sanding to a template."
      } else null,
      when (e.edgeProfile) {
        EdgeProfile.NONE -> null
        EdgeProfile.CHAMFER ->
          "Rout a ${g.f(e.edgeProfileSize)} chamfer along the top and bottom edges."
        EdgeProfile.ROUNDOVER ->
          "Rout a ${g.f(e.edgeProfileSize)} roundover along the top and bottom edges."
      },
      if (e.fingerGrips) "Rout the finger grips into both ends." else null,
    )
  if (steps.isEmpty()) return null
  val callouts =
    listOf(
      Callout(
        CalloutKind.WARNING,
        "Clamp the board down or work on a router table with a fence, and keep your hands well " +
          "clear of the bit.",
      ),
      Callout(
        CalloutKind.TIP,
        "Take several shallow passes; deep cuts burn and tear out, worst on end grain.",
      ),
    )
  return stage(StageKind.EDGE_DETAILS, "Edge details", steps, callouts)
}

internal fun finish(g: GuideContext): Stage {
  val steps =
    listOfNotNull(
      "Sand every face and edge through the grits to 220, wipe with a damp cloth to raise the " +
        "grain, and sand lightly again once dry.",
      "Flood the board with mineral oil, let it soak in, and wipe off the excess. Repeat until it " +
        "stops soaking in.",
      "Finish with a mineral oil and beeswax board butter, buffed in.",
      if (g.design.edges.feet) "Attach four rubber feet to the bottom, near the corners." else null,
    )
  val tip =
    Callout(
      CalloutKind.TIP,
      "Re-oil whenever the board looks dry, and wash it by hand: never in the dishwasher.",
    )
  return stage(StageKind.FINISH, "Finish", steps, listOf(tip))
}
