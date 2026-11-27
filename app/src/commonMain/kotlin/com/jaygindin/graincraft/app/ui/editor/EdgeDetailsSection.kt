package com.jaygindin.graincraft.app.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.platform.testTag
import com.jaygindin.graincraft.app.ui.components.LengthField
import com.jaygindin.graincraft.app.ui.theme.Spacing
import com.jaygindin.graincraft.core.design.EdgeProfile
import com.jaygindin.graincraft.core.design.EdgeTreatments
import com.jaygindin.graincraft.core.design.JuiceGroove
import com.jaygindin.graincraft.core.units.Length

/** A common router bit for a chamfer or roundover. */
private val DEFAULT_PROFILE_SIZE = Length.inches(0, 1, 8)
private val DEFAULT_GROOVE =
  JuiceGroove(
    width = Length.inches(0, 3, 8),
    depth = Length.inches(0, 1, 4),
    inset = Length.inches(0, 1, 2),
  )

/** Corner radius, edge profile, juice groove, finger grips and feet. */
@Composable
internal fun EdgeDetailsSection(
  edges: EdgeTreatments,
  update: PartUpdate<EdgeTreatments>,
) {
  Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
    LengthField(
      label = "Corner radius",
      value = edges.cornerRadius,
      onValueChange = { r -> update.update("edge-corner") { it.copy(cornerRadius = r) } },
      testTag = "edge-corner",
      supportingText = "0 for square corners",
      modifier = Modifier.fillMaxWidth(),
    )
    ProfileDropdown(edges.edgeProfile) { profile ->
      update {
        val size =
          if (profile != EdgeProfile.NONE && it.edgeProfileSize <= Length.ZERO) {
            DEFAULT_PROFILE_SIZE
          } else {
            it.edgeProfileSize
          }
        it.copy(edgeProfile = profile, edgeProfileSize = size)
      }
    }
    if (edges.edgeProfile != EdgeProfile.NONE) {
      LengthField(
        label = "Profile size",
        value = edges.edgeProfileSize,
        onValueChange = { s ->
          update.update("edge-profile-size") { it.copy(edgeProfileSize = s) }
        },
        testTag = "edge-profile-size",
        allowZero = false,
        modifier = Modifier.fillMaxWidth(),
      )
    }
    SwitchRow("Juice groove", "edge-groove", edges.juiceGroove != null) { on ->
      update { it.copy(juiceGroove = if (on) it.juiceGroove ?: DEFAULT_GROOVE else null) }
    }
    edges.juiceGroove?.let { groove -> GrooveFields(groove, update) }
    SwitchRow("Finger grips at both ends", "edge-grips", edges.fingerGrips) { on ->
      update { it.copy(fingerGrips = on) }
    }
    SwitchRow("Rubber feet", "edge-feet", edges.feet) { on -> update { it.copy(feet = on) } }
  }
}

@Composable
private fun GrooveFields(
  groove: JuiceGroove,
  update: PartUpdate<EdgeTreatments>,
) {
  fun groove(key: String, f: (JuiceGroove) -> JuiceGroove) =
    update.update(key) { e -> e.copy(juiceGroove = e.juiceGroove?.let(f)) }
  Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
    LengthField(
      label = "Width",
      value = groove.width,
      onValueChange = { w -> groove("edge-groove-width") { it.copy(width = w) } },
      testTag = "edge-groove-width",
      allowZero = false,
      modifier = Modifier.weight(1f),
    )
    LengthField(
      label = "Depth",
      value = groove.depth,
      onValueChange = { d -> groove("edge-groove-depth") { it.copy(depth = d) } },
      testTag = "edge-groove-depth",
      allowZero = false,
      modifier = Modifier.weight(1f),
    )
    LengthField(
      label = "Inset",
      value = groove.inset,
      onValueChange = { i -> groove("edge-groove-inset") { it.copy(inset = i) } },
      testTag = "edge-groove-inset",
      allowZero = false,
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun SwitchRow(label: String, tag: String, checked: Boolean, onChange: (Boolean) -> Unit) {
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    Switch(checked = checked, onCheckedChange = onChange, modifier = Modifier.testTag(tag))
  }
}

private fun EdgeProfile.label(): String =
  when (this) {
    EdgeProfile.NONE -> "None"
    EdgeProfile.CHAMFER -> "Chamfer"
    EdgeProfile.ROUNDOVER -> "Roundover"
  }

@OptIn(ExperimentalMaterial3Api::class) // ExposedDropdownMenuBox is still experimental
@Composable
private fun ProfileDropdown(selected: EdgeProfile, onSelect: (EdgeProfile) -> Unit) {
  var open by remember { mutableStateOf(false) }
  ExposedDropdownMenuBox(expanded = open, onExpandedChange = { open = it }) {
    OutlinedTextField(
      value = selected.label(),
      onValueChange = {},
      readOnly = true,
      singleLine = true,
      label = { Text("Edge profile") },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = open) },
      modifier =
        Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
          .fillMaxWidth()
          .testTag("edge-profile"),
    )
    ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
      EdgeProfile.entries.forEach { profile ->
        DropdownMenuItem(
          text = { Text(profile.label()) },
          onClick = {
            onSelect(profile)
            open = false
          },
          modifier = Modifier.testTag("edge-profile-${profile.name}"),
        )
      }
    }
  }
}
