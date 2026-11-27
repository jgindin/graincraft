package com.jaygindin.graincraft.core.audit

import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.fabrication.Fit
import com.jaygindin.graincraft.core.fabrication.RowKind
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.UnitFormatter
import com.jaygindin.graincraft.core.wood.Rating
import com.jaygindin.graincraft.core.wood.WoodSpecies
import com.jaygindin.graincraft.core.wood.Woods

/** How loudly an issue is shown; none of them blocks anything. */
enum class Severity {
  INFO,
  WARNING,
  AVOID,
}

enum class IssueCode {
  SPECIES_CAUTION,
  SPECIES_NOT_SUITABLE,
  JANKA_SPREAD,
  OILY_SPECIES,
  COLOR_BLEED,
  STRIP_TOO_THIN,
  END_GRAIN_TOO_THIN,
  JUICE_GROOVE_DOES_NOT_FIT,
}

data class Issue(
  val code: IssueCode,
  val severity: Severity,
  val title: String,
  val detail: String,
  val speciesIds: List<String> = emptyList(),
  val stripIndices: List<Int> = emptyList(),
)

private const val MAX_JANKA_SPREAD_LBF = 1100
private val MIN_STRIP = Length.inches(0, 1, 8)
private val MIN_END_GRAIN_THICKNESS = Length.inches(1, 1, 4)

/**
 * Woodworking warnings for a fitted board, worst first and otherwise in a stable order. Species ids
 * the catalog does not know are skipped; the compiler reports those.
 */
fun audit(
  mode: GrainMode,
  finished: Dimensions,
  fit: Fit,
  woods: Woods,
  formatter: UnitFormatter,
): List<Issue> {
  val species = fit.columns.map { it.speciesId }.distinct().mapNotNull { woods.byId(it) }
  val issues =
    speciesIssues(mode, species) +
      jankaSpread(species) +
      colorBleed(species) +
      thinStrips(fit, formatter) +
      endGrainThickness(mode, finished, formatter) +
      oily(species)
  return issues.sortedByDescending { it.severity }
}

private fun speciesIssues(mode: GrainMode, species: List<WoodSpecies>): List<Issue> =
  species.mapNotNull { wood ->
    val rating =
      if (mode == GrainMode.END_GRAIN) wood.suitability.endGrain else wood.suitability.edgeGrain
    val grain = if (mode == GrainMode.END_GRAIN) "end grain" else "edge grain"
    when (rating.rating) {
      Rating.NOT_SUITABLE ->
        Issue(
          IssueCode.SPECIES_NOT_SUITABLE,
          Severity.AVOID,
          "${wood.name} is not suited to $grain.",
          rating.reasons.joinToString(" "),
          listOf(wood.id),
        )
      Rating.CAUTION ->
        Issue(
          IssueCode.SPECIES_CAUTION,
          Severity.WARNING,
          "${wood.name} needs care in $grain.",
          rating.reasons.joinToString(" "),
          listOf(wood.id),
        )
      Rating.RECOMMENDED -> null
    }
  }

private fun jankaSpread(species: List<WoodSpecies>): List<Issue> {
  val softest = species.minByOrNull { it.jankaLbf }
  val hardest = species.maxByOrNull { it.jankaLbf }
  val wide =
    softest != null && hardest != null && hardest.jankaLbf - softest.jankaLbf > MAX_JANKA_SPREAD_LBF
  if (!wide || softest == null || hardest == null) return emptyList()
  return listOf(
    Issue(
      IssueCode.JANKA_SPREAD,
      Severity.WARNING,
      "These woods differ a lot in hardness.",
      "${hardest.name} (${hardest.jankaLbf} lbf) is much harder than ${softest.name} " +
        "(${softest.jankaLbf} lbf). The softer wood sands away faster and can leave dips.",
      listOf(softest.id, hardest.id),
    ),
  )
}

private fun colorBleed(species: List<WoodSpecies>): List<Issue> {
  val bleeders = species.filter { bleeder ->
    bleeder.bleedsColor && species.any { it.raw.base.luminance > bleeder.raw.base.luminance }
  }
  if (bleeders.isEmpty()) return emptyList()
  val lighter = species.filter { other ->
    bleeders.any { other.raw.base.luminance > it.raw.base.luminance }
  }
  return listOf(
    Issue(
      IssueCode.COLOR_BLEED,
      Severity.INFO,
      "A colour-bleeding wood sits next to a lighter one.",
      "${bleeders.joinToString { it.name }} can stain " +
        "${lighter.joinToString { it.name }} when wet. Wipe glue squeeze-out right away and " +
        "avoid water-based finishes at first.",
      (bleeders + lighter).map { it.id }.distinct(),
    ),
  )
}

private fun thinStrips(fit: Fit, formatter: UnitFormatter): List<Issue> {
  val thinColumns = fit.columns.withIndex().filter { it.value.finishedWidth < MIN_STRIP }
  val thinMortar = fit.rows.filter { it.kind == RowKind.MORTAR && it.height < MIN_STRIP }
  val issues = mutableListOf<Issue>()
  if (thinColumns.isNotEmpty()) {
    val narrowest = thinColumns.minOf { it.value.finishedWidth }
    issues +=
      Issue(
        IssueCode.STRIP_TOO_THIN,
        Severity.WARNING,
        "Some strips are very thin.",
        "The narrowest strip is ${formatter.format(narrowest)}, under " +
          "${formatter.format(MIN_STRIP)}. Thin strips are fragile and hard to glue.",
        thinColumns.map { it.value.speciesId }.distinct(),
        thinColumns.map { it.index },
      )
  }
  if (thinMortar.isNotEmpty()) {
    issues +=
      Issue(
        IssueCode.STRIP_TOO_THIN,
        Severity.WARNING,
        "The mortar lines are very thin.",
        "A mortar line is ${formatter.format(thinMortar.minOf { it.height })}, under " +
          "${formatter.format(MIN_STRIP)}. Thin lines are fragile and hard to glue.",
      )
  }
  return issues
}

private fun endGrainThickness(
  mode: GrainMode,
  finished: Dimensions,
  formatter: UnitFormatter,
): List<Issue> =
  if (mode == GrainMode.END_GRAIN && finished.thickness < MIN_END_GRAIN_THICKNESS) {
    listOf(
      Issue(
        IssueCode.END_GRAIN_TOO_THIN,
        Severity.WARNING,
        "This end-grain board is thin.",
        "At ${formatter.format(finished.thickness)} it is under " +
          "${formatter.format(MIN_END_GRAIN_THICKNESS)} and prone to cracking or warping.",
      ),
    )
  } else {
    emptyList()
  }

private fun oily(species: List<WoodSpecies>): List<Issue> {
  val oily = species.filter { it.oily }
  if (oily.isEmpty()) return emptyList()
  return listOf(
    Issue(
      IssueCode.OILY_SPECIES,
      Severity.INFO,
      "An oily wood is in this board.",
      "${oily.joinToString { it.name }} can resist glue. The plan wipes the glue faces with " +
        "acetone just before gluing.",
      oily.map { it.id },
    ),
  )
}
