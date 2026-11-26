package com.jaygindin.graincraft.core.guide

import com.jaygindin.graincraft.core.design.BoardDesign
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.fabrication.CutPart
import com.jaygindin.graincraft.core.fabrication.Fabrication
import com.jaygindin.graincraft.core.fabrication.FlatteningTool
import com.jaygindin.graincraft.core.fabrication.Lumber
import com.jaygindin.graincraft.core.fabrication.Panel
import com.jaygindin.graincraft.core.fabrication.PanelKind
import com.jaygindin.graincraft.core.fabrication.formatBoardFeet
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.UnitFormatter
import com.jaygindin.graincraft.core.wood.Woods
import kotlin.math.roundToInt

private const val PERCENT = 100

/**
 * The step-by-step build guide for a compiled design, in shop order. Only the stages the board
 * needs are present, numbered from 1: edge grain skips slicing and the final flatten, and edge
 * details appear only when the design has some. Every measurement goes through `formatter`.
 */
fun planStages(
  design: BoardDesign,
  fabrication: Fabrication,
  parts: List<CutPart>,
  lumber: Lumber,
  woods: Woods,
  profile: ShopProfile,
  formatter: UnitFormatter,
): List<Stage> {
  val g = GuideContext(design, fabrication, woods, profile, formatter)
  val endGrain = fabrication.mode == GrainMode.END_GRAIN
  val stages =
    listOfNotNull(
      buyAndMill(g, parts, lumber),
      prepare(g),
      gluePanels(g),
      flattenPanels(g),
      if (endGrain) crosscut(g) else null,
      if (endGrain) arrangeAndGlue(g) else null,
      if (endGrain) flattenBoard(g) else null,
      trim(g),
      edgeDetails(g),
      finish(g),
    )
  return stages.mapIndexed { i, s -> s.copy(number = i + 1) }
}

/** What every stage reads, with the wording helpers they share. */
internal class GuideContext(
  val design: BoardDesign,
  val fabrication: Fabrication,
  val woods: Woods,
  val profile: ShopProfile,
  private val formatter: UnitFormatter,
) {
  val panel1: List<Panel> = fabrication.panels.filter { it.kind != PanelKind.MORTAR }
  val mortar: List<Panel> = fabrication.panels.filter { it.kind == PanelKind.MORTAR }

  fun f(length: Length): String = formatter.format(length)

  fun name(speciesId: String): String = woods.byId(speciesId)?.name ?: speciesId

  /** `"Teak and Wenge"` for the distinct species, in order. */
  fun names(speciesIds: Iterable<String>): String = joinAnd(speciesIds.distinct().map(::name))

  /** How a panel is named in a sentence: `"the board"`, `"panel 1"`, `"sub-panel 1A"`. */
  fun label(panel: Panel): String =
    when {
      panel.kind == PanelKind.EDGE_GRAIN_BOARD && panel.name == "1" -> "the board"
      panel.kind == PanelKind.MORTAR && panel.name == "M" -> "the mortar panel"
      panel.name == "1" -> "panel 1"
      else -> "sub-panel ${panel.name}"
    }

  fun tool(tool: FlatteningTool): String =
    when (tool) {
      FlatteningTool.PLANER -> "thickness planer"
      FlatteningTool.DRUM_SANDER -> "drum sander"
      FlatteningTool.ROUTER_SLED -> "router sled"
      FlatteningTool.BELT_AND_ORBITAL -> "belt sander, then a random-orbit sander"
    }
}

internal fun joinAnd(items: List<String>): String =
  when (items.size) {
    0 -> ""
    1 -> items.single()
    else -> items.dropLast(1).joinToString(", ") + " and " + items.last()
  }

internal fun plural(count: Int, word: String): String =
  if (count == 1) "$count $word" else "$count ${word}s"

internal fun stage(
  kind: StageKind,
  title: String,
  steps: List<String>,
  callouts: List<Callout> = emptyList(),
  diagram: Diagram? = null,
): Stage = Stage(0, kind, title, steps, callouts, diagram)

