# Phase 1 design: core and Linux desktop

Part of the [roadmap](../roadmap.md), which explains what GrainCraft Studio is for and why.

## Approaches considered for phase 1

1. **Desktop, plus a web compile check on the core (chosen by the owner).** The core module also
   targets Kotlin/Wasm from day one, and its tests run there too. No web app is built. This keeps
   Java-only APIs out of shared code, so later ports need no rework. It costs a slightly longer
   build.
2. **Desktop only.** Simplest and fastest build. Rejected because with a single JVM target Kotlin
   lets shared code use Java APIs, which would surface as rework in phases 5 and 7.
3. **Desktop-only, non-multiplatform project, ported later.** Rejected: guaranteed rework.

## Goals

- Design a board in **edge grain** (stripes) or **end grain** (stripes, checkerboard, brick) from a
  target finished length, width and thickness.
- A **wood catalog** of 26 built-in species, each with a cutting-board rating for edge grain and
  for end grain and the reasons for it. You can add your own species.
- **Woodworking warnings** that never block you: unsuitable species, Janka hardness spread, oily
  woods, colour bleed, too-thin strips, too-thin boards.
- A **shop tool profile** (kerf, planer width, crosscut capacity, flattening method, allowances)
  that the build math uses.
- **Build math** that works backwards from the finished size, including splitting a panel that is
  too wide for the planer or crosscut sled.
- A **build guide** of numbered stages, each with text and a diagram; a **cut list** and a
  **lumber shopping list** in board feet.
- An accurate **2D top-down preview** with grain, in Raw or Oiled finish.
- A printable **shop packet**: one self-contained HTML file, opened in the browser to print or save
  as PDF.
- **Local saving** with autosave, a design library, and import / export of `.graincraft.json`
  files.
- Imperial (fractions) or metric display; inputs accept either.
- A **Material 3 Compose** desktop UI following the conventions of the owner's straphanger app.

## Non-goals for phase 1

- The other five patterns, face grain, scrap bin (phase 2); 3D (phase 3); photo import (phase 4);
  Android, web, Windows, sync, prompts and AI (later phases).
- Cost estimates and lumber prices.
- Multi-user anything.

## Anti-goals

- A preview that looks nice but doesn't match the build (wrong grain direction, cells that ignore
  which strip they came from, dimensions that disagree with the cut list).
- Build math that is "close enough": every number on the cut list must follow from the formulas
  below and the shop profile, and be covered by tests.
- Warnings that stop the user. They inform; the user decides.
- UI made of hand-rolled widgets where a Material 3 component exists, or colours and font sizes
  written as literals in screens.

## Constraints

- Kotlin Multiplatform with Compose Multiplatform. Start from the versions in straphanger's
  `gradle/libs.versions.toml` (Kotlin 2.4.21, Compose Multiplatform 1.12.2, kotlinx-serialization
  1.13.0, coroutines 1.11.0, kotlinx-io 0.9.3, FileKit 0.17.0, lifecycle 2.11.0, ktfmt plugin
  0.28.0, detekt 2.0.0-alpha.6, Kover 0.9.11), bumping each to the newest stable release when the
  project is created.
- The core module has no platform code and builds for JVM and Wasm.
- Linux desktop is the only app target in phase 1.
- No network access in phase 1.

## Project layout

```
graincraft/
  settings.gradle.kts, build.gradle.kts, gradle/libs.versions.toml, gradle.properties
  version.properties          versionName, e.g. 0.1
  CHANGELOG.md, README.md
  docs/CODE-TOUR.md           kept current, as in straphanger
  config/detekt/detekt.yml
  core/                       KMP library: jvm() and wasmJs { nodejs() }
    src/commonMain/kotlin/com/jaygindin/graincraft/core/...
    src/commonTest/kotlin/...
  app/                        Compose Multiplatform: jvm("desktop") target only in phase 1
    src/commonMain/kotlin/com/jaygindin/graincraft/app/...   all UI, view models, theme
    src/commonMain/composeResources/drawable/...             Material Symbols as vector XML
    src/desktopMain/kotlin/...                               main(), file storage, browser, dialogs
    src/desktopTest/kotlin/...                               UI tests
  scripts/run-desktop.sh, scripts/ci.sh
```

- `app/commonMain` holds everything that will later be shared with Android and web: screens, view
  models, theme. Because `app` has only a JVM target in phase 1, Kotlin would let `commonMain` use
  Java APIs. A detekt `ForbiddenImport` rule bans `java.*` and `javax.*` imports in every
  `commonMain` source set to prevent this.
- Platform services (`WorkspaceStore`, `FileDialogs`, `ExternalBrowser`, `Clock`) are interfaces
  in `app/commonMain`, implemented in `desktopMain`, and passed in through a small hand-written
  `AppGraph` (constructor injection, no DI framework).
