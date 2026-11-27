package com.jaygindin.graincraft.app.ui.library

private const val MILLIS_PER_DAY = 86_400_000L

/**
 * The UTC calendar date of [millis] as `2026-11-26`. Common code has no time-zone database, and a
 * date is all a card needs.
 */
internal fun formatDate(millis: Long): String {
  // Howard Hinnant's days-to-civil algorithm, with days counted from 1970-01-01.
  val z = millis.floorDiv(MILLIS_PER_DAY) + 719_468
  val era = z.floorDiv(146_097)
  val dayOfEra = z - era * 146_097
  val yearOfEra = (dayOfEra - dayOfEra / 1_460 + dayOfEra / 36_524 - dayOfEra / 146_096) / 365
  val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
  val mp = (5 * dayOfYear + 2) / 153
  val day = dayOfYear - (153 * mp + 2) / 5 + 1
  val month = if (mp < 10) mp + 3 else mp - 9
  val year = yearOfEra + era * 400 + if (month <= 2) 1 else 0
  return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}