private fun buyAndMill(g: GuideContext, parts: List<CutPart>, lumber: Lumber): Stage {
  val buy =
    lumber.bySpecies.map { s ->
      val stocks =
        parts
          .filter { it.speciesId == s.speciesId }
          .mapNotNull { it.stock }
          .distinct()
          .sortedBy { it.quarters }
          .map { it.label }
      val stock = if (stocks.isEmpty()) "" else " in ${joinAnd(stocks)} stock"
      "Buy ${formatBoardFeet(s.boardFeet)} of ${g.name(s.speciesId)}$stock."
    }
  val waste = (g.profile.millingWaste * PERCENT).roundToInt()
  val total =
    "That is ${formatBoardFeet(lumber.total)} in all, with $waste% extra for defects and milling."
  val mill = parts.map { millStep(g, it) }
  val irritants =
    g.fabrication.panels
      .flatMap { p -> p.strips.map { it.speciesId } }
      .filter { g.woods.byId(it)?.irritant == true }
  val callouts =
    listOfNotNull(
      irritants
        .takeIf { it.isNotEmpty() }
        ?.let {
          Callout(
            CalloutKind.WARNING,
            "${g.names(it)} dust is an irritant: wear a respirator, run dust collection and " +
              "cover your skin while milling and sanding.",
          )
        },
      Callout(
        CalloutKind.TIP,
        "Let the lumber settle in your shop for a few days before milling, and mill every part " +
          "of one size at one machine setting so they match.",
      ),
    )
  return stage(StageKind.BUY_AND_MILL, "Buy and mill stock", buy + total + mill, callouts)
}

private fun millStep(g: GuideContext, part: CutPart): String {
  val size = "${g.f(part.width)} × ${g.f(part.height)} × ${g.f(part.length)}"
  val panels = part.panelNames.joinToString(", ")
  val head =
    "Mill ${plural(part.count, "${g.name(part.speciesId)} part")} $size " +
      "(${if (part.panelNames.size == 1) "panel" else "panels"} $panels)"
  val stock = part.stock
  val how =
    if (stock == null) {
      ": no stock in your shop profile is that thick, so laminate it from thinner boards or " +
        "buy thicker stock."
    } else {
      ": plane ${stock.label} stock to ${g.f(part.fromThickness)}, then rip to " +
        "${g.f(part.ripWidth)}."
    }
  val laminated =
    if (part.laminated) " Face-glue them in pairs to make the strips too wide for one board."
    else ""
  return head + how + laminated
}

private fun prepare(g: GuideContext): Stage {
  val species = g.fabrication.panels.flatMap { p -> p.strips.map { it.speciesId } }.distinct()
  val oily = species.filter { g.woods.byId(it)?.oily == true }
  val bleeds = species.filter { g.woods.byId(it)?.bleedsColor == true }
  val steps =
    listOfNotNull(
      "Dry-fit every panel in order, and have clamps and cauls ready before you open the glue.",
      "Use ${g.profile.glue}.",
      oily
        .takeIf { it.isNotEmpty() }
        ?.let {
          "Right before glue-up, wipe the gluing faces of ${g.names(it)} with acetone and let " +
            "them flash off for a few minutes; the natural oils otherwise keep glue from bonding."
        },
      bleeds
        .takeIf { it.isNotEmpty() }
        ?.let {
          "${g.names(it)} bleeds colour into lighter woods: glue up, then wipe off the " +
            "squeeze-out promptly with a damp cloth rather than letting it dry."
        },
    )
  val tip =
    Callout(
      CalloutKind.TIP,
      "Draw a pencil triangle across each panel's strips, so their order and which way up they " +
        "go are obvious at a glance.",
    )
  return stage(StageKind.PREPARE, "Prepare for glue-up", steps, listOf(tip))
}