- Package root: `com.jaygindin.graincraft`. Desktop package name `GrainCraft Studio`.
- Tooling as in straphanger: ktfmt (Google style, 100 columns), detekt (build on the default
  config), Kover, Roborazzi. `scripts/ci.sh` runs `./gradlew ktfmtCheck detekt :core:allTests
  :app:desktopTest verifyRoborazziDesktop koverVerify :app:packageDeb`.
- Packaging: `createDistributable` for local runs and `packageDeb` (the owner runs Ubuntu).

## Core domain (`core`)

All core types are immutable and `@Serializable` where they are saved. Every function that turns a
design into output is pure and deterministic.

### Lengths and units

- `Length` is a value class holding **nanometres as a `Long`**. This represents 1/64" (396,875 nm)
  and 0.001 mm exactly, so imperial and metric sums never drift.
- `Length.parse(text, defaultUnit)` accepts `1-3/4`, `1 3/4`, `1.75`, `3/32`, `1-3/4"`, `44.5mm`,
  `44.5 mm`, `4.5cm`. A bare number uses the user's unit setting. Errors return a typed failure
  that the UI shows as field error text.
- `UnitFormatter` displays imperial as reduced fractions rounded to the nearest 1/16" (setting:
  1/16 or 1/32), e.g. `1-3/4"`, `3/32"`, `13"`; metric as millimetres with one decimal.
- **Board feet:** `BF = nominal thickness (in) × width (in) × length (in) / 144`. Nominal
  thicknesses: 4/4, 5/4, 6/4, 8/4, with default surfaced (S2S) thicknesses 13/16", 1-1/16",
  1-5/16", 1-3/4", editable in the shop profile.

### Coordinates

- **X** = board length (the long side), **Y** = board width, **Z** = thickness. The finished board
  is `L × W × T`.
- Each piece has a **fibre direction**: `X` (long grain running along the board, edge-grain
  boards) or `Z` (end grain facing up, end-grain boards). `Y` is reserved for later patterns.

### Wood catalog

`WoodSpecies`: `id`, `name`, `botanicalName`, `jankaLbf`, `rawColor` and `oiledColor` (sRGB hex;
two or three tones per finish for grain contrast: base, latewood/ring, pore), `poreType`
(`DIFFUSE`, `SEMI_RING`, `RING_OPEN`, `RING_CLOSED` (pores closed by tyloses)), `oily`,
`bleedsColor`, `irritant` (dust or skin sensitiser), `suitability` (`edgeGrain` and `endGrain`, each
`RECOMMENDED`, `CAUTION` or `NOT_SUITABLE`, with reasons), and `notes`.

Built-in species and ratings (Janka values are the commonly published figures). **The owner should
review this table before it's coded; the ratings are my call from common woodworking guidance.**

| Species | Janka | Edge grain | End grain | Reasons and notes |
|---|---|---|---|---|
| Hard Maple | 1450 | Recommended | Recommended | Closed pores, hard, the standard choice. |
| Ambrosia Maple | 950 | Caution | Caution | Beetle holes can trap food and moisture; fill voids with epoxy. Softer than hard maple. |
| Black Walnut | 1010 | Recommended | Recommended | |
| American Cherry | 950 | Recommended | Recommended | Darkens noticeably with light. |
| White Oak | 1360 | Recommended | Recommended | Pores are closed by tyloses. |
| Red Oak | 1290 | Caution | Not suitable | Open pores with no tyloses wick moisture and trap bacteria, worst on end grain. Use white oak or hard maple. |
| White Ash | 1320 | Caution | Caution | Large open pores. |
| Hickory | 1820 | Recommended | Recommended | Moves a lot with humidity. |
| European Beech | 1300 | Recommended | Recommended | Moves a lot with humidity. |
| Yellow Birch | 1260 | Recommended | Recommended | |
| Purpleheart | 2520 | Recommended | Recommended | Irritant dust. Fades to brown with light. |
| African Padauk | 1970 | Recommended | Recommended | Irritant dust. Colour bleeds into light woods during glue-up and oiling. |
| Yellowheart | 1820 | Recommended | Recommended | Irritant dust. |
| Bloodwood | 2900 | Recommended | Recommended | Very hard, so watch the Janka spread. Colour can bleed. |
| Zebrawood | 1575 | Caution | Caution | Open, splintery grain; irritant. |
| Wenge | 1630 | Caution | Caution | Splinters easily (splinters fester); coarse open grain; sensitiser; oily. |
| Sapele | 1410 | Recommended | Recommended | Interlocked grain tears out when planed. |
| Jatoba | 2690 | Recommended | Recommended | Mild sensitiser. |
| Teak | 1070 | Caution | Caution | Silica dulls knives quickly; oily. |
| Bubinga | 1980 | Caution | Caution | Sensitiser. |
| Osage Orange | 2040 | Recommended | Recommended | Irritant dust. Turns brown with light. |
| Bocote | 2010 | Caution | Caution | Oily; sensitiser. |
| Eastern White Pine | 380 | Not suitable | Not suitable | Far too soft; resin. |
| Western Red Cedar | 350 | Not suitable | Not suitable | Far too soft; strong irritant; aromatic taste. |
| Spalted Maple | 700 | Not suitable | Not suitable | Fungal decay leaves soft, punky areas. |
| Cocobolo | 1136 | Not suitable | Not suitable | Strong sensitiser; very oily. |

