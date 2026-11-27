package com.jaygindin.graincraft.app

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/**
 * The app action for [key] pressed with these modifiers, or null for a key the app leaves to
 * whatever has focus. Every shortcut needs Ctrl; Shift only goes with Z, for redo.
 */
internal fun shortcutAction(
  key: Key,
  ctrl: Boolean,
  shift: Boolean = false,
  alt: Boolean = false,
  meta: Boolean = false,
): AppAction? {
  if (!ctrl || alt || meta) return null
  return if (shift) {
    if (key == Key.Z) AppAction.REDO else null
  } else {
    when (key) {
      Key.N -> AppAction.NEW_DESIGN
      Key.Z -> AppAction.UNDO
      Key.Y -> AppAction.REDO
      Key.P -> AppAction.EXPORT_PACKET
      Key.F -> AppAction.FIND_WOOD
      else -> null
    }
  }
}

/**
 * Handles a key press seen by the window before any field: true when it was one of the app's
 * shortcuts and a screen on show did it.
 */
fun AppController.onKeyEvent(event: KeyEvent): Boolean {
  val action =
    if (event.type == KeyEventType.KeyDown) {
      shortcutAction(
        event.key,
        ctrl = event.isCtrlPressed,
        shift = event.isShiftPressed,
        alt = event.isAltPressed,
        meta = event.isMetaPressed,
      )
    } else {
      null
    }
  return action != null && perform(action)
}
