package com.jaygindin.graincraft.core.fabrication

import com.jaygindin.graincraft.core.design.Dimensions
import com.jaygindin.graincraft.core.design.GrainMode
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.sum

private const val PANEL_1 = "1"
private const val MORTAR_PANEL = "M"

/**
 * Split edge-grain boards are left this proud of `T`, so the joint between them can be levelled.
 */
private val SPLIT_BOARD_PROUD = Length.inches(0, 1, 32)

/** Turns a fitted pattern into panels, slices and rows, reading every allowance from `profile`. */
fun compileFabrication(
  mode: GrainMode,
  target: Dimensions,
  fit: Fit,
  profile: ShopProfile,
): FabricationResult =
  when (mode) {
    GrainMode.EDGE_GRAIN -> compileEdgeGrain(target, fit, profile)
    GrainMode.END_GRAIN -> compileEndGrain(target, fit, profile)
  }

private fun compileEdgeGrain(
  target: Dimensions,
  fit: Fit,
  profile: ShopProfile,
): FabricationResult {
  val strips = withSideTrim(fit.columns, profile.edgeTrimPerSide)
  val width = strips.map { it.millWidth }.sum()
  val flattening = longGrainFlattening(width, profile)
  val limit = splitLimit(flattening, endGrainPanel = false, profile)
  val groups = splitPanel(strips.map { it.millWidth }, limit) ?: return noValidSplit(PANEL_1, limit)
  val flattenTo = if (groups.size == 1) target.thickness else target.thickness + SPLIT_BOARD_PROUD
  val panels =
    subPanels(PANEL_1, strips, groups) { name, members ->
      Panel(
        name = name,
        kind = PanelKind.EDGE_GRAIN_BOARD,
        strips = members,
        width = members.map { it.millWidth }.sum(),
        millHeight = target.thickness + profile.edgeGrainFlattenAllowance,
        flattenTo = flattenTo,
        length = target.length + profile.endTrim,
        flattening = flattening,
      )
    }
  val rows =
    fit.rows.mapIndexed { i, r -> Row(i, r.kind, r.height, false, Length.ZERO, names(panels)) }
  return FabricationResult.Ok(
    Fabrication(
      mode = GrainMode.EDGE_GRAIN,
      panels = panels,
      sliceThickness = null,
      slicesPerPanel = emptyMap(),
      rows = rows,
      glueUpWidth = target.width + profile.edgeTrimPerSide * 2,
      glueUpLength = target.length + profile.endTrim,
      finalFlatten = null,
      finalAllowance = profile.edgeGrainFlattenAllowance,
    ),
  )
}

private fun compileEndGrain(target: Dimensions, fit: Fit, profile: ShopProfile): FabricationResult {
  val brick = fit.columns.any { it.role == StripRole.BRICK }
  val finalMethod = profile.endGrainFlattening
  val finalAllowance =
    profile.endGrainFlattenAllowance[finalMethod] ?: profile.panelFlattenAllowance
  val sliceThickness = target.thickness + finalAllowance
  val perSlice = sliceThickness + profile.kerf
  val slices = fit.rows.count { it.kind != RowKind.MORTAR }

  val strips =
    if (brick) brickSequence(fit.columns, target.width, profile.edgeTrimPerSide)
    else withSideTrim(fit.columns, profile.edgeTrimPerSide)
  val width = strips.map { it.millWidth }.sum()
  val flattening = longGrainFlattening(width, profile)
  val limit = splitLimit(flattening, endGrainPanel = true, profile)
  val groups = splitPanel(strips.map { it.millWidth }, limit) ?: return noValidSplit(PANEL_1, limit)
  // Rows differ by at most a nanometre, so the first (largest) one sets the strip height.
  val rowHeight = fit.rows.first().height
  val panel1 =
    subPanels(PANEL_1, strips, groups) { name, members ->
      Panel(
        name = name,
        kind = PanelKind.PANEL_1,
        strips = members,
        width = members.map { it.millWidth }.sum(),
        millHeight = rowHeight + flattening.allowance,
        flattenTo = rowHeight,
        length = perSlice * slices + profile.endTrim,
        flattening = flattening,
      )
    }
  // A single brick row has no mortar rows, so there is no mortar panel to cut.
  val mortar =
    if (brick && slices > 1) mortarPanels(fit.columns[1], width, slices - 1, perSlice, profile)
    else emptyList()

  val slicesPerPanel =
    panel1.associate { it.name to slices } + mortar.associate { it.name to slices - 1 }
  return FabricationResult.Ok(
    Fabrication(
      mode = GrainMode.END_GRAIN,
      panels = panel1 + mortar,
      sliceThickness = sliceThickness,
      slicesPerPanel = slicesPerPanel,
      rows = endGrainRows(fit, names(panel1), names(mortar), brick),
      glueUpWidth = width,
      // A checkerboard keeps its cells square, so its length can differ from the target.
      glueUpLength = fit.actual.length,
      finalFlatten = finalMethod,
      finalAllowance = finalAllowance,
    ),
  )
}