Oily species (acetone wipe before glue): Teak, Wenge, Bocote, Cocobolo. Colour-bleeding species:
African Padauk, Bloodwood.

**Custom species.** The user enters name, Janka, pore type, oily, bleeds, irritant, and raw and
oiled base colours as hex with a live swatch (Compose has no colour picker; the other tones are
derived by darkening the base). Their rating is computed by rules (below) and the user can
override it with their own reason. Rating rules for custom woods:

- Janka under 900 lbf: Caution; under 600: Not suitable ("too soft").
- `RING_OPEN`: edge grain Caution, end grain Not suitable.
- Irritant or oily: Caution.
- Otherwise Recommended. The worst rule wins.

Custom woods are saved in `custom-woods.json`. A saved design embeds a copy of each custom species
it uses, so an exported design file is self-contained.

### Designs

```kotlin
@Serializable data class BoardDesign(
  val schemaVersion: Int = 1,
  val id: String,                 // UUID
  val name: String,
  val revision: Long,             // +1 on every saved change
  val updatedAtMillis: Long,
  val grainMode: GrainMode,       // EDGE_GRAIN, END_GRAIN
  val finished: Dimensions,       // L, W, T as Length
  val pattern: PatternSpec,
  val edges: EdgeTreatments,
  val embeddedCustomWoods: List<WoodSpecies> = emptyList(),
)
```

`PatternSpec` is a sealed interface:

- `Stripes(strips: List<Strip>)`, where `Strip(speciesId, width)`. Presets fill the list: uniform,
  alternating (2 to 3 species), symmetrical mirror, Fibonacci widths (1, 1, 2, 3, 5 units mirrored),
  and pinstripe accents. After applying a preset, the user edits strips freely (add, remove,
  reorder, change species or width).
- `Checkerboard(speciesA, speciesB, targetCell: Length)`.
- `Brick(brickSpecies, mortarSpecies, targetBrickWidth, targetBrickHeight, mortarWidth)`.

`EdgeTreatments`: `cornerRadius` (0 = square), `edgeProfile` (`NONE`, `CHAMFER`, `ROUNDOVER`, with
size), `juiceGroove` (none, or width, depth and inset from the edge), `fingerGrips` (none or both
ends), `feet` (none or four rubber feet). Phase 1 shows corner radius and the juice groove in the
2D preview and lists every treatment as build steps; the rest are drawn in phase 3.

Valid ranges (my call): L 6"–36", W 4"–24", T 3/4"–3". Grain-mode and pattern pairs: edge grain
allows only stripes. Checkerboard and brick in edge grain would need end-to-end glue joints, which
fail, so the UI doesn't offer them.

### Shop tool profile

```kotlin
@Serializable data class ShopProfile(
  val kerf: Length = 1/8",
  val planerMaxWidth: Length? = 13",          // null = no planer
  val drumSanderMaxWidth: Length? = null,
  val crosscutMaxWidth: Length = 12",         // sled or miter saw capacity
  val endGrainFlattening: FlatteningMethod = ROUTER_SLED, // DRUM_SANDER, ROUTER_SLED, BELT_AND_ORBITAL
  val panelFlattenAllowance: Length = 1/16",  // removed from a long-grain panel's height when planed
  val endGrainFlattenAllowance: Map<FlatteningMethod, Length> =
    {DRUM_SANDER: 1/16", ROUTER_SLED: 1/8", BELT_AND_ORBITAL: 1/16"},
  val edgeGrainFlattenAllowance: Length = 1/16",
  val endTrim: Length = 2",                    // total length added to strips for squaring and snipe
  val edgeTrimPerSide: Length = 1/8",          // squaring the glued board's sides
  val millingWaste: Double = 0.30,             // extra rough lumber for defects and milling
  val surfacedThickness: Map<Nominal, Length>, // 4/4 → 13/16", 5/4 → 1-1/16", 6/4 → 1-5/16", 8/4 → 1-3/4"
  val glue: String = "Titebond III",
)
```

### Build math

Write `e` = `edgeTrimPerSide`, `k` = `kerf`, `Δp` = `panelFlattenAllowance`, `Δf` = the final
flatten allowance (end grain: by the profile's flattening method; edge grain:
`edgeGrainFlattenAllowance`).

**Fitting to the target size.** Patterns use whole elements, so the compiler adjusts element sizes
to hit the target exactly where it can, and reports the actual finished size where it can't.

- **Widths the user enters are finished, visible widths.** Trim allowances are added only to the
  pieces that get trimmed, so a 1/4" pinstripe on the edge is still 1/4" in the finished board.
