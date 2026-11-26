package com.jaygindin.graincraft.core.wood

private const val SOFT_BELOW = 900
private const val TOO_SOFT_BELOW = 600
private const val LATEWOOD_FACTOR = 0.85
private const val PORE_FACTOR = 0.65
private const val CHANNEL = 0xFF
private const val RED_BITS = 16
private const val GREEN_BITS = 8

/** Rates a user-entered species; the worst applicable rule wins for each grain. */
fun rateCustomWood(
  jankaLbf: Int,
  poreType: PoreType,
  oily: Boolean,
  irritant: Boolean,
): Suitability {
  val edge = mutableListOf<Pair<Rating, String>>()
  val end = mutableListOf<Pair<Rating, String>>()

  fun both(rating: Rating, reason: String) {
    edge += rating to reason
    end += rating to reason
  }

  when {
    jankaLbf < TOO_SOFT_BELOW -> both(Rating.NOT_SUITABLE, "Too soft for a cutting board.")
    jankaLbf < SOFT_BELOW -> both(Rating.CAUTION, "Fairly soft; the surface will scar quickly.")
  }
  if (poreType == PoreType.RING_OPEN) {
    val reason = "Open pores with no tyloses wick moisture and trap bacteria."
    edge += Rating.CAUTION to reason
    end += Rating.NOT_SUITABLE to reason
  }
  if (irritant) both(Rating.CAUTION, "Irritant or sensitiser dust.")
  if (oily) both(Rating.CAUTION, "Oily; wipe with acetone before gluing.")

  return Suitability(grade(edge), grade(end))
}

private fun grade(entries: List<Pair<Rating, String>>): GrainRating =
  GrainRating(
    rating = entries.maxOfOrNull { it.first } ?: Rating.RECOMMENDED,
    reasons = entries.map { it.second },
  )

/** Derives latewood and pore tones by darkening [base]. */
fun derivedTones(base: Rgb): WoodTones =
  WoodTones(base = base, latewood = darken(base, LATEWOOD_FACTOR), pore = darken(base, PORE_FACTOR))

private fun darken(c: Rgb, factor: Double): Rgb {
  fun ch(shift: Int) = (((c.argb shr shift) and CHANNEL) * factor).toInt()
  return Rgb.of(ch(RED_BITS), ch(GREEN_BITS), ch(0))
}
