package com.jaygindin.graincraft.app.ui.shop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.jaygindin.graincraft.app.ui.theme.Spacing
import com.jaygindin.graincraft.core.design.FlatteningMethod
import com.jaygindin.graincraft.core.shop.ShopProfile
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.Nominal

/** The shop profile as a form of grouped cards; every change is saved at once. */
@Composable
fun ShopScreen(viewModel: ShopViewModel) {
  val profile by viewModel.profile.collectAsState()
  ShopContent(profile, viewModel::update, viewModel::resetToDefaults)
}

@OptIn(ExperimentalMaterial3Api::class) // TopAppBar is still experimental in Material 3
@Composable
internal fun ShopContent(
  profile: ShopProfile,
  onChange: (ShopProfile) -> Unit,
  onReset: () -> Unit,
) {
  var confirmingReset by remember { mutableStateOf(false) }
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Shop") },
        actions = {
          TextButton(
            onClick = { confirmingReset = true },
            modifier = Modifier.testTag("shop-reset"),
          ) {
            Text("Reset to defaults")
          }
          Spacer(Modifier.width(Spacing.lg))
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
          ),
      )
    },
    containerColor = MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxSize().testTag("screen-shop"),
  ) { padding ->
    Column(
      verticalArrangement = Arrangement.spacedBy(Spacing.lg),
      modifier =
        Modifier.padding(padding)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = Spacing.xl, vertical = Spacing.sm),
    ) {
      SawsCard(profile, onChange)
      MachinesCard(profile, onChange)
      FlatteningCard(profile, onChange)
      AllowancesCard(profile, onChange)
      LumberCard(profile, onChange)
    }
  }
  if (confirmingReset) {
    ResetDialog(
      onConfirm = {
        onReset()
        confirmingReset = false
      },
      onDismiss = { confirmingReset = false },
    )
  }
}

@Composable
private fun ResetDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Reset the shop profile?") },
    text = { Text("Every shop setting goes back to its default. This cannot be undone.") },
    confirmButton = {
      TextButton(
        onClick = onConfirm,
        modifier = Modifier.testTag("shop-reset-confirm"),
      ) {
        Text("Reset")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("shop-reset-cancel"),
      ) {
        Text("Cancel")
      }
    },
  )
}

@Composable
private fun SawsCard(p: ShopProfile, onChange: (ShopProfile) -> Unit) {
  ShopCard("Saws") {
    ShopLength(
      "Kerf",
      p.kerf,
      "shop-kerf",
      "Wood lost to each saw cut; strips are cut this much wider so they finish at size.",
      allowZero = false,
    ) {
      onChange(p.copy(kerf = it))
    }
    ShopLength(
      "Crosscut capacity",
      p.crosscutMaxWidth,
      "shop-crosscutMaxWidth",
      "Widest panel your crosscut sled or miter saw can cut; wider panels are split first.",
      allowZero = false,
    ) {
      onChange(p.copy(crosscutMaxWidth = it))
    }
  }
}

@Composable
private fun MachinesCard(p: ShopProfile, onChange: (ShopProfile) -> Unit) {
  ShopCard("Planer and sander") {
    OptionalLength(
      "Planer width",
      p.planerMaxWidth,
      "shop-planerMaxWidth",
      "Widest board your planer takes. Without one, the build assumes you flatten by other means.",
      Length.inches(PLANER_INCHES),
    ) {
      onChange(p.copy(planerMaxWidth = it))
    }
    OptionalLength(
      "Drum sander width",
      p.drumSanderMaxWidth,
      "shop-drumSanderMaxWidth",
      "Widest panel your drum sander takes; it can flatten end-grain panels.",
      Length.inches(SANDER_INCHES),
    ) {
      onChange(p.copy(drumSanderMaxWidth = it))
    }
  }
}

// What the width starts at when the switch is turned on.
private const val PLANER_INCHES = 13
private const val SANDER_INCHES = 12