- Stripes: strip widths are scaled proportionally so they sum to `W`. Widths are rounded to 1/64",
  and the rounding remainder goes to the widest strip. The two outermost strips are then milled `e`
  wider, for trimming the sides.
- Checkerboard: squares stay square. `M` = `W / targetCell` rounded to the nearest **even** number
  of columns, because the flip that makes the checker only offsets colours when the strip count is
  even. Cell `s = W / M`, so width is exact. The two outer strips are milled `s + e` wide.
  `N = round(L / s)` rows, and the **actual length** `N·s` is shown beside the target when it
  differs.
- Brick: period across the width `P = b + m`. Rows along the length: `N_b` brick rows and
  `N_b − 1` mortar rows, with `N_b = round((L + m) / (h_target + m))` and brick height
  `h = (L − (N_b − 1)·m) / N_b`, so length is exact. Brick width `b` is the target as given;
  width is filled by trimming.
- **End grain is not trimmed along the length.** The two ends of an end-grain glue-up are the long-
  grain faces of the outer rows, already flat from flattening panel 1. Only the sides (where slice
  ends don't line up) are trimmed, by `e` each. Edge grain is trimmed on the ends (covered by
  `endTrim`) and the sides (`e`).

**Edge grain (stripes).**

- Each strip is milled to `width w_i × height (T + Δf) × length (L + endTrim)` (outer strips
  `w_i + e`). The strip's **width comes from the stock's thickness** and its height from the rip,
  so the top of the board is always edge grain. A strip wider than the thickest surfaced stock
  (1-3/4" for 8/4) is made by face-gluing two thinner pieces first; the cut list says so and the
  preview shows the glue line.
- Glue-up width `W_g = W + 2e`.
- If the long-grain flattening tool fits `W_g` (see "Flattening long-grain panels"), flatten the
  whole board to `T` in it. Otherwise split into sub-panels (see "Panel splitting"), flatten each
  to `T + 1/32"`, glue them together, and level the joint to `T` with a hand plane or sander.

**End grain (stripes, checkerboard, brick).** Panel 1 is a long-grain glue-up of strips. It is
crosscut into slices, and the slices are turned so their end grain faces up and glued together.

- Row height `r` (along X in the final board): the stripe row height the user sets (default 1-1/2";
  rows only affect construction, not the look of a stripes board), the cell `s` for checkerboard,
  or the brick height `h` for brick.
- Panel 1 strips are milled to height `H1 = r + Δp`, and panel 1 is **flattened down to `r`**.
- For panel 1 strips, either cross-section dimension can come from stock thickness, since end
  grain faces up either way (see "Cut list and lumber" for how it's chosen). It only changes where
  the growth rings sit, and the preview places the pith to match.
- Slice thickness: `t = T + Δf`.
- Number of slices: `N_s` = rows (stripes: `round(L / r)` with `r` adjusted to fit; checker
  `N`; brick `N_b`).
- Panel 1 length: `L1 = N_s·t + N_s·k + endTrim`. This deliberately counts one more kerf than
  strictly needed.
- Panel 1 width: stripes and checkerboard `W1 = W + 2e`. Brick: `n = ceil((W + 2e + P + m) / P)`
  bricks and `n − 1` mortar strips, so `W1 = n·P − m`. That is at least `W + 2e + P`, so a row
  shifted by `P/2` still covers the width after trimming.
- Brick panel 1 alternates brick-species strips `b` wide with mortar-species strips `m` wide (the
  vertical joints), starting and ending with a brick strip. The pattern is centred across the
  width, and the excess is trimmed equally from both sides. So the mortar species is in both panel
  1 and the mortar panel, and both count towards its board feet.
- Slices are arranged as follows:
  - Stripes: all the same way round.
  - Checkerboard: every other row rotated 180° about Z, which offsets the colours by one cell.
  - Brick: every other brick row shifted by `P/2` across the width (rows 0, 2, 4… centred, rows
    1, 3, 5… shifted), with a mortar slice between brick rows.
  - A row's flip or shift always applies to the **whole row**. When panel 1 is split, a rotated row
    takes its sub-slices in reverse order, each rotated, and a shifted row moves all its sub-slices
    together. This keeps the wider outer strips at the outside, and lets a split fall on any strip
    boundary.
- Brick mortar rows come from a **mortar panel**: mortar species, width `W1`, height `m + Δp`,
  length `(N_b − 1)·(t + k) + endTrim`, crosscut to slices of thickness `t`. Mortar rows are then end
  grain like everything else, so the whole board moves together with the seasons. A mortar panel
  wider than its limit is made as `ceil(W1 / limit)` equal-width sub-panels, each a single board
  (or a glue-up of mortar-species strips, which the build guide mentions).
- Final glue-up size before trimming: `L × W1`. It is flattened with the profile's end-grain
  method, then its sides are trimmed to width `W`.
- **End grain never goes through a thickness planer.** The flatten stage always says so in a
  warning callout, whatever the profile says.

**Flattening long-grain panels.** The tool is the planer if one is set, else the drum sander if one
is set, else the profile's end-grain flattening method (router sled or belt and orbital sander, with
its allowance in place of `Δp`). A planer or drum sander has a width limit; a router sled or
sanders don't. If the planer is too narrow but the drum sander fits, the drum sander is used
instead of splitting.

**Panel splitting.** Any long-grain panel (edge-grain glue-up, end-grain panel 1, mortar panel)
wider than its limit is split into sub-panels at strip boundaries. The limit is the flattening
tool's width for edge grain, and the smaller of that and `crosscutMaxWidth` for end-grain panels.
The split uses the fewest sub-panels, and among those the boundaries that minimise the widest
sub-panel. Remaining ties go to the split whose narrowest sub-panel is widest, then to the one
with the leftmost boundaries.

Sub-panels are named `1A`, `1B`, ... For end grain, each row of the final glue-up uses a slice from
every sub-panel, side by side in order. If no split satisfies the limit (for example, a single strip
or period is wider than it), that's an error shown to the user.

**Cut list and lumber.**

- **Cut list:** one row per distinct milled part: species, count, and milled width × height ×
  length.
- **Stock thickness:** for edge-grain strips the width comes from stock thickness (see above). For
  every other part the compiler tries both cross-section dimensions as the one from stock
  thickness, takes the thinnest nominal whose surfaced thickness is at least that dimension, and
  keeps the orientation with fewer board feet (ties: the thinner stock). So thin strips are ripped
  from thick stock instead of planing thick stock down. If neither dimension fits (both over
  1-3/4"), it says to laminate or buy thicker stock.
- **Board feet** per species: `Σ nominal thickness × (rip width + k) × length / 144 × (1 +
  millingWaste)`. Shown to 0.1 BF, plus a total.

### Build guide stages

`FabricationPlan` is a list of `Stage(number, title, steps: List<String>, callouts, diagram)`.
Stages are generated, never written by hand per design.

1. **Buy and mill stock.** The shopping list and cut list.
2. **Prepare.** For oily species: wipe the gluing faces with acetone and let them flash off right
   before glue-up. For bleeding species: glue up and wipe the squeeze-out promptly. Glue type from
   the profile.
3. **Glue panel 1** (or the edge-grain board), and the mortar panel for brick. Diagram: the strips'
   cross-section in order, colour-coded, sub-panels marked.
4. **Flatten panel 1.** With the long-grain flattening tool, down to row height `r`. Edge grain:
   flatten the board to `T` (levelling any sub-panel joint), then skip to stage 8.
5. **Crosscut slices.** Count and thickness `t`; set a stop block; square one end first. Diagram:
   side view of the panel with slice lines.
6. **Arrange and glue slices.** Flip or shift rule, mortar rows for brick. Diagram: top view of the
   arrangement, with flipped slices marked.
7. **Flatten the board.** Method from the profile, final thickness `T`. End grain: the
   never-use-a-planer warning.
8. **Trim to size.** Edge grain: ends and sides to `L × W`. End grain: sides only, to `W`.
9. **Edge details** in a safe order: juice groove, corner radius, edge profile, finger grips, then
   feet after finishing.
10. **Finish.** Mineral oil, then a mineral oil and beeswax board butter.

Diagrams are produced by `StageDiagramSvgBuilder` in core as SVG strings: plain shapes, species
fill colours, labels and dimensions. The same diagram model is drawn in the app with Compose
`Canvas`, so the app doesn't render SVG. Its visual style is left to the builder.

### Warnings (`WoodAuditor`)

Each `Issue` has `severity` (`INFO`, `WARNING`, `AVOID`), `title`, `detail`, and the related species
or strips. None blocks anything.

- A species rated `CAUTION` (Warning) or `NOT_SUITABLE` (Avoid) for this grain mode, with its
  reasons.
- Janka spread: `max − min > 1100 lbf` among the board's species (Warning: softer wood sands
  away faster, leaving dips).
- Oily species present (Info: the acetone step was added).
- A colour-bleeding species next to a lighter one (Info).
- A strip or mortar line under 1/8" after fitting (Warning).
- Thickness: end grain under 1-1/4", or edge grain under 3/4" (Warning: prone to cracking or
  warping). These two thresholds are my call.
- The compiler also checks that every piece's fibre direction matches the board's grain mode.
  Generators guarantee this, so it is enforced by tests and an internal assertion, not shown to the
  user.

### Compiling a design

`DesignCompiler.compile(design, catalog, profile): CompiledDesign`, a pure function:

- `layout`: the finished board's pieces, each a polygon in board coordinates (rectangles in phase 1,
  but the type is a polygon so phase 2 needs no model change), with `speciesId`, `fibre`,
  `sourceStripIndex`, `sliceIndex`, `flipped` and `shiftY`. These let the preview show that cells
  from the same strip share rings.
