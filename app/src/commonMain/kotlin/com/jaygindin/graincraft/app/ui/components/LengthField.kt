package com.jaygindin.graincraft.app.ui.components

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import com.jaygindin.graincraft.app.ui.LocalUnitFormatter
import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.units.LengthParse
import com.jaygindin.graincraft.core.units.parseLength

/**
 * A text field for a length. While the user is editing it keeps exactly what they typed, shows why
 * it is not a length yet, and commits to [onValueChange] each time the text is one; otherwise it
 * shows [value] in the chosen units.
 *
 * @param supportingText help shown under the field when the text is valid.
 * @param allowZero false when the build math needs a length above zero.
 */
@Composable
fun LengthField(
  label: String,
  value: Length,
  onValueChange: (Length) -> Unit,
  modifier: Modifier = Modifier,
  testTag: String,
  supportingText: String? = null,
  allowZero: Boolean = true,
) {
  val formatter = LocalUnitFormatter.current
  // Non-null only while the field has been edited and not yet left.
  var typed by remember { mutableStateOf<String?>(null) }
  var error by remember { mutableStateOf<String?>(null) }
  OutlinedTextField(
    value = typed ?: formatter.format(value),
    onValueChange = { text ->
      typed = text
      when (val parsed = parseLength(text, formatter.unit)) {
        is LengthParse.Error -> error = parsed.reason
        is LengthParse.Ok ->
          if (!allowZero && parsed.length <= Length.ZERO) {
            error = "Must be more than zero"
          } else {
            error = null
            if (parsed.length != value) onValueChange(parsed.length)
          }
      }
    },
    label = { Text(label) },
    singleLine = true,
    isError = error != null,
    supportingText = (error ?: supportingText)?.let { { Text(it) } },
    modifier =
      modifier.testTag(testTag).onFocusChanged {
        if (!it.isFocused) {
          typed = null
          error = null
        }
      },
  )
}