private val FLATTENING_LABELS =
  mapOf(
    FlatteningMethod.DRUM_SANDER to "Drum sander",
    FlatteningMethod.ROUTER_SLED to "Router sled",
    FlatteningMethod.BELT_AND_ORBITAL to "Belt and orbital",
  )

@Composable
private fun FlatteningCard(p: ShopProfile, onChange: (ShopProfile) -> Unit) {
  val method = p.endGrainFlattening
  val methodAllowance = p.endGrainFlattenAllowance[method] ?: Length.ZERO
  ShopCard("Flattening") {
    Text("End-grain flattening method", style = MaterialTheme.typography.bodyLarge)
    SingleChoiceSegmentedButtonRow(
      modifier = Modifier.fillMaxWidth().testTag("shop-endGrainFlattening"),
    ) {
      FlatteningMethod.entries.forEachIndexed { index, option ->
        SegmentedButton(
          selected = option == method,
          onClick = { onChange(p.copy(endGrainFlattening = option)) },
          shape = SegmentedButtonDefaults.itemShape(index, FlatteningMethod.entries.size),
          label = { Text(FLATTENING_LABELS.getValue(option)) },
          modifier = Modifier.testTag("shop-endGrainFlattening-${option.name.lowercase()}"),
        )
      }
    }
    Help("How you level a glued end-grain panel; it decides the allowance below.")
    ShopLength(
      "End-grain allowance (${FLATTENING_LABELS.getValue(method)})",
      methodAllowance,
      "shop-endGrainFlattenAllowance",
      "Extra thickness milled onto end-grain strips, then flattened away. Kept per method.",
    ) {
      onChange(p.copy(endGrainFlattenAllowance = p.endGrainFlattenAllowance + (method to it)))
    }
    ShopLength(
      "Panel flattening allowance",
      p.panelFlattenAllowance,
      "shop-panelFlattenAllowance",
      "Removed from a long-grain panel's height when it is planed flat.",
    ) {
      onChange(p.copy(panelFlattenAllowance = it))
    }
    ShopLength(
      "Edge-grain flattening allowance",
      p.edgeGrainFlattenAllowance,
      "shop-edgeGrainFlattenAllowance",
      "Extra thickness on edge-grain strips so the glued board can be planed flat.",
    ) {
      onChange(p.copy(edgeGrainFlattenAllowance = it))
    }
  }
}

@Composable
private fun AllowancesCard(p: ShopProfile, onChange: (ShopProfile) -> Unit) {
  ShopCard("Allowances") {
    ShopLength(
      "End trim",
      p.endTrim,
      "shop-endTrim",
      "Total length added to every strip for squaring the ends and planer snipe.",
    ) {
      onChange(p.copy(endTrim = it))
    }
    ShopLength(
      "Side trim (each side)",
      p.edgeTrimPerSide,
      "shop-edgeTrimPerSide",
      "Removed from each side when squaring the glued board.",
    ) {
      onChange(p.copy(edgeTrimPerSide = it))
    }
    PercentField(
      "Milling waste (%)",
      p.millingWaste,
      "shop-millingWaste",
      "Extra rough lumber for defects and milling; raises the board feet you shop for.",
    ) {
      onChange(p.copy(millingWaste = it))
    }
  }
}

@Composable
private fun LumberCard(p: ShopProfile, onChange: (ShopProfile) -> Unit) {
  ShopCard("Lumber") {
    Help("Thickness each rough grade is surfaced to; strips are cut from the thinnest that fits.")
    Nominal.entries.forEach { grade ->
      ShopLength(
        "Surfaced ${grade.label} stock",
        p.surfacedThickness[grade] ?: grade.nominalThickness,
        "shop-surfacedThickness-${grade.label}",
        null,
        allowZero = false,
      ) {
        onChange(p.copy(surfacedThickness = p.surfacedThickness + (grade to it)))
      }
    }
    OutlinedTextField(
      value = p.glue,
      onValueChange = { onChange(p.copy(glue = it)) },
      label = { Text("Glue") },
      supportingText = { Text("Named in the build guide's glue-up stage.") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth().testTag("shop-glue"),
    )
  }
}
