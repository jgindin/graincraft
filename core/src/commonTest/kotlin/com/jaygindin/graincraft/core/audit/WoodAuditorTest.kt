package com.jaygindin.graincraft.core.audit

import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.fabrication.Fit
import com.jaygindin.graincraft.core.fabrication.FitRow
import com.jaygindin.graincraft.core.fabrication.FitStrip
import com.jaygindin.graincraft.core.fabrication.RowKind
import com.jaygindin.graincraft.core.fabrication.StripRole
import com.jaygindin.graincraft.core.units.FractionPrecision
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.LengthUnit
import com.jaygindin.graincraft.core.units.UnitFormatter
import com.jaygindin.graincraft.core.wood.Woods
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WoodAuditorTest {
  private val woods = Woods(emptyList())
  private val formatter = UnitFormatter(LengthUnit.INCH, FractionPrecision.SIXTEENTH)

  private fun inch(w: Int, n: Int = 0, d: Int = 1) = Length.inches(w, n, d)

  private fun fit(
    vararg strips: Pair<String, Length>,
    mortarHeights: List<Length> = emptyList(),
  ): Fit =
    Fit(
      columns = strips.map { FitStrip(it.first, it.second, StripRole.PLAIN) },
      rows =
        listOf(FitRow(RowKind.PLAIN, inch(1))) + mortarHeights.map { FitRow(RowKind.MORTAR, it) },
      actual = Dimensions(inch(12), inch(8), inch(1, 1, 2)),
      notes = emptyList(),
    )

  private fun audit(
    mode: GrainMode,
    fit: Fit,
    thickness: Length = inch(1, 1, 2),
  ): List<Issue> = audit(mode, Dimensions(inch(12), inch(8), thickness), fit, woods, formatter)

  private fun List<Issue>.codes() = map { it.code }

  @Test
  fun redOakEndGrainIsAvoid() {
    val issues = audit(GrainMode.END_GRAIN, fit("red-oak" to inch(2)))
    val issue = issues.single { it.code == IssueCode.SPECIES_NOT_SUITABLE }
    assertEquals(Severity.AVOID, issue.severity)
    assertEquals(listOf("red-oak"), issue.speciesIds)
    val reasons = woods.byId("red-oak")!!.suitability.endGrain.reasons
    assertEquals(reasons.joinToString(" "), issue.detail)
  }

  @Test
  fun redOakEdgeGrainIsWarning() {
    val issues = audit(GrainMode.EDGE_GRAIN, fit("red-oak" to inch(2)))
    val issue = issues.single { it.code == IssueCode.SPECIES_CAUTION }
    assertEquals(Severity.WARNING, issue.severity)
  }

  @Test
  fun oneSpeciesIssuePerSpecies() {
    val issues =
      audit(
        GrainMode.END_GRAIN,
        fit("red-oak" to inch(2), "hard-maple" to inch(2), "red-oak" to inch(2)),
      )
    assertEquals(1, issues.count { it.code == IssueCode.SPECIES_NOT_SUITABLE })
  }

  @Test
  fun mapleAndWalnutAreClean() {
    val issues = audit(GrainMode.END_GRAIN, fit("hard-maple" to inch(2), "black-walnut" to inch(2)))
    assertEquals(emptyList(), issues)
  }

  @Test
  fun bloodwoodAndCherryHaveJankaSpread() {
    val issue =
      audit(GrainMode.EDGE_GRAIN, fit("bloodwood" to inch(2), "american-cherry" to inch(2)))
        .single { it.code == IssueCode.JANKA_SPREAD }
    assertEquals(Severity.WARNING, issue.severity)
    assertEquals(setOf("bloodwood", "american-cherry"), issue.speciesIds.toSet())
  }

  @Test
  fun oilySpeciesIsInfo() {
    val issue =
      audit(GrainMode.EDGE_GRAIN, fit("teak" to inch(2), "hard-maple" to inch(2))).single {
        it.code == IssueCode.OILY_SPECIES
      }
    assertEquals(Severity.INFO, issue.severity)
    assertEquals(listOf("teak"), issue.speciesIds)
  }

  @Test
  fun colourBleedNeedsLighterNeighbourInBoard() {
    val bleed =
      audit(GrainMode.EDGE_GRAIN, fit("african-padauk" to inch(2), "hard-maple" to inch(2)))
        .single { it.code == IssueCode.COLOR_BLEED }
    assertEquals(Severity.INFO, bleed.severity)
    assertTrue("african-padauk" in bleed.speciesIds && "hard-maple" in bleed.speciesIds)
    val none = audit(GrainMode.EDGE_GRAIN, fit("african-padauk" to inch(2), "wenge" to inch(2)))
    assertTrue(IssueCode.COLOR_BLEED !in none.codes())
  }

  @Test
  fun thinStripNamesItsIndex() {
    val issue =
      audit(
          GrainMode.EDGE_GRAIN,
          fit("hard-maple" to inch(2), "black-walnut" to inch(0, 1, 16), "hard-maple" to inch(2)),
        )
        .single { it.code == IssueCode.STRIP_TOO_THIN }
    assertEquals(Severity.WARNING, issue.severity)
    assertEquals(listOf(1), issue.stripIndices)
  }

  @Test
  fun thinMortarRowIsReportedWithoutIndices() {
    val issue =
      audit(
          GrainMode.END_GRAIN,
          fit("hard-maple" to inch(2), mortarHeights = listOf(inch(0, 1, 16))),
        )
        .single { it.code == IssueCode.STRIP_TOO_THIN }
    assertEquals(emptyList(), issue.stripIndices)
  }

  @Test
  fun eighthInchStripIsFine() {
    val issues = audit(GrainMode.EDGE_GRAIN, fit("hard-maple" to inch(0, 1, 8)))
    assertTrue(IssueCode.STRIP_TOO_THIN !in issues.codes())
  }

  @Test
  fun endGrainThickness() {
    val f = fit("hard-maple" to inch(2))
    assertEquals(
      listOf(IssueCode.END_GRAIN_TOO_THIN),
      audit(GrainMode.END_GRAIN, f, inch(1)).codes(),
    )
    assertEquals(emptyList(), audit(GrainMode.END_GRAIN, f, inch(1, 1, 4)).codes())
    assertEquals(emptyList(), audit(GrainMode.EDGE_GRAIN, f, inch(1)).codes())
  }

  @Test
  fun avoidComesFirst() {
    val issues = audit(GrainMode.END_GRAIN, fit("teak" to inch(2), "red-oak" to inch(2)), inch(1))
    assertEquals(Severity.AVOID, issues.first().severity)
    assertEquals(issues.map { it.severity }, issues.map { it.severity }.sortedDescending())
  }

  @Test
  fun unknownSpeciesIsIgnored() {
    assertEquals(emptyList(), audit(GrainMode.EDGE_GRAIN, fit("nonesuch" to inch(2))))
  }
}
