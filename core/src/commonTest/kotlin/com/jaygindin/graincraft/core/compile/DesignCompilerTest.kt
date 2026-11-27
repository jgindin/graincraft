package com.jaygindin.graincraft.core.compile

import com.jaygindin.graincraft.core.Examples
import com.jaygindin.graincraft.core.Examples.inch
import com.jaygindin.graincraft.core.audit.IssueCode
import com.jaygindin.graincraft.core.audit.audit
import com.jaygindin.graincraft.core.design.BoardDesign
import com.jaygindin.graincraft.core.design.EdgeTreatments
import com.jaygindin.graincraft.core.design.JuiceGroove
import com.jaygindin.graincraft.core.design.PatternSpec
import com.jaygindin.graincraft.core.design.Strip
import com.jaygindin.graincraft.core.fabrication.CompileError
import com.jaygindin.graincraft.core.fabrication.FabricationResult
import com.jaygindin.graincraft.core.fabrication.FitResult
import com.jaygindin.graincraft.core.fabrication.compileFabrication
import com.jaygindin.graincraft.core.fabrication.cutList
import com.jaygindin.graincraft.core.fabrication.fitPattern
import com.jaygindin.graincraft.core.fabrication.formatBoardFeet
import com.jaygindin.graincraft.core.guide.StageKind
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.FractionPrecision
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.LengthUnit
import com.jaygindin.graincraft.core.units.UnitFormatter
import com.jaygindin.graincraft.core.wood.PoreType
import com.jaygindin.graincraft.core.wood.Rgb
import com.jaygindin.graincraft.core.wood.WoodSpecies
import com.jaygindin.graincraft.core.wood.Woods
import com.jaygindin.graincraft.core.wood.derivedTones
import com.jaygindin.graincraft.core.wood.rateCustomWood
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesignCompilerTest {
  private val profile = ShopProfile()
  private val woods = Woods(emptyList())

  private fun compile(design: BoardDesign, userWoods: Woods = woods): CompileResult =
    DesignCompiler.compile(design, userWoods, profile, Examples.fmt)

  private fun ok(design: BoardDesign, userWoods: Woods = woods): CompiledDesign {
    val result = compile(design, userWoods)
    assertTrue(result is CompileResult.Ok, "expected Ok but was $result")
    return result.compiled
  }

  /** The compiled pieces must be exactly what the individual build steps produce. */
  private fun assertMatchesBuildSteps(design: BoardDesign, compiled: CompiledDesign) {
    val fit = fitPattern(design.grainMode, design.finished, design.pattern, Examples.fmt)
    assertTrue(fit is FitResult.Ok)
    assertEquals(fit.fit, compiled.fit)
    val fabrication = compileFabrication(design.grainMode, design.finished, fit.fit, profile)
    assertTrue(fabrication is FabricationResult.Ok)
    assertEquals(fabrication.fabrication, compiled.fabrication)
    assertEquals(cutList(fabrication.fabrication, profile), compiled.cutList)
    assertEquals(fit.fit.actual, compiled.layout.size)
    assertEquals(
      audit(design.grainMode, fit.fit.actual, fit.fit, woods, Examples.fmt),
      compiled.issues,
    )
    assertEquals(design, compiled.design)
  }

  private fun CompiledDesign.boardFeet(): List<String> =
    lumber.bySpecies.map { formatBoardFeet(it.boardFeet) } + formatBoardFeet(lumber.total)

  @Test
  fun exampleA() {
    val compiled = ok(Examples.a)
    assertMatchesBuildSteps(Examples.a, compiled)
    assertEquals(listOf("1A", "1B"), compiled.fabrication.panels.map { it.name })
    assertEquals(listOf("2.7 BF", "2.7 BF", "5.5 BF"), compiled.boardFeet())
    assertEquals(StageKind.entries.filter { it != StageKind.EDGE_DETAILS }, compiled.kinds())
  }

  @Test
  fun exampleB() {
    val compiled = ok(Examples.b)
    assertMatchesBuildSteps(Examples.b, compiled)
    assertEquals(listOf("1"), compiled.fabrication.panels.map { it.name })
    assertEquals(listOf("0.9 BF", "0.7 BF", "0.9 BF", "2.6 BF"), compiled.boardFeet())
    assertEquals(6, compiled.stages.size)
  }

  @Test
  fun exampleC() {
    val compiled = ok(Examples.c)
    assertMatchesBuildSteps(Examples.c, compiled)
    assertEquals(listOf("1A", "1B", "MA", "MB"), compiled.fabrication.panels.map { it.name })
    assertEquals(listOf("5.5 BF", "3.1 BF", "8.6 BF"), compiled.boardFeet())
  }

  @Test
  fun checkerboardLengthThatSnapsIsLaidOutAtTheFittedSize() {
    val design = Examples.a.copy(finished = Examples.a.finished.copy(length = inch(17)))
    val compiled = ok(design)
    assertEquals(inch(16, 1, 2), compiled.layout.size.length)
    assertTrue(compiled.fit.notes.isNotEmpty())
  }

  @Test
  fun unknownSpeciesFails() {
    val design =
      Examples.b.copy(
        pattern =
          PatternSpec.Stripes(
            listOf(Strip("hard-maple", inch(4)), Strip("no-such-wood", inch(4, 1, 4))),
          ),
      )
    assertEquals(
      CompileResult.Failed(listOf(CompileError.UnknownSpecies("no-such-wood"))),
      compile(design),
    )
  }

  @Test
  fun everyUnknownSpeciesIsReportedOnce() {
    val design =
      Examples.c.copy(
        pattern = PatternSpec.Brick("nope-1", "nope-2", inch(3), inch(1, 1, 2), inch(0, 1, 4)),
      )
    assertEquals(
      CompileResult.Failed(
        listOf(CompileError.UnknownSpecies("nope-1"), CompileError.UnknownSpecies("nope-2")),
      ),
      compile(design),
    )
  }

  @Test
  fun outOfRangeFails() {
    val design = Examples.b.copy(finished = Examples.b.finished.copy(length = inch(40)))
    assertEquals(
      CompileResult.Failed(listOf(CompileError.OutOfRange("length", inch(6), inch(36)))),
      compile(design),
    )
  }

  @Test
  fun noValidSplitFails() {
    val narrow = ShopProfile(planerMaxWidth = inch(1))
    val result = DesignCompiler.compile(Examples.b, woods, narrow, Examples.fmt)
    assertEquals(CompileResult.Failed(listOf(CompileError.NoValidSplit("1", inch(1)))), result)
  }

  @Test
  fun embeddedCustomWoodIsUsed() {
    val olive =
      WoodSpecies(
        id = "custom-olive",
        name = "Olive",
        botanicalName = "Olea europaea",
        jankaLbf = 2700,
        raw = derivedTones(Rgb.hex("#C8B27A")),
        oiled = derivedTones(Rgb.hex("#A88F55")),
        poreType = PoreType.DIFFUSE,
        oily = false,
        bleedsColor = false,
        irritant = false,
        suitability = rateCustomWood(2700, PoreType.DIFFUSE, oily = false, irritant = false),
        notes = emptyList(),
        custom = true,
      )
    val design =
      Examples.b.copy(
        pattern =
          PatternSpec.Stripes(
            listOf(Strip("custom-olive", inch(4)), Strip("hard-maple", inch(4, 1, 4))),
          ),
        embeddedCustomWoods = listOf(olive),
      )
    val compiled = ok(design)
    val buy = compiled.stages.single { it.kind == StageKind.BUY_AND_MILL }
    assertTrue(buy.steps.any { "Olive" in it }, "steps were ${buy.steps}")
  }

  @Test
  fun isDeterministic() {
    for (design in listOf(Examples.a, Examples.b, Examples.c)) {
      assertEquals(compile(design), compile(design))
    }
  }

  @Test
  fun errorMessagesAreSentencesWithFormattedMeasurements() {
    val errors =
      listOf(
        CompileError.OutOfRange("length", inch(6), inch(36)),
        CompileError.PatternNotAllowedInEdgeGrain,
        CompileError.NoStrips,
        CompileError.NonPositiveWidth(2),
        CompileError.NonPositiveDimension("targetCell"),
        CompileError.UnknownSpecies("no-such-wood"),
        CompileError.NoValidSplit("1", inch(12)),
      )
    for (error in errors) {
      val message = error.message(Examples.fmt)
      assertTrue(message.first().isUpperCase() && message.endsWith("."), message)
    }
    assertTrue(
      "6\"" in errors[0].message(Examples.fmt) && "36\"" in errors[0].message(Examples.fmt),
    )
    assertTrue("Strip 3" in errors[3].message(Examples.fmt))
    assertTrue("cell size" in errors[4].message(Examples.fmt))
    assertTrue("no-such-wood" in errors[5].message(Examples.fmt))
    assertTrue("12\"" in errors[6].message(Examples.fmt))
  }

  @Test
  fun tinyAndHugeElementsFailThroughCompile() {
    val cell =
      Examples.a.copy(
        pattern = PatternSpec.Checkerboard("black-walnut", "hard-maple", Length.millimetres(1.0)),
      )
    val tinyStrip =
      Examples.b.copy(
        pattern = PatternSpec.Stripes(listOf(Strip("black-walnut", Length.inchesApprox(1e-6)))),
      )
    val hugeStrip =
      Examples.b.copy(
        finished = Examples.b.finished.copy(width = inch(24)),
        pattern = PatternSpec.Stripes(listOf(Strip("black-walnut", inch(300)))),
      )
    for (design in listOf(cell, tinyStrip, hugeStrip)) {
      assertTrue(compile(design) is CompileResult.Failed, "expected Failed for ${design.pattern}")
    }
  }

  @Test
  fun aGrooveTooWideForTheBoardIsAWarningNotAFailure() {
    val groove = JuiceGroove(width = inch(6), depth = inch(0, 1, 4), inset = inch(1))
    val design = Examples.b.copy(edges = EdgeTreatments(juiceGroove = groove))
    val compiled = ok(design)
    assertTrue(compiled.issues.any { it.code == IssueCode.JUICE_GROOVE_DOES_NOT_FIT })
  }

  @Test
  fun elementRangeMessagesNameTheField() {
    val cell = CompileError.OutOfRange("targetCell", inch(0, 1, 4), inch(36)).message(Examples.fmt)
    assertEquals("The cell size must be between 1/4\" and 36\".", cell)
    val fine = UnitFormatter(LengthUnit.INCH, FractionPrecision.THIRTY_SECOND)
    val strip = CompileError.StripOutOfRange(1, inch(0, 1, 32), inch(36)).message(fine)
    assertEquals("Strip 2 must be between 1/32\" and 36\" wide on the finished board.", strip)
  }

  private fun CompiledDesign.kinds(): List<StageKind> = stages.map { it.kind }
}
