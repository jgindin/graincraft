package com.jaygindin.graincraft.app.ui.woods

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.jaygindin.graincraft.app.ui.preview.ColorSwatch
import com.jaygindin.graincraft.app.ui.theme.Spacing
import com.jaygindin.graincraft.core.wood.GrainRating
import com.jaygindin.graincraft.core.wood.PoreType
import com.jaygindin.graincraft.core.wood.Rating
import com.jaygindin.graincraft.core.wood.Rgb
import com.jaygindin.graincraft.core.wood.Suitability
import com.jaygindin.graincraft.core.wood.WoodSpecies
import com.jaygindin.graincraft.core.wood.derivedTones
import com.jaygindin.graincraft.core.wood.rateCustomWood
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val MAX_JANKA = 5000
private const val DEFAULT_JANKA = "1000"
private const val DEFAULT_RAW = "#C8A878"
private const val DEFAULT_OILED = "#A98558"
private const val HEX_DIGITS = 6
private const val RGB_MASK = 0xFFFFFF
private const val HEX_RADIX = 16
private val HEX_COLOUR = Regex("""#?[0-9a-fA-F]{6}""")

private fun parseHex(text: String): Rgb? =
  if (HEX_COLOUR.matches(text.trim())) Rgb.hex(text.trim()) else null

private fun Rgb.toHexText(): String =
  "#" + (argb and RGB_MASK).toString(HEX_RADIX).padStart(HEX_DIGITS, '0').uppercase()

/**
 * Adds a custom wood, or edits [initial]. The rating is computed from the entries as they change;
 * the override switch replaces it with the user's own rating and reason.
 */
@OptIn(ExperimentalUuidApi::class)
@Composable
fun CustomWoodDialog(
  initial: WoodSpecies?,
  onSave: (WoodSpecies) -> Unit,
  onDismiss: () -> Unit,
) {
  val form = remember(initial) { CustomWoodForm(initial) }
  val janka = form.janka.toIntOrNull()?.takeIf { it in 1..MAX_JANKA }
  val raw = parseHex(form.rawHex)
  val oiled = parseHex(form.oiledHex)
  val computed = janka?.let { rateCustomWood(it, form.pore, form.oily, form.irritant) }
  val shown = if (form.overriding) form.overridden() else computed
  val valid =
    form.name.isNotBlank() &&
      janka != null &&
      raw != null &&
      oiled != null &&
      (!form.overriding || form.reason.isNotBlank())

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (initial == null) "Add custom wood" else "Edit ${initial.name}") },
    text = { CustomWoodFields(form, janka, raw, oiled, shown, computed) },
    confirmButton = {
      Button(
        enabled = valid,
        onClick = {
          val id = initial?.id ?: "custom-${Uuid.random()}"
          onSave(
            form.toWood(
              id,
              janka ?: return@Button,
              raw ?: return@Button,
              oiled ?: return@Button,
              shown ?: return@Button,
            ),
          )
        },
        modifier = Modifier.testTag("custom-save"),
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, modifier = Modifier.testTag("custom-cancel")) {
        Text("Cancel")
      }
    },
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth(),
  )
}

/** What the dialog's fields currently hold. */
private class CustomWoodForm(initial: WoodSpecies?) {
  var name by mutableStateOf(initial?.name ?: "")
  var janka by mutableStateOf(initial?.jankaLbf?.toString() ?: DEFAULT_JANKA)
  var pore by mutableStateOf(initial?.poreType ?: PoreType.DIFFUSE)
  var oily by mutableStateOf(initial?.oily ?: false)
  var bleeds by mutableStateOf(initial?.bleedsColor ?: false)
  var irritant by mutableStateOf(initial?.irritant ?: false)
  var rawHex by mutableStateOf(initial?.raw?.base?.toHexText() ?: DEFAULT_RAW)
  var oiledHex by mutableStateOf(initial?.oiled?.base?.toHexText() ?: DEFAULT_OILED)

  // A saved wood whose rating differs from the rules was overridden.
  private val savedOverride = initial?.takeIf {
    it.suitability != rateCustomWood(it.jankaLbf, it.poreType, it.oily, it.irritant)
  }
  var overriding by mutableStateOf(savedOverride != null)
  var overrideRating by
    mutableStateOf(savedOverride?.suitability?.edgeGrain?.rating ?: Rating.CAUTION)
  var reason by mutableStateOf(savedOverride?.suitability?.edgeGrain?.reasons?.firstOrNull() ?: "")

  fun overridden(): Suitability {
    val rating = GrainRating(overrideRating, listOf(reason.trim()))
    return Suitability(rating, rating)
  }

  fun toWood(id: String, janka: Int, raw: Rgb, oiled: Rgb, suitability: Suitability) =
    WoodSpecies(
      id = id,
      name = name.trim(),
      botanicalName = "Custom",
      jankaLbf = janka,
      raw = derivedTones(raw),
      oiled = derivedTones(oiled),
      poreType = pore,
      oily = oily,
      bleedsColor = bleeds,
      irritant = irritant,
      suitability = suitability,
      notes = emptyList(),
      custom = true,
    )
}

