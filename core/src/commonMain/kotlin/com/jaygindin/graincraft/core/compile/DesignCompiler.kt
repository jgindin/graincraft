package com.jaygindin.graincraft.core.compile

import com.jaygindin.graincraft.core.audit.Issue
import com.jaygindin.graincraft.core.audit.audit
import com.jaygindin.graincraft.core.audit.auditEdges
import com.jaygindin.graincraft.core.design.BoardDesign
import com.jaygindin.graincraft.core.design.PatternSpec
import com.jaygindin.graincraft.core.fabrication.CompileError
import com.jaygindin.graincraft.core.fabrication.CutPart
import com.jaygindin.graincraft.core.fabrication.Fabrication
import com.jaygindin.graincraft.core.fabrication.FabricationResult
import com.jaygindin.graincraft.core.fabrication.Fit
import com.jaygindin.graincraft.core.fabrication.FitResult
import com.jaygindin.graincraft.core.fabrication.Lumber
import com.jaygindin.graincraft.core.fabrication.compileFabrication
import com.jaygindin.graincraft.core.fabrication.cutList
import com.jaygindin.graincraft.core.fabrication.fitPattern
import com.jaygindin.graincraft.core.fabrication.lumber
import com.jaygindin.graincraft.core.guide.Stage
import com.jaygindin.graincraft.core.guide.planStages
import com.jaygindin.graincraft.core.layout.BoardLayout
import com.jaygindin.graincraft.core.layout.buildLayout
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.UnitFormatter
import com.jaygindin.graincraft.core.wood.Woods

/** Everything the app shows for a design: the fitted size, the build, the guide and warnings. */
data class CompiledDesign(
  val design: BoardDesign,
  val fit: Fit,
  val fabrication: Fabrication,
  val layout: BoardLayout,
  val cutList: List<CutPart>,
  val lumber: Lumber,
  val stages: List<Stage>,
  val issues: List<Issue>,
)

sealed interface CompileResult {
  data class Ok(val compiled: CompiledDesign) : CompileResult

  data class Failed(val errors: List<CompileError>) : CompileResult
}

object DesignCompiler {
  /**
   * Compiles a design in one pure pass. Species ids are checked first, so every unknown one is
   * reported together; then the pattern is fitted and the build worked out, stopping at the first
   * error. The design's embedded custom woods count even when `woods` lacks them, so an imported
   * design compiles on any machine.
   */
  fun compile(
    design: BoardDesign,
    woods: Woods,
    profile: ShopProfile,
    formatter: UnitFormatter,
  ): CompileResult {
    val allWoods = woods.withExtra(design.embeddedCustomWoods)
    val unknown = speciesIds(design.pattern).filter { allWoods.byId(it) == null }
    if (unknown.isNotEmpty()) return CompileResult.Failed(unknown.map(CompileError::UnknownSpecies))
    return when (val r = fitPattern(design.grainMode, design.finished, design.pattern, formatter)) {
      is FitResult.Failed -> CompileResult.Failed(listOf(r.error))
      is FitResult.Ok -> build(design, r.fit, allWoods, profile, formatter)
    }
  }

  private fun build(
    design: BoardDesign,
    fit: Fit,
    woods: Woods,
    profile: ShopProfile,
    formatter: UnitFormatter,
  ): CompileResult {
    val fabrication =
      when (val r = compileFabrication(design.grainMode, design.finished, fit, profile)) {
        is FabricationResult.Failed -> return CompileResult.Failed(listOf(r.error))
        is FabricationResult.Ok -> r.fabrication
      }
    val parts = cutList(fabrication, profile)
    val lumber = lumber(parts, profile)
    // A checkerboard's length can snap away from the target, so the layout and the warnings use
    // the size actually reached.
    return CompileResult.Ok(
      CompiledDesign(
        design = design,
        fit = fit,
        fabrication = fabrication,
        layout = buildLayout(fabrication, fit.actual, profile),
        cutList = parts,
        lumber = lumber,
        stages = planStages(design, fabrication, parts, lumber, woods, profile, formatter),
        issues =
          (audit(design.grainMode, fit.actual, fit, woods, formatter) +
              auditEdges(design.edges, fit.actual, formatter))
            .sortedByDescending { it.severity },
      ),
    )
  }

  private fun speciesIds(pattern: PatternSpec): List<String> =
    when (pattern) {
      is PatternSpec.Stripes -> pattern.strips.map { it.speciesId }
      is PatternSpec.Checkerboard -> listOf(pattern.speciesA, pattern.speciesB)
      is PatternSpec.Brick -> listOf(pattern.brickSpecies, pattern.mortarSpecies)
    }.distinct()
}

/** One plain sentence for the user, with every measurement in their units. */
fun CompileError.message(formatter: UnitFormatter): String =
  when (this) {
    is CompileError.OutOfRange ->
      "The ${fieldName(field)} must be between ${formatter.format(min)} and ${formatter.format(max)}."
    CompileError.PatternNotAllowedInEdgeGrain ->
      "Edge-grain boards can only be striped; checkerboard and brick patterns need end grain."
    CompileError.NoStrips -> "Add at least one strip."
    is CompileError.NonPositiveWidth -> "Strip ${stripIndex + 1} needs a width greater than zero."
    is CompileError.StripOutOfRange ->
      "Strip ${stripIndex + 1} must be between ${formatter.format(min)} and " +
        "${formatter.format(max)} wide on the finished board."
    is CompileError.NonPositiveDimension -> "The ${fieldName(field)} must be greater than zero."
    is CompileError.UnknownSpecies -> "The wood \"$id\" is not in your wood list."
    is CompileError.NoValidSplit ->
      "Panel $panel can't be split into sub-panels ${formatter.format(limit)} wide or less, " +
        "because a single strip is wider than that; use narrower strips or a wider tool."
  }

private fun fieldName(field: String): String =
  when (field) {
    "targetCell" -> "cell size"
    "targetBrickWidth" -> "brick width"
    "targetBrickHeight" -> "brick height"
    "mortarWidth" -> "mortar width"
    "rowHeight" -> "row height"
    else -> field
  }
