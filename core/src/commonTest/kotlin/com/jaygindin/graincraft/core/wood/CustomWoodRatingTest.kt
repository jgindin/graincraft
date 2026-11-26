package com.jaygindin.graincraft.core.wood

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomWoodRatingTest {
  @Test
  fun hardDiffuseWoodIsRecommended() {
    val s = rateCustomWood(1450, PoreType.DIFFUSE, oily = false, irritant = false)
    assertEquals(Rating.RECOMMENDED, s.edgeGrain.rating)
    assertEquals(Rating.RECOMMENDED, s.endGrain.rating)
  }

  @Test
  fun softWoodIsCaution() {
    val s = rateCustomWood(850, PoreType.DIFFUSE, oily = false, irritant = false)
    assertEquals(Rating.CAUTION, s.edgeGrain.rating)
    assertEquals(Rating.CAUTION, s.endGrain.rating)
    assertTrue(s.edgeGrain.reasons.any { "soft" in it })
  }

  @Test
  fun veryLowJankaIsNotSuitable() {
    val s = rateCustomWood(550, PoreType.DIFFUSE, oily = false, irritant = false)
    assertEquals(Rating.NOT_SUITABLE, s.edgeGrain.rating)
    assertEquals(Rating.NOT_SUITABLE, s.endGrain.rating)
  }

  @Test
  fun openPoresHurtEndGrainMost() {
    val s = rateCustomWood(1200, PoreType.RING_OPEN, oily = false, irritant = false)
    assertEquals(Rating.CAUTION, s.edgeGrain.rating)
    assertEquals(Rating.NOT_SUITABLE, s.endGrain.rating)
  }

  @Test
  fun oilyIsCaution() {
    val s = rateCustomWood(1200, PoreType.DIFFUSE, oily = true, irritant = false)
    assertEquals(Rating.CAUTION, s.edgeGrain.rating)
    assertEquals(Rating.CAUTION, s.endGrain.rating)
  }

  @Test
  fun worstRuleWinsAndEveryReasonIsListed() {
    val s = rateCustomWood(550, PoreType.RING_OPEN, oily = true, irritant = true)
    assertEquals(Rating.NOT_SUITABLE, s.edgeGrain.rating)
    assertEquals(Rating.NOT_SUITABLE, s.endGrain.rating)
    assertEquals(4, s.edgeGrain.reasons.size)
    assertEquals(4, s.endGrain.reasons.size)
  }

  @Test
  fun derivedTonesDarkenFromTheBase() {
    val t = derivedTones(Rgb.hex("#C8A877"))
    assertTrue(t.latewood.luminance < t.base.luminance)
    assertTrue(t.pore.luminance < t.latewood.luminance)
  }
}
