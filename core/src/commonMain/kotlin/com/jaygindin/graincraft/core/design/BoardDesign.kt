package com.jaygindin.graincraft.core.design

import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.wood.WoodSpecies
import kotlinx.serialization.Serializable

enum class GrainMode {
  EDGE_GRAIN,
  END_GRAIN,
}

@Serializable data class Dimensions(val length: Length, val width: Length, val thickness: Length)

/** One saved cutting-board design. `revision` goes up by one on every saved change. */
@Serializable
data class BoardDesign(
  val schemaVersion: Int = DesignJson.SCHEMA_VERSION,
  val id: String,
  val name: String,
  val revision: Long,
  val updatedAtMillis: Long,
  val grainMode: GrainMode,
  val finished: Dimensions,
  val pattern: PatternSpec,
  val edges: EdgeTreatments,
  val embeddedCustomWoods: List<WoodSpecies> = emptyList(),
)
