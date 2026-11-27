package com.jaygindin.graincraft.app.ui.editor

/** The editor's tabs; the preview is a tab only when the window is too narrow for three panes. */
enum class EditorTab(val label: String) {
  PREVIEW("Preview"),
  ISSUES("Issues"),
  BUILD("Build"),
  LUMBER("Lumber");

  val tag: String
    get() = "tab-${name.lowercase()}"

  companion object {
    /** The tabs of the right pane. */
    val RESULTS = listOf(ISSUES, BUILD, LUMBER)
  }
}
