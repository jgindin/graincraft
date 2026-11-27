package com.jaygindin.graincraft.app.ui.preview

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.jaygindin.graincraft.app.platform.SkiaArgbImageFactory
import com.jaygindin.graincraft.app.ui.theme.GrainCraftTheme
import com.jaygindin.graincraft.core.compile.CompileResult
import com.jaygindin.graincraft.core.compile.CompiledDesign
import com.jaygindin.graincraft.core.compile.DesignCompiler
import com.jaygindin.graincraft.core.design.BoardDesign
import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.design.EdgeTreatments
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.design.PatternSpec
import com.jaygindin.graincraft.core.design.Strip
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.FractionPrecision
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.LengthUnit
import com.jaygindin.graincraft.core.units.UnitFormatter
import com.jaygindin.graincraft.core.wood.Woods

/** The worked examples A, B and C, compiled with the default shop profile. */
object PreviewExamples {
  val woods = Woods(emptyList())
  private val fmt = UnitFormatter(LengthUnit.INCH, FractionPrecision.SIXTEENTH)

  fun inch(w: Int, n: Int = 0, d: Int = 1): Length = Length.inches(w, n, d)

  private fun design(mode: GrainMode, finished: Dimensions, pattern: PatternSpec) =
    BoardDesign(
      id = "example",
      name = "Example",
      revision = 1,
      updatedAtMillis = 0,
      grainMode = mode,
      finished = finished,
      pattern = pattern,
      edges = EdgeTreatments(),
    )

  /** End-grain checkerboard, walnut and maple, 18 × 12 × 1-1/2 with 1-1/2" cells. */
  val a =
    design(
      GrainMode.END_GRAIN,
      Dimensions(inch(18), inch(12), inch(1, 1, 2)),
      PatternSpec.Checkerboard("black-walnut", "hard-maple", inch(1, 1, 2)),
    )

  /** Edge-grain stripes, walnut, maple and cherry, 16 × 8-1/4 × 1-1/4. */
  val b =
    design(
      GrainMode.EDGE_GRAIN,
      Dimensions(inch(16), inch(8, 1, 4), inch(1, 1, 4)),
      PatternSpec.Stripes(
        listOf(
          Strip("black-walnut", inch(1, 1, 2)),
          Strip("hard-maple", inch(0, 3, 4)),
          Strip("american-cherry", inch(1, 1, 2)),
          Strip("hard-maple", inch(0, 3, 4)),
          Strip("american-cherry", inch(1, 1, 2)),
          Strip("hard-maple", inch(0, 3, 4)),
          Strip("black-walnut", inch(1, 1, 2)),
        ),
      ),
    )

  /** End-grain brick, cherry bricks and maple mortar, 17-1/4 × 12 × 1-1/2. */
  val c =
    design(
      GrainMode.END_GRAIN,
      Dimensions(inch(17, 1, 4), inch(12), inch(1, 1, 2)),
      PatternSpec.Brick("american-cherry", "hard-maple", inch(3), inch(1, 1, 2), inch(0, 1, 4)),
    )

  fun compile(design: BoardDesign): CompiledDesign {
    val result = DesignCompiler.compile(design, woods, ShopProfile(), fmt)
    return (result as? CompileResult.Ok)?.compiled ?: error("Example does not compile: $result")
  }
}

/** [content] in the light theme with the desktop bitmap factory, as the app provides them. */
@Composable
fun PreviewHost(content: @Composable () -> Unit) {
  GrainCraftTheme(dark = false) {
    CompositionLocalProvider(LocalArgbImageFactory provides SkiaArgbImageFactory) {
      Surface { content() }
    }
  }
}