@Composable
private fun CustomWoodFields(
  form: CustomWoodForm,
  janka: Int?,
  raw: Rgb?,
  oiled: Rgb?,
  shown: Suitability?,
  computed: Suitability?,
) {
  Column(
    verticalArrangement = Arrangement.spacedBy(Spacing.md),
    modifier = Modifier.verticalScroll(rememberScrollState()),
  ) {
    OutlinedTextField(
      value = form.name,
      onValueChange = { form.name = it },
      label = { Text("Name") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("custom-name"),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
      OutlinedTextField(
        value = form.janka,
        onValueChange = { form.janka = it },
        label = { Text("Janka hardness (lbf)") },
        isError = janka == null,
        supportingText = { Text(if (janka == null) "Whole number, 1 to $MAX_JANKA" else "") },
        singleLine = true,
        modifier = Modifier.weight(1f).testTag("custom-janka"),
      )
      EnumDropdown(
        label = "Pores",
        options = PoreType.entries,
        selected = form.pore,
        text = PoreType::label,
        onSelect = { form.pore = it },
        tag = "custom-pore",
        modifier = Modifier.weight(1f),
      )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
      LabelledCheckbox("Oily", form.oily, "custom-oily") { form.oily = it }
      LabelledCheckbox("Bleeds colour", form.bleeds, "custom-bleeds") { form.bleeds = it }
      LabelledCheckbox("Irritant", form.irritant, "custom-irritant") { form.irritant = it }
    }
    HexField("Raw colour", form.rawHex, raw, "custom-raw-hex") { form.rawHex = it }
    HexField("Oiled colour", form.oiledHex, oiled, "custom-oiled-hex") { form.oiledHex = it }
    RatingSection(form, shown, computed)
  }
}

@Composable
private fun RatingSection(form: CustomWoodForm, shown: Suitability?, computed: Suitability?) {
  Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
    Text(
      if (form.overriding) "Your rating" else "Computed rating",
      style = MaterialTheme.typography.titleSmall,
    )
    if (shown == null) {
      Text("Enter a Janka hardness to rate this wood.")
    } else {
      Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        RatingChip("Edge grain", shown.edgeGrain.rating, Modifier.testTag("custom-rating-edge"))
        RatingChip("End grain", shown.endGrain.rating, Modifier.testTag("custom-rating-end"))
      }
      (shown.edgeGrain.reasons + shown.endGrain.reasons).distinct().forEach {
        Text("• $it", style = MaterialTheme.typography.bodySmall)
      }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      Switch(
        checked = form.overriding,
        onCheckedChange = {
          form.overriding = it
          if (it) form.overrideRating = computed?.edgeGrain?.rating ?: form.overrideRating
        },
        modifier = Modifier.testTag("custom-override"),
      )
      Text("Override the rating", modifier = Modifier.padding(start = Spacing.sm))
    }
    if (form.overriding) {
      EnumDropdown(
        label = "Rating for both grains",
        options = Rating.entries,
        selected = form.overrideRating,
        text = Rating::label,
        onSelect = { form.overrideRating = it },
        tag = "custom-override-rating",
        modifier = Modifier.fillMaxWidth(),
      )
      OutlinedTextField(
        value = form.reason,
        onValueChange = { form.reason = it },
        label = { Text("Reason (required)") },
        isError = form.reason.isBlank(),
        modifier = Modifier.fillMaxWidth().testTag("custom-override-reason"),
      )
    }
  }
}

@Composable
private fun LabelledCheckbox(
  label: String,
  checked: Boolean,
  tag: String,
  onChange: (Boolean) -> Unit,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Checkbox(checked = checked, onCheckedChange = onChange, modifier = Modifier.testTag(tag))
    Text(label)
  }
}

/** A hex colour entry with a live swatch; [colour] is null while the text is not a colour. */
@Composable
private fun HexField(
  label: String,
  text: String,
  colour: Rgb?,
  tag: String,
  onChange: (String) -> Unit,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
    verticalAlignment = Alignment.Top,
  ) {
    OutlinedTextField(
      value = text,
      onValueChange = onChange,
      label = { Text(label) },
      isError = colour == null,
      supportingText = { Text(if (colour == null) "Use six hex digits, like #C8A878" else "") },
      singleLine = true,
      modifier = Modifier.weight(1f).testTag(tag),
    )
    ColorSwatch(colour, Modifier.padding(top = Spacing.md).testTag("$tag-swatch"), size = 40.dp)
  }
}

/** A read-only text field that opens a menu of [options]; option tags are `<tag>-<NAME>`. */
@OptIn(ExperimentalMaterial3Api::class) // ExposedDropdownMenuBox is still experimental
@Composable
private fun <T : Enum<T>> EnumDropdown(
  label: String,
  options: List<T>,
  selected: T,
  text: (T) -> String,
  onSelect: (T) -> Unit,
  tag: String,
  modifier: Modifier = Modifier,
) {
  var open by remember { mutableStateOf(false) }
  ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }, modifier = modifier) {
    OutlinedTextField(
      value = text(selected),
      onValueChange = {},
      readOnly = true,
      label = { Text(label) },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
      modifier =
        Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
          .fillMaxWidth()
          .testTag(tag),
    )
    ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
      options.forEach { option ->
        DropdownMenuItem(
          text = { Text(text(option)) },
          onClick = {
            onSelect(option)
            open = false
          },
          modifier = Modifier.testTag("$tag-${option.name}"),
        )
      }
    }
  }
}
