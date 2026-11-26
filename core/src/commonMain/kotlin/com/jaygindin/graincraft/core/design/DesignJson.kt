package com.jaygindin.graincraft.core.design

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** The JSON settings shared by every file the app reads and writes. */
object WorkspaceJson {
  val json: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = true
  }
}

sealed interface DesignDecode {
  data class Ok(val design: BoardDesign) : DesignDecode

  data class NewerSchema(val version: Int) : DesignDecode

  data class Invalid(val reason: String) : DesignDecode
}

object DesignJson {
  const val SCHEMA_VERSION = 1

  fun encode(design: BoardDesign): String =
    WorkspaceJson.json.encodeToString(BoardDesign.serializer(), design)

  /** Checks `schemaVersion` first, so a newer file is refused even if its shape has changed. */
  fun decode(text: String): DesignDecode =
    try {
      val root = WorkspaceJson.json.parseToJsonElement(text).jsonObject
      val version = schemaVersion(root)
      if (version > SCHEMA_VERSION) {
        DesignDecode.NewerSchema(version)
      } else {
        DesignDecode.Ok(WorkspaceJson.json.decodeFromJsonElement(BoardDesign.serializer(), root))
      }
    } catch (e: IllegalArgumentException) {
      // SerializationException and JsonDecodingException are both IllegalArgumentExceptions.
      DesignDecode.Invalid(e.message ?: "Not a GrainCraft design file")
    }

  private fun schemaVersion(root: JsonObject): Int = root["schemaVersion"]?.jsonPrimitive?.int ?: 1
}
