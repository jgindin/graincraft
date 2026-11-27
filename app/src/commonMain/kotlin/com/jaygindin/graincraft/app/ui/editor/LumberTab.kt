package com.jaygindin.graincraft.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaygindin.graincraft.app.ui.LocalUnitFormatter
import com.jaygindin.graincraft.app.ui.preview.ColorSwatch
import com.jaygindin.graincraft.app.ui.theme.Spacing
import com.jaygindin.graincraft.core.compile.CompiledDesign
import com.jaygindin.graincraft.core.fabrication.CutPart
import com.jaygindin.graincraft.core.fabrication.formatBoardFeet
import com.jaygindin.graincraft.core.wood.Woods

private val SWATCH = 16.dp
private val COUNT_COLUMN = 32.dp
private val STOCK_COLUMN = 48.dp
private const val WOOD_WEIGHT = 1f
private const val SIZE_WEIGHT = 1.6f
private const val RIP_WEIGHT = 0.8f

/** What to buy, per species with milling waste, and the cut list of every milled part. */
@Composable
internal fun LumberTab(compiled: CompiledDesign, woods: Woods, modifier: Modifier) {
  Column(
    modifier.verticalScroll(rememberScrollState()).padding(Spacing.lg),
    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
  ) {
    Text("Shopping list", style = MaterialTheme.typography.titleSmall)
    compiled.lumber.bySpecies.forEach { species ->
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier =
          Modifier.fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .testTag(
              "lumber-${species.speciesId}",
            ),
      ) {
        ColorSwatch(woods.byId(species.speciesId)?.raw?.base, size = SWATCH)
        Text(
          woods.nameOf(species.speciesId),
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.weight(1f),
        )
        Text(formatBoardFeet(species.boardFeet), style = MaterialTheme.typography.bodyMedium)
      }
    }
    HorizontalDivider()
    Row(
      Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.testTag("lumber-total"),
    ) {
      Text("Total", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
      Text(formatBoardFeet(compiled.lumber.total), style = MaterialTheme.typography.titleSmall)
    }
    Text(
      "Board feet include milling waste. Stock is the rough thickness to buy.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      "Cut list",
      style = MaterialTheme.typography.titleSmall,
      modifier = Modifier.padding(top = Spacing.md),
    )
    CutListTable(compiled.cutList, woods)
  }
}

@Composable
private fun CutListTable(parts: List<CutPart>, woods: Woods) {
  val formatter = LocalUnitFormatter.current
  val header = MaterialTheme.typography.labelMedium
  val body = MaterialTheme.typography.bodySmall
  Column {
    CutRow(header, "Wood", "Qty", "W × H × L", "Stock", "Rip")
    HorizontalDivider()
    parts.forEachIndexed { i, part ->
      CutRow(
        body,
        woods.nameOf(part.speciesId),
        part.count.toString(),
        "${formatter.format(part.width)} × ${formatter.format(part.height)} × " +
          formatter.format(part.length),
        part.stock?.label ?: "Laminate",
        formatter.format(part.ripWidth),
        Modifier.testTag("cut-$i"),
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
  }
}

@Composable
private fun CutRow(
  style: TextStyle,
  wood: String,
  count: String,
  size: String,
  stock: String,
  rip: String,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier.fillMaxWidth().padding(vertical = Spacing.xs),
    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
  ) {
    Cell(wood, style, Modifier.weight(WOOD_WEIGHT))
    Cell(count, style, Modifier.width(COUNT_COLUMN), TextAlign.End)
    Cell(size, style, Modifier.weight(SIZE_WEIGHT))
    Cell(stock, style, Modifier.width(STOCK_COLUMN))
    Cell(rip, style, Modifier.weight(RIP_WEIGHT))
  }
}

@Composable
private fun Cell(
  text: String,
  style: TextStyle,
  modifier: Modifier,
  align: TextAlign = TextAlign.Start,
) {
  Text(text, style = style, textAlign = align, modifier = modifier)
}

private fun Woods.nameOf(id: String): String = byId(id)?.name ?: id
