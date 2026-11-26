# Changelog

Newest first. The version matches `version.properties`.

## 0.4 (not yet released)

- Lengths stored as exact nanometres, a forgiving length parser (`1-3/4`, `¾`, `44,5 mm`), the unit
  formatter for 1/16, 1/32 and millimetre display, and lumber nominal sizes with board feet.

## 0.3 (not yet released)

- The Gradle build: modules `:core` (JVM and Wasm) and `:app` (Compose Desktop), ktfmt, detekt with a
  ban on Java imports in shared code, Kover coverage gate, Roborazzi screenshot checks and a `.deb`
  package. The app opens an empty "GrainCraft Studio" window.
