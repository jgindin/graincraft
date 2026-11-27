package com.jaygindin.graincraft.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Where the shell is: the rail's destination, the design open in the editor, and the requests the
 * shortcuts make. Leaving the editor, by any route, writes its last change first.
 *
 * @param onLeftEditor runs once the editor has gone, so the library cards show the design as it now
 *   is.
 */
internal class ShellNavigation(
  private val scope: CoroutineScope,
  private val controller: AppController,
  private val onLeftEditor: () -> Unit,
) {
  var destination by mutableStateOf(Destination.DESIGNS)

  /** A design opened from the library, shown instead of the Designs list. */
  var openDesignId by mutableStateOf<String?>(null)

  /** The new design dialog is showing. */
  var creatingDesign by mutableStateOf(false)

  /** The Woods search field should take the focus once it is showing. */
  var focusWoodSearch by mutableStateOf(false)

  private var leaving = false

  fun select(to: Destination) = leaveEditor { destination = to }

  fun back() = leaveEditor {}

  fun newDesign() = leaveEditor {
    destination = Destination.DESIGNS
    creatingDesign = true
  }

  fun findWood() = leaveEditor {
    destination = Destination.WOODS
    focusWoodSearch = true
  }

  private fun leaveEditor(then: () -> Unit) {
    if (openDesignId == null) return then()
    // A second click while the first leave is saving would otherwise save and leave twice.
    if (leaving) return
    leaving = true
    scope.launch {
      try {
        controller.flushOpenEditor()
      } finally {
        // A failed flush must not block every later leave.
        leaving = false
      }
      openDesignId = null
      onLeftEditor()
      then()
    }
  }
}

/** The shell's navigation, with the new design and find wood shortcuts registered. */
@Composable
internal fun rememberShellNavigation(
  controller: AppController,
  onLeftEditor: () -> Unit,
): ShellNavigation {
  val scope = rememberCoroutineScope()
  val navigation = remember { ShellNavigation(scope, controller, onLeftEditor) }
  RegisterActions(
    controller,
    mapOf(
      AppAction.NEW_DESIGN to navigation::newDesign,
      AppAction.FIND_WOOD to navigation::findWood,
    ),
  )
  return navigation
}

/** The Woods search field's focus, taken whenever [navigation] asks for it. */
@Composable
internal fun rememberWoodSearchFocus(navigation: ShellNavigation): FocusRequester {
  val requester = remember { FocusRequester() }
  // Once the Woods screen is showing, so its search field is there to take the focus.
  LaunchedEffect(navigation.destination, navigation.focusWoodSearch) {
    if (navigation.focusWoodSearch && navigation.destination == Destination.WOODS) {
      withFrameNanos {}
      requester.requestFocus()
      navigation.focusWoodSearch = false
    }
  }
  return requester
}
