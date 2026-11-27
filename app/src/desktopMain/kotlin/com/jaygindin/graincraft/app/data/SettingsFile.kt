package com.jaygindin.graincraft.app.data

import com.jaygindin.graincraft.core.design.WorkspaceJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

private const val SCHEMA_VERSION = 1
private const val VERSION_KEY = "schemaVersion"

/**
 * One of the app's own files beside the designs, as `{"schemaVersion": 1, ...fields}`. A list is
 * kept under [listKey]. A file without a version is version 1, and a bare list is read as the
 * version 1 list.
 *
 * @param stem the file name without `.json`.
 * @param label what the file holds, for a sentence like "Couldn't read your [label]".
 */
internal class SettingsFile<T>(
  val stem: String,
  val label: String,
  private val serializer: KSerializer<T>,
  private val listKey: String? = null,
  val default: () -> T,
) {
  val fileName = "$stem.json"

  fun encode(value: T): String {
    val body = WorkspaceJson.json.encodeToJsonElement(serializer, value)
    val fields = if (listKey != null) mapOf(listKey to body) else (body as JsonObject).toMap()
    return WorkspaceJson.json.encodeToString(
      JsonObject.serializer(),
      JsonObject(mapOf(VERSION_KEY to JsonPrimitive(SCHEMA_VERSION)) + fields),
    )
  }

  /** Null when the text is not this file or was written by a newer version. */
  fun decode(text: String): T? =
    try {
      when (val root = WorkspaceJson.json.parseToJsonElement(text)) {
        is JsonArray if listKey != null -> decodeBody(root)
        is JsonObject -> if (version(root) > SCHEMA_VERSION) null else decodeObject(root)
        else -> null
      }
    } catch (_: IllegalArgumentException) {
      // SerializationException is an IllegalArgumentException, and so is a non-numeric version.
      null
    }

  private fun decodeObject(root: JsonObject): T? =
    if (listKey == null) decodeBody(JsonObject(root - VERSION_KEY))
    else root[listKey]?.let(::decodeBody)

  private fun decodeBody(body: JsonElement): T =
    WorkspaceJson.json.decodeFromJsonElement(serializer, body)

  private fun version(root: JsonObject): Int =
    root[VERSION_KEY]?.let {
      requireNotNull(it.jsonPrimitive.intOrNull) { "schemaVersion is not a whole number" }
    } ?: SCHEMA_VERSION
}
