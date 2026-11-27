package com.jaygindin.graincraft.core.packet

import com.jaygindin.graincraft.core.audit.Issue
import com.jaygindin.graincraft.core.audit.Severity
import com.jaygindin.graincraft.core.compile.CompiledDesign
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.fabrication.CutPart
import com.jaygindin.graincraft.core.fabrication.formatBoardFeet
import com.jaygindin.graincraft.core.guide.CalloutKind
import com.jaygindin.graincraft.core.guide.Stage
import com.jaygindin.graincraft.core.guide.StageDiagramSvgBuilder
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.UnitFormatter
import com.jaygindin.graincraft.core.wood.Woods
import kotlin.math.roundToInt

private const val STAGE_PIXELS_PER_INCH = 24.0
private const val PERCENT = 100
private const val UNKNOWN_SPECIES_FILL = "#BBBBBB"

private val STYLE =
  """
  body { font-family: -apple-system, Segoe UI, Helvetica, Arial, sans-serif; color: #000;
    background: #fff; margin: 0 auto; max-width: 8in; padding: 0.5in; line-height: 1.4; }
  h1 { font-size: 1.8em; margin: 0 0 0.3em; }
  h2 { font-size: 1.3em; margin: 1.6em 0 0.4em; border-bottom: 1px solid #000; }
  h3 { font-size: 1.05em; margin: 0.8em 0 0.2em; }
  table { border-collapse: collapse; width: 100%; margin: 0.4em 0; }
  th, td { text-align: left; padding: 0.2em 0.6em 0.2em 0; border-bottom: 1px solid #999; }
  svg { max-width: 100%; height: auto; }
  .swatch { display: inline-block; width: 0.9em; height: 0.9em; border: 1px solid #333;
    vertical-align: -0.1em; margin-right: 0.3em; }
  .species { margin-right: 1em; white-space: nowrap; }
  .callout { border: 1px solid #000; padding: 0.3em 0.6em; margin: 0.4em 0; }
  .warning { border-width: 2px; }
  .stage { margin-bottom: 1.2em; }
  .severity { font-weight: bold; }
  @media print {
    body { padding: 0; max-width: none; }
    .stage { break-inside: avoid; page-break-inside: avoid; }
    h2, h3 { break-after: avoid; page-break-after: avoid; }
    tr { break-inside: avoid; page-break-inside: avoid; }
  }
  """
    .trimIndent()

/** Escapes text for HTML content and attribute values. */
internal fun escapeHtml(s: String): String =
  s.replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#39;")

/**
 * Builds the shop packet: one self-contained HTML page with inline CSS and SVG, which the user
 * opens in a browser to print or save as PDF. Every measurement goes through the formatter, so the
 * inch mark appears only where the user's units call for it.
 */
object ShopPacketExporter {
  fun html(
    compiled: CompiledDesign,
    woods: Woods,
    profile: ShopProfile,
    formatter: UnitFormatter,
  ): String {
    val ctx = PacketContext(compiled, woods, profile, formatter)
    val name = escapeHtml(compiled.design.name)
    return buildString {
      append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n")
      append("<title>$name - shop packet</title>\n<style>\n$STYLE\n</style>\n</head>\n<body>\n")
      append(ctx.cover())
      append(ctx.issues())
      append(ctx.shoppingList())
      append(ctx.cutList())
      for (stage in compiled.stages) append(stageHtml(stage))
      append(ctx.settings())
      append("</body>\n</html>\n")
    }
  }

  /** `"<name> - shop packet.html"`, with characters that are unsafe in file names replaced. */
  fun defaultFileName(designName: String): String {
    val safe = designName.replace(Regex("[/\\\\:*?\"<>|]"), "-")
    return "$safe - shop packet.html"
  }
}