- `actualFinished`, plus any fit notes ("length is 15-7/8", 1/8" under target, to keep squares
  square").
- `plan` (stages), `cutList`, `lumber`, `issues`.
- Or errors (e.g. a strip wider than the planer limit, an invalid range).

### Grain textures

`GrainTexture` in core generates ARGB pixels (`IntArray`) for a piece, so the algorithm is shared
and testable and the app only wraps it in an `ImageBitmap`:

- **End grain (fibre Z):** concentric growth rings around a pith centre that sits outside the piece
  (as it does in real strip stock), with noise-warped ring spacing, ring colour from the species'
  latewood tone, and pore dots for ring-porous and semi-ring species. The pith position and seed
  come from `sourceStripIndex`, so every cell cut from the same strip shows the same ring pattern,
  mirrored when `flipped`.
- **Long grain (fibre X):** straight, slightly wavy lines along X, as an edge-grain face shows.
- Finish: Raw or Oiled tones from the catalog. Aged comes in phase 3.
- Cached by species, fibre, seed, finish and pixel size.

The exact noise and colour functions are left to the builder. The acceptance bar is that end grain
and long grain are unmistakable at a glance, species are recognisable by colour, and the same-strip
and flip relationships are visible.

## Local storage

- `WorkspaceStore` interface; desktop implementation under `$XDG_DATA_HOME/graincraft-studio/`
  (default `~/.local/share/graincraft-studio/`):
  - `designs/<id>.graincraft.json`
  - `shop-profile.json`, `custom-woods.json`, `settings.json`
- Writes are atomic (temp file, then rename). Saving bumps `revision` and `updatedAtMillis`, which
  phase 6 sync will build on.
- **Autosave** 1 s after the last edit. There is no save button. The top bar shows "Saved" or
  "Saving…".
- `schemaVersion` on every file. Unknown future versions are refused with a clear message, not
  overwritten.
- A design file that fails to parse is listed in the library as "Can't open" with the reason, and
  left untouched.

## The app

### Conventions (from straphanger)

- **Material 3 Compose components throughout:** `Scaffold`, `TopAppBar`, `NavigationRail`,
  `PrimaryTabRow`, `OutlinedTextField`, `ExposedDropdownMenuBox`, `FilterChip`,
  `SingleChoiceSegmentedButtonRow`, `Card`, `ListItem`, `AlertDialog`, `ModalBottomSheet` /
  side sheets, `SnackbarHost`, `PlainTooltip`, `DropdownMenu`. Hand-drawn UI only where no
  component exists: the preview canvas and the stage diagrams.
- **A full app theme:** `GrainCraftTheme` with light and dark colour schemes (every role set),
  a type scale (body text never under 13 sp), shapes (4 / 8 / 12 / 16 / 28 dp), a `Spacing` object
  (4 / 8 / 12 / 16 / 24 dp), and extra semantic colours for the ratings (recommended, caution,
  not suitable) provided through a `CompositionLocal`. The palette is warm and wood-toned; the exact
  values are left to the builder. The theme follows the system light or dark setting, with an
  override in Settings (my call; straphanger is dark only).
- **Tests like straphanger's `ThemeTest`:** contrast of at least 4.5:1 for text roles and rating
  colours in both schemes, minimum text sizes, and a scan that fails if any screen file contains
  `Color(0x`, named colours or `fontSize =`. The theme, the preview canvas and the diagram renderer
  are exempt, because species colours come from data.
- **Icons** are Material Symbols saved as vector drawables in `composeResources/drawable`.
- Every interactive element has a test tag. Screens get `docs/CODE-TOUR.md` entries.
- View models use `androidx.lifecycle` multiplatform `ViewModel` and expose `StateFlow` UI state.
  Compiling runs on `Dispatchers.Default`, 150 ms after the last edit.

### Window and navigation

- One window, 1280 × 800 by default, minimum 1024 × 700. Size and position are remembered in
  `settings.json`.
- A `NavigationRail` on the left with **Designs**, **Woods**, **Shop** and **Settings**. Opening a
  design shows the design editor in place of the Designs list, with a back arrow in the top bar.
- Keyboard shortcuts:
  - Ctrl+N: new design
  - Ctrl+Z, and Ctrl+Shift+Z or Ctrl+Y: undo and redo in the editor
  - Ctrl+P: export the shop packet
  - Ctrl+F: search woods

### Designs (library)

- A grid of cards: thumbnail (the 2D preview, small), name, size, pattern and grain, last edited.
- A "New design" button opens a dialog: name, grain mode, pattern, size presets (e.g. 18 × 12 ×
  1-1/2) or custom.
- Card menu: Open, Rename, Duplicate, Export file…, Delete. Delete moves the file to a
  `trash/` folder and shows a snackbar with Undo. The trash is cleared after 30 days.
- "Import file…" opens a `.graincraft.json` file. An imported design whose `id` already exists gets
  a new id and " (imported)" added to its name.

```
┌ Rail ┬───────────────────────────────────────────────────────────────┐
│ ▣    │ Designs                                   [Import] [+ New]   │
│ Woods│ ┌────────┐ ┌────────┐ ┌────────┐                              │
│ Shop │ │▒▓▒▓▒▓▒ │ │▓▓▓▓▓▓▓ │ │▒▒▓▒▒▓▒ │                              │
│ Set. │ │Checker │ │Walnut… │ │Brick 1 │                              │
│      │ │18×12   │ │16×10   │ │20×14   │                              │
└──────┴───────────────────────────────────────────────────────────────┘
```

### Design editor

The top bar has the name (click to rename), a save status, undo and redo, a **Raw / Oiled**
segmented button, and **Export packet**.

Three panes:

- **Left (about 360 dp, scrolls): Design.**
  - Grain mode as a segmented button. Switching to edge grain when the pattern isn't stripes asks
    first.
  - Finished length, width and thickness fields that accept either unit, with errors shown as
    supporting text.
  - Pattern as filter chips.
  - The pattern's parameters:
    - Stripes: a strip list, each row with a species dropdown with swatch, width field, move up or
      down, and remove; "Add strip"; a "Presets" menu; for end grain, the row height.
    - Checkerboard: two species and the cell size.
    - Brick: brick and mortar species, brick width and height, mortar width.
  - Edge details: corner radius, profile, juice groove, finger grips, feet.
- **Centre: Preview.**
  - The 2D board on a neutral surface, with zoom (scroll wheel and buttons) and pan (drag).
  - A dimensions overlay you can toggle. Target and actual size are shown when they differ.
  - Hovering a piece shows a tooltip with species, strip number and slice.
- **Right (about 380 dp): tabs.**
  - **Issues:** the warnings, with severity icons and rating colours.
  - **Build:** the stages as cards with diagrams.
  - **Lumber:** the shopping list per species and the cut list table.

Below 1200 dp of window width, the right pane collapses into a fourth tab next to the preview
("Preview | Issues | Build | Lumber").

```
┌──────────────────────────────────────────────────────────────────────────────┐
│ ← Walnut & Maple Checker   Saved   ↶ ↷        [Raw|Oiled]   [Export packet] │
├──────────────────┬──────────────────────────────────────┬────────────────────┤
│ Grain [Edge|End] │                                      │ Issues Build Lumber│
│ L [ 18"    ]     │      ┌──────────────────────┐        │ ⚠ Janka spread     │
│ W [ 12"    ]     │      │▓▒▓▒▓▒▓▒▓▒▓▒▓▒▓▒▓▒▓▒▓▒│        │   1450 vs 1010 ok  │
│ T [ 1-1/2" ]     │      │▒▓▒▓▒▓▒▓▒▓▒▓▒▓▒▓▒▓▒▓▒▓│        │ ℹ Actual length    │
│ (Stripes)(Check… │      │▓▒▓▒▓▒▓▒▓▒▓▒▓▒▓▒▓▒▓▒▓▒│        │   17-7/8"          │
│ A [Walnut  ▾]    │      └──────────────────────┘        │                    │
│ B [Maple   ▾]    │        18" × 12" × 1-1/2"            │                    │
│ Cell [ 1-1/2" ]  │                          [－][＋][⤢] │                    │
└──────────────────┴──────────────────────────────────────┴────────────────────┘
```

### Woods

- A searchable list. Each row has raw and oiled swatches, the name, Janka, pore type, two rating
  chips (Edge, End) in rating colours, and icons for oily, bleeds and irritant. Expanding a row
  shows the reasons and notes.
- Filter chips: Recommended only, Hide not suitable, Custom.
- "Add custom wood" opens a dialog (fields listed under Custom species) showing the computed
  rating, with an override switch and reason. Custom woods can be edited or deleted. Deleting one
  that a design uses is allowed, because designs embed their copy.

### Shop

A form with every `ShopProfile` field, grouped into cards: Saws, Planer and sander, Flattening,
Allowances, Lumber. Each field has a tooltip or help text explaining what it affects. "Reset to
defaults" asks first.

### Settings

Units (Imperial / Metric), fraction precision (1/16 or 1/32), theme (System / Light / Dark), the
data folder location (shown, with an "Open folder" button), and About (version, licences).

### Shop packet

`ShopPacketExporter` in core builds **one self-contained HTML file**: inline CSS with print
styles, and inline SVG for the board drawing and stage diagrams. It contains:

- Cover: name, the board drawing, finished size, grain mode, species.
- Issues.
- Shopping list and cut list.
- Stages with diagrams.
- Profile settings used.

The board drawing in the packet is a flat SVG top view in species colours (no texture). "Export
packet" saves the file through a save dialog (default name `<design name> - shop packet.html`) and
offers "Open in browser", from which the user prints or saves as PDF.

## Testing

- **Core (`commonTest`, runs on JVM and Wasm/Node):**
  - `LengthTest`: parsing every accepted form; exact 1/64" arithmetic; formatting and rounding at
    1/16 and 1/32; metric.
  - `BoardFeetTest`.
  - `WoodCatalogTest`: every built-in has complete data; ratings match the table; custom-wood
    rating rules.
  - `PatternFitTest`: stripes scaling and remainder; checkerboard keeps squares and reports actual
    length; brick row fitting.
  - `FabricationMathTest`: worked examples, checked by hand, for each pattern × grain mode, covering
    `H1`, `t`, `N_s`, `L1`, `W1`, the mortar panel, and BF. Also panel splitting: none, two
    balanced, three, and a too-wide single strip.
  - `LayoutTest`: pieces tile the board with no gaps or overlaps; checkerboard flips; brick shift;
    `sourceStripIndex` and `flipped` are correct; fibre matches grain mode.
  - `AuditorTest`: each rule.
  - `StagePlanTest`: stage list per mode; the planer warning is always present for end grain;
    acetone step only with oily species.
  - `SerializationTest`: round trip; unknown future schema refused; embedded custom woods.
  - `GrainTextureTest`: deterministic per seed; a flipped cell is the mirror image; end-grain and
    long-grain textures differ statistically (ring structure versus line structure).
  - `ShopPacketExporterTest`: contains every section; well-formed; no external URLs.
- **App (`desktopTest`, Compose UI tests):**
  - `ThemeTest` (contrast, sizes, no literals).
  - Library: new, rename, duplicate, delete and undo, import collision.
  - Editor: changing size, pattern and strips updates the preview, issues and lumber; undo and
    redo; the grain-mode switch confirmation.
  - Woods: add a custom wood and see its computed rating.
  - Shop: a profile change changes the cut list.
  - A store test against a temporary directory: atomic write, autosave debounce, unparseable file
    handling.
- **Screenshot tests (Roborazzi, Compose Desktop):** golden images committed under
  `app/screenshots/`, verified by `verifyRoborazziDesktop` in CI and re-recorded with
  `recordRoborazziDesktop` only when a visual change is intended (say so in the PR, as in zmanim).
  Goldens, each in light and dark theme at the default window size:
  - The library with a few designs, and empty.
  - The editor for each pattern and grain combination (edge-grain stripes, end-grain stripes,
    checkerboard, brick), in Raw and in Oiled, which also pins the grain textures: end-grain rings
    versus long-grain lines, same-strip cells and flipped slices.
  - The editor with the right pane collapsed (below 1200 dp).
  - The Issues, Build and Lumber tabs for one design.
  - Woods (with a row expanded), the custom wood dialog, Shop and Settings.
  - Each stage diagram, drawn on its own.
- So that goldens match on any Linux machine and on CI, the theme uses a font bundled in
  `composeResources/font` (an open-licence sans such as Inter or Noto Sans, the builder's pick)
  rather than whatever system font is installed, and the screenshot tests set a small pixel-change
  threshold for anti-aliasing differences.
- Kover gate: 85% line coverage on `core` (my call); `app` is reported but not gated.

## Done for phase 1

- `scripts/ci.sh` passes.
- `scripts/run-desktop.sh` launches the app, and the `.deb` installs and runs on the owner's
  Ubuntu machine.
- The owner can design each of the four pattern and grain combinations, see an accurate preview,
  read a build guide whose numbers match the hand-checked tests, and print the shop packet from the
  browser.
- `README.md`, `CHANGELOG.md` and `docs/CODE-TOUR.md` describe the app.

## Calls I made (for the owner to check)

- The wood table: species added as "Not suitable" examples (pine, cedar, spalted maple, cocobolo),
  and every rating.
- Default shop allowances, including `endTrim` 2" and `edgeTrimPerSide` 1/8".
- Edge grain offers stripes only.
- Strip widths you enter are finished widths; trim allowances go only on the outer pieces.
- Edge-grain strips always take their width from the stock's thickness, so the top is always edge
  grain. Strips wider than 1-3/4" are face-glued from two pieces, which leaves a visible glue line.
- End-grain boards are trimmed only on the sides, never on the ends.
- With no planer or drum sander, long-grain panels are flattened with the end-grain method.
- A row's flip or shift applies to the whole row, so panels can be split at any strip boundary.
- Brick rows alternate centred and shifted by half a brick; the mortar species also forms the
  vertical joints in panel 1.
- Checkerboard keeps squares square and lets the length come out slightly off target; brick keeps
  length exact and trims width.
- Thickness warnings at 1-1/4" (end grain) and 3/4" (edge grain).
- Light and dark themes following the system, with a wood-toned palette.
- Autosave with no save button; delete goes to a 30-day trash.
- Size limits 6–36" × 4–24" × 3/4–3".

## Left to the builder

- Exact colour values for the theme and species tones.
- The grain texture algorithm, within the acceptance bar above.
- Diagram and packet visual style.
- Pane proportions and spacing details within the theme's tokens.
- Internal package structure inside `core` and `app`.
- Version control setup: jj on git, as in straphanger.
