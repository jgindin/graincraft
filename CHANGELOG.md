# Changelog

Newest first. The version matches `version.properties`.

## 0.11 (not yet released)

- The cut list and lumber: every milled part with its species, count, size, the nominal stock it
  comes from, the thickness it is planed to and the width it is ripped to, with identical parts
  merged. Wide edge-grain strips are laminated from two boards, thin strips are ripped from thick
  stock when that takes less lumber, and parts too thick for any stock say so. Board feet per
  species include kerf and milling waste, shown to 0.1 BF with a total.

## 0.10 (not yet released)

- Laying out the finished board: every piece on its face as a rectangle in board coordinates, with
  its species, fibre direction, the panel-1 strip it came from, its slice row, and whether that row
  is rotated or shifted. Rows are centred on the board and trimmed to the finished size; wide
  edge-grain strips show as two laminated pieces.

## 0.9 (not yet released)

- Turning a fitted pattern into a build: the long-grain panels (edge-grain board, end-grain panel 1
  and the brick mortar panel) with milled strip widths, heights and lengths, split into sub-panels
  where a tool is too narrow; slice thickness and counts; and the rows of the final glue-up, with
  checkerboard rows rotated and brick rows shifted as whole rows.

## 0.8 (not yet released)

- Choosing the long-grain flattening tool (planer, drum sander, or the end-grain method) and
  splitting panels that are too wide into sub-panels at strip boundaries: fewest sub-panels, then
  the smallest widest, then the widest narrowest, then the leftmost boundaries. Mortar panels split
  into equal parts.

## 0.7 (not yet released)

- Fitting patterns to the target size: stripe widths scale to the board width with the rounding
  remainder on the widest strip, checkerboards keep square cells on an even column count and report
  the actual length, bricks fill the length exactly. Size and grain-mode checks return a
  `CompileError`.

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
