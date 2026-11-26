package com.jaygindin.graincraft.core.units

enum class LengthUnit {
  INCH,
  MILLIMETRE,
}

sealed interface LengthParse {
  data class Ok(val length: Length) : LengthParse

  data class Error(val reason: String) : LengthParse
}

private val VULGAR_FRACTIONS =
  mapOf(
    '½' to "1/2",
    '¼' to "1/4",
    '¾' to "3/4",
    '⅛' to "1/8",
    '⅜' to "3/8",
    '⅝' to "5/8",
    '⅞' to "7/8",
  )

private val DECIMAL = Regex("""\d{1,6}(?:\.\d{1,6})?""")
private val MIXED = Regex("""(?:(\d{1,6})[- ])?(\d{1,6})/(\d{1,6})""")
private const val MM_PER_CM = 10
private val WHITESPACE = Regex("""\s+""")

private enum class Suffix(val text: String, val metric: Boolean) {
  MM("mm", true),
  CM("cm", true),
  IN("in", false),
  QUOTE("\"", false),
}

/**
 * Reads what a woodworker types: `1-3/4`, `1 3/4"`, `1.75 in`, `3/32`, `¾`, `44,5 mm`, `4.45cm`.
 * Fractions are always inches; a bare decimal uses [defaultUnit].
 */
fun parseLength(text: String, defaultUnit: LengthUnit): LengthParse {
  val normalised = normalise(text)
  if (normalised.isEmpty()) return LengthParse.Error("Enter a length")
  val suffix = Suffix.entries.firstOrNull { normalised.endsWith(it.text) }
  var s = if (suffix != null) normalised.removeSuffix(suffix.text).trim() else normalised
  val explicitMetric = suffix?.metric == true
  // A comma is a decimal point only next to a metric unit; "1,5" in inches stays an error.
  if (explicitMetric) s = s.replace(',', '.')

  val fraction = MIXED.matchEntire(s)
  return when {
    fraction != null && explicitMetric -> LengthParse.Error("Fractions are inches, not millimetres")
    fraction != null -> parseFraction(fraction.groupValues)
    DECIMAL.matches(s) -> LengthParse.Ok(decimalLength(s.toDouble(), suffix, defaultUnit))
    else -> LengthParse.Error("Not a length: ${text.trim()}")
  }
}

private fun decimalLength(value: Double, suffix: Suffix?, defaultUnit: LengthUnit): Length {
  val metric = suffix?.metric ?: (defaultUnit == LengthUnit.MILLIMETRE)
  return when {
    !metric -> Length.inchesApprox(value)
    suffix == Suffix.CM -> Length.millimetres(value * MM_PER_CM)
    else -> Length.millimetres(value)
  }
}

private fun parseFraction(groups: List<String>): LengthParse {
  val whole = groups[1].ifEmpty { "0" }.toInt()
  val numerator = groups[2].toInt()
  val denominator = groups[3].toInt()
  if (denominator == 0) return LengthParse.Error("The denominator cannot be zero")
  return try {
    LengthParse.Ok(Length.inches(whole, numerator, denominator))
  } catch (_: IllegalArgumentException) {
    LengthParse.Error("Use halves, quarters, eighths, 16ths, 32nds or 64ths")
  }
}

private fun normalise(text: String): String {
  val sb = StringBuilder()
  for (c in text.trim().lowercase()) {
    val vulgar = VULGAR_FRACTIONS[c]
    when {
      vulgar != null -> sb.append(' ').append(vulgar)
      c == '″' || c == '”' -> sb.append('"')
      else -> sb.append(c)
    }
  }
  return sb.toString().trim().replace(WHITESPACE, " ").replace(" -", "-").replace("- ", "-")
}
