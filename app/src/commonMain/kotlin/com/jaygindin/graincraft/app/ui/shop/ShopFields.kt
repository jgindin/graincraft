package com.jaygindin.graincraft.app.ui.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.jaygindin.graincraft.app.ui.components.LengthField
import com.jaygindin.graincraft.app.ui.theme.Spacing
import com.jaygindin.graincraft.core.units.Length
import kotlin.math.roundToInt

/** A titled card; shop cards share one readable width rather than stretching. */
@Composable
internal fun ShopCard(title: String, content: @Composable ColumnScope.() -> Unit) {
  Card(
    colors =
      CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
  ) {
    Column(
      verticalArrangement = Arrangement.spacedBy(Spacing.md),
      modifier = Modifier.padding(Spacing.lg),
    ) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      content()
    }
  }
}

@Composable
internal fun Help(text: String) {
  Text(
    text,
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

@Composable
internal fun ShopLength(
  label: String,
  value: Length,
  tag: String,
  help: String?,
  allowZero: Boolean = true,
  onChange: (Length) -> Unit,
) {
  LengthField(
    label = label,
    value = value,
    onValueChange = onChange,
    modifier = Modifier.fillMaxWidth(),
    testTag = tag,
    supportingText = help,
    allowZero = allowZero,
  )
}

/** A machine the user may not own: an "I have one" switch that reveals its width field. */
@Composable
internal fun OptionalLength(
  label: String,
  value: Length?,
  tag: String,
  help: String,
  defaultWidth: Length,
  onChange: (Length?) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Switch(
        checked = value != null,
        onCheckedChange = { onChange(if (it) defaultWidth else null) },
        modifier = Modifier.testTag("$tag-switch"),
      )
      Text("I have one: $label", modifier = Modifier.padding(start = Spacing.sm))
    }
    if (value == null) {
      Help(help)
    } else {
      ShopLength(label, value, tag, help, allowZero = false) { onChange(it) }
    }
  }
}

/** A 0 to 100 percentage entry for a fraction; text that is not in range is not saved. */
@Composable
internal fun PercentField(
  label: String,
  fraction: Double,
  tag: String,
  help: String,
  onChange: (Double) -> Unit,
) {
  // Non-null only while the field has been edited and not yet left.
  var typed by remember { mutableStateOf<String?>(null) }
  val percent = typed?.trim()?.toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..100.0 }
  OutlinedTextField(
    value = typed ?: percentText(fraction),
    onValueChange = {
      typed = it
      it
        .trim()
        .toDoubleOrNull()
        ?.takeIf { v -> v.isFinite() && v in 0.0..100.0 }
        ?.let { v -> onChange(v / 100.0) }
    },
    label = { Text(label) },
    isError = typed != null && percent == null,
    supportingText = { Text(if (typed != null && percent == null) "Enter 0 to 100" else help) },
    singleLine = true,
    modifier =
      Modifier.fillMaxWidth().testTag(tag).onFocusChanged { if (!it.isFocused) typed = null },
  )
}

private const val TENTHS = 10.0

private fun percentText(fraction: Double): String =
  ((fraction * 100 * TENTHS).roundToInt() / TENTHS).toString().removeSuffix(".0")
