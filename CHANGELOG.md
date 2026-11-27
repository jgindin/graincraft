# Changelog

Newest first. The version matches `version.properties`.

## 0.22 (not yet released)

- Board preview (no editor screen yet): the finished board drawn piece by piece with each
  species' grain texture in raw or oiled tones, rounded corners, a juice groove, a soft shadow and
  glue lines. End-grain cells cut from the same strip show the same rings, mirrored in rotated
  rows. Zoom from 1/4 to 8 times with the scroll wheel or buttons, pan by dragging, fit to view,
  a hover tooltip with species, strip and slice, and a dimensions label with the target in
  brackets when it differs. Textures are cached at power-of-two densities.
- Grain textures: end-grain rings are much stronger and wider apart, with a crisp latewood band,
  a slightly lighter earlywood on dark woods and a pith just outside the piece, so rings read as
  clear arcs on a whole-board view. Latewood tones are now 0.68 of the base (pores 0.52).

## 0.21 (not yet released)

- Editor view model (no screen yet): edits with 100 steps of undo and redo, a compile 150 ms after
  the last change and whenever the woods, shop profile or units change, and an autosave 1 s after
  the last change that bumps the revision and embeds a copy of each custom wood the design uses.
  Switching a checkerboard or brick design to edge grain asks first and then starts from the
  default stripes. A failed save shows an error and is retried on the next edit or on close.

## 0.20 (not yet released)

- Woods screen: a searchable list of the 26 built-in and the custom species with raw and oiled
  swatches, Janka, pore type, Edge and End rating chips, flag icons for oily, bleeds and irritant,
  expandable reasons and notes, and the filters Recommended only, Hide not suitable and Custom.
- Custom woods: add, edit and delete with a computed rating (shown live), hex colours with a live
  swatch, and an override that needs a reason. Saved in `custom-woods.json`.
- Shop screen: every shop profile field in cards (Saws, Planer and sander, Flattening, Allowances,
  Lumber) with help text, saved at once, and "Reset to defaults" with a confirmation.
- Library thumbnails are rebuilt when the woods or the shop profile change.

## 0.19 (not yet released)

- Designs library: a grid of cards with a flat top-view thumbnail, name, size, pattern, grain and
  last edited date. New design dialog (name, grain, pattern, size presets or custom), Import and
  Export of `.graincraft.json` files, rename, duplicate, and delete with an Undo snackbar. Files
  that cannot be read show as "Can't open" cards. The trash is purged once at start. Opening a
  design shows a placeholder until the editor is built.

## 0.18 (not yet released)

- App shell: a navigation rail with Designs, Woods, Shop and Settings (the first three are
  placeholders for now), `AppGraph` for the platform services (`WorkspaceStore`, `FileDialogs`,
  `ExternalBrowser`, clock, id source), and `LocalUnitFormatter` for the chosen units.
- Settings: units (Imperial / Metric) with an example length, fraction precision (1/16" or 1/32"),
  theme (System / Light / Dark), the data folder with "Open folder", and About with the version
  generated from `version.properties`. Changes are saved to `settings.json` at once.
- The window opens at 1280 × 800 (minimum 1024 × 700) and remembers its position and size; a saved
  position that is on no current screen is ignored.
- Shop (saw) and Woods (tree) rail icons redrawn so they read clearly at 24 dp. Screenshot goldens
  for Settings in light and dark.

## 0.17 (not yet released)

- Workspace storage: `WorkspaceStore` with a file implementation under
  `$XDG_DATA_HOME/graincraft-studio/` (default `~/.local/share/graincraft-studio/`). Designs,
  a 30-day trash, shop profile, custom woods and `AppSettings` are written atomically (temp file,
  then rename). Unreadable or newer-version design files are listed with a reason and never
  overwritten, and trash and restore never replace an existing file.

## 0.16 (not yet released)

- App theme: `GrainCraftTheme(dark)` with warm wood-toned light and dark colour schemes (every
  role set), shapes, `Spacing`, a type scale in bundled Noto Sans (SIL OFL, licence beside the
  fonts), and rating colours through `LocalRatingColors`. Twenty-six Material Symbols vector
  icons. `ThemeTest` checks contrast, minimum text sizes, the bundled font, icon loading, and that
  screens use no colour or font-size literals.

## 0.15 (not yet released)

- Shop packet: `ShopPacketExporter` builds one self-contained, print-friendly HTML page with the
  board drawing in species colours, issues, shopping list, cut list, every build stage with its
  diagram, and the shop settings used. User text is escaped and every length goes through the
  unit formatter.

## 0.14 (not yet released)

- Grain textures: pixel textures for the 2D preview. End grain shows noise-warped growth rings
  around a pith outside the piece, with pore dots for ring-porous and semi-ring woods; long grain
  shows fine wavy lines along the board. Cells cut from one strip share a seed, so their rings
  match, and a mirrored render flips them left to right. Output is identical on every platform.

## 0.13 (not yet released)

- The build guide and the design compiler. A design compiles in one pure step into its fitted
  size, panels, layout, cut list, lumber, warnings and a numbered guide of the stages it needs:
  buying and milling, preparing (an acetone wipe for oily woods, prompt squeeze-out wiping for
  woods that bleed), gluing and flattening the panels, crosscutting, arranging and gluing the
  slices, flattening (always warning that end grain never goes through a planer), trimming, edge
  details in a safe order, and finishing, with feet last. Glue-up, crosscut and arrangement
  diagrams are drawn as plain SVG. A design that cannot be built lists every unknown wood, or the
  first other problem, as a plain sentence. A design's own custom woods are used even when they
  are missing from the woods list.

## 0.12 (not yet released)

- Woodworking warnings: species rated Caution or Not suitable for the board's grain, a Janka
  spread over 1100 lbf, oily woods, a colour-bleeding wood beside a lighter one, strips or mortar
  lines under 1/8", and end-grain boards thinner than 1-1/4". They are listed worst first and
  never block anything.

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
