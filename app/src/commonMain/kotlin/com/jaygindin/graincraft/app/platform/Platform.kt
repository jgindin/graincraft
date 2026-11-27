package com.jaygindin.graincraft.app.platform

/** The system's file chooser and save dialogs. */
interface FileDialogs {
  /** Lets the user pick a `.graincraft.json` file; its text, or null when they cancel. */
  suspend fun openDesignFile(): String?

  /**
   * Lets the user pick where to save [contents], starting from [suggestedName] with [extension];
   * the saved path, or null when they cancel.
   */
  suspend fun saveFile(suggestedName: String, extension: String, contents: String): String?
}

/** Hands files and folders to the desktop's own apps. */
interface ExternalBrowser {
  /** Opens a file in its default app, such as a shop packet in the web browser. */
  fun openFile(path: String)

  /** Shows a folder in the file manager. */
  fun openFolder(path: String)
}

/** The current time in milliseconds since the epoch; a function so tests can fix it. */
typealias Clock = () -> Long
