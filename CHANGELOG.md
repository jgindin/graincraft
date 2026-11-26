# Changelog

Newest first. The version matches `version.properties`.

## 0.6 (not yet released)

- Board designs and the shop tool profile: stripes, checkerboard and brick patterns, edge
  treatments, stripe presets (uniform, alternating, mirror, Fibonacci, pinstripe), and versioned
  JSON files that refuse a design from a newer schema.

## 0.5 (not yet released)

- The wood catalog: 26 built-in species with Janka hardness, raw and oiled tones, pore type, oily,
  colour-bleeding and irritant flags, and edge- and end-grain cutting-board ratings with reasons.
  Custom species are rated by rule, and the worst rule wins.

## 0.4 (not yet released)

- Lengths stored as exact nanometres, a forgiving length parser (`1-3/4`, `¾`, `44,5 mm`), the unit
  formatter for 1/16, 1/32 and millimetre display, and lumber nominal sizes with board feet.

## 0.3 (not yet released)

- The Gradle build: modules `:core` (JVM and Wasm) and `:app` (Compose Desktop), ktfmt, detekt with a
  ban on Java imports in shared code, Kover coverage gate, Roborazzi screenshot checks and a `.deb`
  package. The app opens an empty "GrainCraft Studio" window.
