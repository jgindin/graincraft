package com.jaygindin.graincraft.app.platform

/** The system's file chooser and save dialogs. */
interface FileDialogs {
  /** Lets the user pick a `.graincraft.json` file; its text, or null when they cancel. */
  suspend fun openDesignFile(): String?

  /**
   * Lets the user pick where to save [contents], starting from [suggestedName] with [extension]
   * (the dialog adds the extension). A write that fails is reported, never thrown, so a full disk
   * or a read-only folder costs only this one save.
   */
  suspend fun saveFile(suggestedName: String, extension: String, contents: String): SaveResult
}

/** How a save through [FileDialogs.saveFile] ended. */
sealed interface SaveResult {
  data class Saved(val path: String) : SaveResult

  data object Cancelled : SaveResult

  /** The file could not be written; [reason] is one line the user can read. */
  data class Failed(val reason: String) : SaveResult
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