/** Every row uses a slice from each sub-panel; a row's rotation or shift applies to all of them. */
private fun endGrainRows(
  fit: Fit,
  panel1: List<String>,
  mortar: List<String>,
  brick: Boolean,
): List<Row> {
  val halfPeriod =
    if (brick) (fit.columns[0].finishedWidth + fit.columns[1].finishedWidth) / 2 else Length.ZERO
  var sliceRow = 0
  return fit.rows.mapIndexed { i, r ->
    if (r.kind == RowKind.MORTAR) {
      Row(i, r.kind, r.height, false, Length.ZERO, mortar)
    } else {
      val odd = sliceRow++ % 2 == 1
      val rotated = odd && fit.rotateAlternateRows
      val shift = if (odd && brick) halfPeriod else Length.ZERO
      Row(i, r.kind, r.height, rotated, shift, if (rotated) panel1.reversed() else panel1)
    }
  }
}

/** The two outer strips are milled `e` wider for trimming the sides; a lone strip gets both. */
private fun withSideTrim(columns: List<FitStrip>, e: Length): List<PanelStrip> =
  columns.mapIndexed { i, c ->
    val extra = (if (i == 0) e else Length.ZERO) + (if (i == columns.lastIndex) e else Length.ZERO)
    PanelStrip(c.speciesId, c.finishedWidth + extra, i, c.role)
  }

/**
 * `n` bricks and `n - 1` mortar strips, with `n` chosen so a row shifted by half a period still
 * covers the trimmed width. The excess is trimmed from both sides, so no strip gets extra width.
 */
private fun brickSequence(period: List<FitStrip>, width: Length, e: Length): List<PanelStrip> {
  val (b, m) = period
  val p = b.finishedWidth + m.finishedWidth
  val needed = width + e * 2 + p + m.finishedWidth
  val n = ((needed.nanometres + p.nanometres - 1) / p.nanometres).toInt()
  return List(2 * n - 1) { i ->
    val s = if (i % 2 == 0) b else m
    PanelStrip(s.speciesId, s.finishedWidth, i % 2, s.role)
  }
}

/** Mortar rows are sliced from boards of the mortar species, as wide as panel 1. */
private fun mortarPanels(
  mortar: FitStrip,
  width: Length,
  slices: Int,
  perSlice: Length,
  profile: ShopProfile,
): List<Panel> {
  val flattening = longGrainFlattening(width, profile)
  val widths = splitEvenly(width, splitLimit(flattening, endGrainPanel = true, profile))
  return subPanelNames(MORTAR_PANEL, widths.size).mapIndexed { i, name ->
    Panel(
      name = name,
      kind = PanelKind.MORTAR,
      strips = listOf(PanelStrip(mortar.speciesId, widths[i], -1 - i, StripRole.MORTAR)),
      width = widths[i],
      millHeight = mortar.finishedWidth + flattening.allowance,
      flattenTo = mortar.finishedWidth,
      length = perSlice * slices + profile.endTrim,
      flattening = flattening,
    )
  }
}

private fun subPanels(
  base: String,
  strips: List<PanelStrip>,
  groups: List<IntRange>,
  build: (String, List<PanelStrip>) -> Panel,
): List<Panel> =
  subPanelNames(base, groups.size).zip(groups).map { (name, range) ->
    build(name, strips.slice(range))
  }

private fun names(panels: List<Panel>): List<String> = panels.map { it.name }

/** `splitPanel` only fails under a limit, so `limit` is set here. */
private fun noValidSplit(panel: String, limit: Length?): FabricationResult =
  FabricationResult.Failed(CompileError.NoValidSplit(panel, checkNotNull(limit)))
