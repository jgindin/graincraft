package com.jaygindin.graincraft.core.design

import com.jaygindin.graincraft.core.units.Length
import com.jaygindin.graincraft.core.wood.WoodCatalog
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DesignJsonTest {
  private val inch = Length.inches(1)
  private val half = Length.inches(0, 1, 2)

  private val custom =
    WoodCatalog.builtIns.first().copy(id = "custom-1", name = "Backyard oak", custom = true)

  private fun design(pattern: PatternSpec, edges: EdgeTreatments = EdgeTreatments()) =
    BoardDesign(
      id = "11111111-2222-3333-4444-555555555555",
      name = "Test",
      revision = 3,
      updatedAtMillis = 1_700_000_000_000,
      grainMode = GrainMode.END_GRAIN,
      finished = Dimensions(Length.inches(18), Length.inches(12), Length.inches(1, 1, 2)),
      pattern = pattern,
      edges = edges,
      embeddedCustomWoods = listOf(custom),
    )

  @Test
  fun roundTripsEveryPattern() {
    val edges =
      EdgeTreatments(
        cornerRadius = half,
        edgeProfile = EdgeProfile.ROUNDOVER,
        edgeProfileSize = Length.inches(0, 1, 4),
        juiceGroove = JuiceGroove(half, Length.inches(0, 1, 4), inch),
        fingerGrips = true,
        feet = true,
      )
    val patterns =
      listOf(
        PatternSpec.Stripes(listOf(Strip("walnut", inch), Strip("custom-1", half))),
        PatternSpec.Checkerboard("walnut", "hard-maple", Length.inches(1, 1, 2)),
        PatternSpec.Brick(
          "cherry",
          "hard-maple",
          Length.inches(3),
          Length.inches(1, 1, 2),
          Length.inches(0, 1, 4),
        ),
      )
    for (p in patterns) {
      val d = design(p, edges)
      assertEquals(DesignDecode.Ok(d), DesignJson.decode(DesignJson.encode(d)))
    }
  }

  @Test
  fun patternsCarryATypeDiscriminator() {
    val text = DesignJson.encode(design(PatternSpec.Checkerboard("a", "b", inch)))
    assertContains(text, "\"type\": \"checkerboard\"")
  }

  @Test
  fun newerSchemaIsRefusedEvenWhenTheShapeDiffers() {
    assertEquals(
      DesignDecode.NewerSchema(2),
      DesignJson.decode("""{"schemaVersion": 2, "somethingNew": [1, 2]}"""),
    )
  }

  @Test
  fun garbageIsInvalid() {
    assertIs<DesignDecode.Invalid>(DesignJson.decode("{"))
    assertIs<DesignDecode.Invalid>(DesignJson.decode("[]"))
    assertIs<DesignDecode.Invalid>(DesignJson.decode("""{"schemaVersion": 1}"""))
  }

  @Test
  fun lengthsAreStoredAsNanometres() {
    val text = DesignJson.encode(design(PatternSpec.Checkerboard("a", "b", inch)))
    assertContains(text, "25400000")
  }
}