private fun gluePanels(g: GuideContext): Stage {
  val endGrain = g.fabrication.mode == GrainMode.END_GRAIN
  val title =
    when {
      !endGrain -> "Glue up the board"
      g.mortar.isEmpty() -> "Glue up panel 1"
      else -> "Glue up panel 1 and the mortar panel"
    }
  val steps = mutableListOf<String>()
  val first = g.panel1.first()
  if (g.panel1.size > 1) {
    val what = if (endGrain) "Panel 1" else "The board"
    val why =
      if (endGrain) "for the ${g.tool(first.flattening.tool)} and the crosscut"
      else "for the ${g.tool(first.flattening.tool)}; they are joined after flattening"
    steps +=
      "$what is made as ${g.panel1.size} sub-panels " +
        "(${joinAnd(g.panel1.map { it.name })}), each narrow enough $why."
  }
  var next = 1
  for (panel in g.panel1) {
    val range = next until next + panel.strips.size
    next = range.last + 1
    val strips = panel.strips.joinToString(", ") { "${g.name(it.speciesId)} ${g.f(it.millWidth)}" }
    val which = if (g.panel1.size > 1) " from strips ${range.first}–${range.last}" else ""
    steps +=
      "Glue up ${g.label(panel)}$which, left to right: $strips. It is ${g.f(panel.width)} wide, " +
        "with strips ${g.f(panel.millHeight)} tall and ${g.f(panel.length)} long."
  }
  if (g.mortar.isNotEmpty()) steps += mortarStep(g)
  steps +=
    "Clamp with even pressure until glue beads along every joint, and let it cure as the " +
      "glue's label says."
  val tip =
    Callout(
      CalloutKind.TIP,
      "Alternate clamps above and below the panel, and use cauls, so it stays flat.",
    )
  return stage(StageKind.GLUE_PANELS, title, steps, listOf(tip), glueDiagram(g))
}

private fun mortarStep(g: GuideContext): String {
  val m = g.mortar.first()
  val wood = g.name(m.strips.first().speciesId)
  val each = if (g.mortar.size > 1) "each " else ""
  val which =
    if (g.mortar.size > 1) "Mill ${joinAnd(g.mortar.map { it.name })} for the mortar rows"
    else "Mill the mortar panel"
  return "$which: ${each}one $wood board ${g.f(m.width)} wide, ${g.f(m.millHeight)} thick and " +
    "${g.f(m.length)} long. If you have no board that wide, glue ${each}one up from $wood " +
    "strips first."
}

private fun flattenPanels(g: GuideContext): Stage {
  val first = g.panel1.first()
  val tool = g.tool(first.flattening.tool)
  val t = g.design.finished.thickness
  val steps =
    if (g.fabrication.mode == GrainMode.EDGE_GRAIN) {
      if (g.panel1.size == 1) {
        listOf(
          "Flatten both faces of the board with the $tool, from ${g.f(first.millHeight)} to ${g.f(t)}.",
        )
      } else {
        listOf(
          "Flatten ${joinAnd(g.panel1.map { it.name })} with the $tool to ${g.f(first.flattenTo)}.",
          "Glue the sub-panels together edge to edge, in order.",
          "Level the joint to ${g.f(t)} with a hand plane or sander.",
        )
      }
    } else {
      listOfNotNull(
        "Flatten ${joinAnd(g.panel1.map { it.name })} with the $tool, from " +
          "${g.f(first.millHeight)} to ${g.f(first.flattenTo)}, the row height.",
        g.mortar.firstOrNull()?.let { m ->
          "Flatten ${joinAnd(g.mortar.map { it.name })} with the ${g.tool(m.flattening.tool)}, " +
            "from ${g.f(m.millHeight)} to ${g.f(m.flattenTo)}, the mortar width."
        },
      )
    }
  val title =
    if (g.fabrication.mode == GrainMode.EDGE_GRAIN) "Flatten the board" else "Flatten the panels"
  val tip =
    Callout(
      CalloutKind.TIP,
      "Scrape off dried glue first, take light passes and turn the panel over between passes, " +
        "so both faces end flat and parallel.",
    )
  return stage(StageKind.FLATTEN_PANELS, title, steps, listOf(tip))
}