private class PacketContext(
  val compiled: CompiledDesign,
  val woods: Woods,
  val profile: ShopProfile,
  val formatter: UnitFormatter,
) {
  private fun f(length: Length): String = escapeHtml(formatter.format(length))

  private fun speciesName(id: String): String = escapeHtml(woods.byId(id)?.name ?: id)

  private fun fill(id: String): String =
    woods.byId(id)?.let { hex(it.raw.base) } ?: UNKNOWN_SPECIES_FILL

  private fun swatch(id: String) = "<span class=\"swatch\" style=\"background:${fill(id)}\"></span>"

  fun cover(): String {
    val d = compiled.design
    val size = compiled.fit.actual
    val mode = if (d.grainMode == GrainMode.END_GRAIN) "End grain" else "Edge grain"
    val species = compiled.layout.pieces.map { it.speciesId }.distinct()
    return buildString {
      append("<header>\n<h1>${escapeHtml(d.name)}</h1>\n")
      append(boardDrawingSvg(compiled.layout, d.edges.cornerRadius, formatter, ::fill))
      append(
        "\n<p>Finished size: ${f(size.length)} × ${f(size.width)} × ${f(size.thickness)}</p>\n",
      )
      append("<p>Grain: $mode</p>\n<p>Species: ")
      append(
        species.joinToString("") {
          "<span class=\"species\">${swatch(it)}${speciesName(it)}</span>"
        },
      )
      append("</p>\n")
      for (note in compiled.fit.notes) append("<p>${escapeHtml(note)}</p>\n")
      append("</header>\n")
    }
  }

  fun issues(): String =
    section("Issues") {
      if (compiled.issues.isEmpty()) append("<p>No issues found.</p>\n")
      for (issue in compiled.issues) append(issueHtml(issue))
    }

  fun shoppingList(): String =
    section("Shopping list") {
      append("<table>\n<tr><th>Species</th><th>Stock</th><th>Board feet</th></tr>\n")
      for (s in compiled.lumber.bySpecies) {
        val stock =
          compiled.cutList
            .filter { it.speciesId == s.speciesId }
            .mapNotNull { it.stock?.label }
            .distinct()
            .joinToString(", ")
        append("<tr><td>${swatch(s.speciesId)}${speciesName(s.speciesId)}</td>")
        append("<td>$stock</td><td>${formatBoardFeet(s.boardFeet)}</td></tr>\n")
      }
      append("<tr><th>Total</th><th></th><th>${formatBoardFeet(compiled.lumber.total)}</th></tr>\n")
      append("</table>\n")
      append("<p>Includes ${percent(profile.millingWaste)} extra for milling waste.</p>\n")
    }

  fun cutList(): String =
    section("Cut list") {
      append("<table>\n<tr><th>Species</th><th>Qty</th><th>Size (thickness × width × length)</th>")
      append("<th>Mill from</th><th>Panels</th></tr>\n")
      for (part in compiled.cutList) append(partRow(part))
      append("</table>\n")
    }

  private fun partRow(p: CutPart): String {
    val from =
      p.stock?.let { "${it.label} stock, ${f(p.fromThickness)} thick, rip ${f(p.ripWidth)}" } ?: ""
    val lamination = if (p.laminated) " (glue two boards)" else ""
    return "<tr><td>${swatch(p.speciesId)}${speciesName(p.speciesId)}</td><td>${p.count}</td>" +
      "<td>${f(p.height)} × ${f(p.width)} × ${f(p.length)}$lamination</td>" +
      "<td>$from</td><td>${escapeHtml(p.panelNames.joinToString(", "))}</td></tr>\n"
  }

  fun settings(): String =
    section("Shop settings") {
      append("<table>\n")
      for ((k, v) in settingsRows(profile, formatter)) append("<tr><th>$k</th><td>$v</td></tr>\n")
      append("</table>\n")
    }

  private fun section(title: String, body: StringBuilder.() -> Unit): String = buildString {
    append("<section>\n<h2>$title</h2>\n")
    body()
    append("</section>\n")
  }
}

internal fun percent(fraction: Double): String = "${(fraction * PERCENT).roundToInt()}%"

private fun issueHtml(issue: Issue): String {
  val label =
    when (issue.severity) {
      Severity.INFO -> "Note"
      Severity.WARNING -> "Warning"
      Severity.AVOID -> "Avoid"
    }
  return "<div class=\"callout\"><span class=\"severity\">$label:</span> " +
    "<strong>${escapeHtml(issue.title)}</strong> ${escapeHtml(issue.detail)}</div>\n"
}

private fun settingsRows(
  profile: ShopProfile,
  formatter: UnitFormatter,
): List<Pair<String, String>> {
  fun f(length: Length) = escapeHtml(formatter.format(length))
  fun optional(length: Length?) = length?.let(::f) ?: "none"
  val flatten = profile.endGrainFlattening
  return listOf(
    "Saw kerf" to f(profile.kerf),
    "Planer maximum width" to optional(profile.planerMaxWidth),
    "Drum sander maximum width" to optional(profile.drumSanderMaxWidth),
    "Crosscut capacity" to f(profile.crosscutMaxWidth),
    "End-grain flattening" to
      "${flatten.name.lowercase().replace('_', ' ')}, " +
        "allowance ${f(profile.endGrainFlattenAllowance[flatten] ?: Length.ZERO)}",
    "Long-grain panel flatten allowance" to f(profile.panelFlattenAllowance),
    "Edge-grain flatten allowance" to f(profile.edgeGrainFlattenAllowance),
    "End trim (total)" to f(profile.endTrim),
    "Side trim (per side)" to f(profile.edgeTrimPerSide),
    "Milling waste" to percent(profile.millingWaste),
    "Glue" to escapeHtml(profile.glue),
  ) +
    profile.surfacedThickness.entries
      .sortedBy { it.key.quarters }
      .map { "Surfaced ${it.key.label} stock" to f(it.value) }
}

private fun stageHtml(stage: Stage): String = buildString {
  append(
    "<section class=\"stage\">\n<h2>Stage ${stage.number}. ${escapeHtml(stage.title)}</h2>\n",
  )
  stage.diagram?.let { append(StageDiagramSvgBuilder.svg(it, STAGE_PIXELS_PER_INCH) + "\n") }
  append("<ol>\n")
  for (step in stage.steps) append("<li>${escapeHtml(step)}</li>\n")
  append("</ol>\n")
  for (c in stage.callouts) {
    val kind = if (c.kind == CalloutKind.WARNING) "Warning" else "Tip"
    val css = if (c.kind == CalloutKind.WARNING) "callout warning" else "callout"
    append("<div class=\"$css\"><span class=\"severity\">$kind:</span> ")
    append("${escapeHtml(c.text)}</div>\n")
  }
  append("</section>\n")
}
