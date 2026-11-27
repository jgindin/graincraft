package com.jaygindin.graincraft.app.data

private const val APP_DIR = "graincraft-studio"

/** `$XDG_DATA_HOME/graincraft-studio`, or `~/.local/share/graincraft-studio` when it is unset. */
fun xdgDataRoot(env: Map<String, String>, home: String): String {
  val base = env["XDG_DATA_HOME"]?.takeIf { it.isNotBlank() } ?: "${home.trimEnd('/')}/.local/share"
  return "${base.trimEnd('/')}/$APP_DIR"
}
