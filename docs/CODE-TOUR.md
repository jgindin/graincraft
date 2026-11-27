# Code tour

A walk through the GrainCraft Studio code. Paths are relative to the repository root.

## 1. The shape of the project

```
graincraft/
├── core/              Kotlin Multiplatform library (JVM + Wasm): the domain, no platform code
├── app/               Compose Multiplatform desktop app: UI in commonMain, entry point in desktopMain
├── config/detekt/     Static analysis settings (overrides only)
├── scripts/           ci.sh (everything CI runs), run-desktop.sh
├── docs/              Roadmap, per-phase design and plan, this tour
└── version.properties The one place the version lives
```

- [settings.gradle.kts](../settings.gradle.kts) includes `:core` and `:app`. Library and plugin
  versions are all in [gradle/libs.versions.toml](../gradle/libs.versions.toml).
- [core/.../units/Length.kt](../core/src/commonMain/kotlin/com/jaygindin/graincraft/core/units/Length.kt)
  is the one distance type: whole nanometres in a `Long`, so 1/64" is exact and sums never drift.
  Domain code never holds inches as a `Double`.

- [app/.../Main.kt](../app/src/desktopMain/kotlin/com/jaygindin/graincraft/app/Main.kt) opens the
  window and [App.kt](../app/src/commonMain/kotlin/com/jaygindin/graincraft/app/App.kt) is the root
  composable (section 13).

## 2. Lengths and units

All in [core/.../units/](../core/src/commonMain/kotlin/com/jaygindin/graincraft/core/units/).

- `LengthParser.kt` turns typed text (`1-3/4`, `1 3/4"`, `¾`, `44,5 mm`) into a `Length` or an
  error with a reason the UI can show. Fractions are always inches; bare decimals use the
  user's default unit.
- `UnitFormatter.kt` is the only way a `Length` becomes text: inches at 1/16 or 1/32, or
  millimetres with one decimal.
- `Nominal.kt` has the nominal thickness grades (4/4 to 8/4) and `boardFeet`, which prices on the
  nominal thickness.

## 3. The wood catalog

All in [core/.../wood/](../core/src/commonMain/kotlin/com/jaygindin/graincraft/core/wood/).

- `WoodSpecies.kt` has the species type and its parts: `Rgb` (with WCAG luminance), `WoodTones`,
  `PoreType`, and `Rating`, which is ordered best to worst so the worst of several is the maximum.
- `WoodCatalog.kt` holds the 26 built-in species, copied from the design table, and `Woods`, which
  merges them with the user's custom species (a custom id never replaces a built-in);
  `withExtra` adds a design's embedded customs whose ids are not known yet.
- `CustomWoodRating.kt` rates a custom species by rule (soft, open pores, irritant, oily) and
  derives latewood and pore tones by darkening a base colour.

## 4. Designs and the shop profile

In [core/.../design/](../core/src/commonMain/kotlin/com/jaygindin/graincraft/core/design/) and
[core/.../shop/](../core/src/commonMain/kotlin/com/jaygindin/graincraft/core/shop/).

- `BoardDesign.kt` is a saved design: grain mode, finished size, a pattern and edge treatments.
- `PatternSpec.kt` is the sealed pattern type (`Stripes`, `Checkerboard`, `Brick`), written to JSON
  with a `"type"` field.
- `EdgeTreatments.kt` holds the edge details and the end-grain `FlatteningMethod`.
- `StripePresets.kt` fills a strip list (uniform, alternating, mirror, Fibonacci, pinstripe).
- `DesignJson.kt` encodes and decodes design files. It reads `schemaVersion` before anything else,
  so a file from a newer app is reported as such even if its shape has changed. `WorkspaceJson`
  holds the JSON settings every file shares.
- `ShopProfile.kt` has the tools, allowances and stock thicknesses the build math reads.

## 5. Fitting to the target size

In [core/.../fabrication/](../core/src/commonMain/kotlin/com/jaygindin/graincraft/core/fabrication/).

- `PatternFit.kt` turns a pattern and a target size into finished column widths, rows and the
  actual size reached. Widths are finished widths; trim allowances are added later.
- `CompileError.kt` lists the reasons a design cannot be compiled, shared by the later build steps.
- `Flattening.kt` picks the tool that flattens a long-grain panel (planer, drum sander, or the
  profile's end-grain method) with its allowance and width limit, and `splitLimit` gives the width
  a sub-panel may reach (end-grain panels are also capped by the crosscut).
- `PanelSplitter.kt` splits strips into contiguous sub-panels under that limit, splits mortar
  panels into equal parts, and names sub-panels `1A`, `1B`, ...
- `Fabrication.kt` holds the build: panels (`Panel`, `PanelStrip`), the rows of the final glue-up
  (`Row`), slice thickness and counts, and the glue-up size and final flatten.
- `FabricationCompiler.kt` builds it from a `Fit`. It adds the side-trim allowance to the outer
  strips (or lays out the centred brick sequence), mills strips to the row height plus the panel
  tool's allowance, splits panels too wide for their tool, and lays out rows. A rotated or shifted
  row applies to every sub-panel slice in it, so a rotated row lists its sub-panels in reverse.
- `CutList.kt` turns a `Fabrication` into the parts to mill and the lumber to buy. Each strip of
  each panel is a part; identical parts merge. Edge-grain strips take their width from stock
  thickness (laminated from two halves when wider than the thickest stock); other parts take
  whichever orientation needs fewer board feet. `lumber` sums board feet per species with kerf and
  milling waste, and `formatBoardFeet` shows them to 0.1 BF.

## 6. Layout of the finished board

In [core/.../layout/](../core/src/commonMain/kotlin/com/jaygindin/graincraft/core/layout/).

- `Layout.kt` holds the finished board's face: `BoardLayout` and its `Piece`s, each a rectangle in
  board coordinates (`x` along the length, `y` across the width) with species, `Fibre`, the
  panel-1 strip it came from, its slice row, and the row's rotation and shift.
- `LayoutBuilder.kt` builds it from a `Fabrication`. Each row's strips are laid across the glue-up
  width centred on the board (a rotated row's strips reversed, a shifted row moved by its shift),
  then clipped to `L × W`. An edge-grain strip thicker than the profile's thickest stock is split
  into two laminated pieces.

## 7. Woodworking warnings

In [core/.../audit/](../core/src/commonMain/kotlin/com/jaygindin/graincraft/core/audit/).

- `WoodAuditor.kt` holds `audit(mode, finished, fit, woods, formatter)`, which returns `Issue`s
  sorted AVOID, WARNING, INFO. It reads the species from the fit's columns and checks species
  suitability for the grain mode, Janka spread, colour bleed, thin strips and mortar lines,
  end-grain thickness and oily woods. Unknown species ids are skipped.

## 8. Build guide and compiler

In [core/.../guide/](../core/src/commonMain/kotlin/com/jaygindin/graincraft/core/guide/) and
[core/.../compile/](../core/src/commonMain/kotlin/com/jaygindin/graincraft/core/compile/).

- `Diagram.kt` holds the guide's model: `Stage` (number, kind, title, steps, callouts, diagram),
  `Callout` (a safety `WARNING` or a `TIP`), and `Diagram`, plain shapes in inches.
- `StagePlanner.kt` has `planStages`, which writes the stages a board needs and numbers them:
  edge grain skips slicing and the final flatten, and edge details appear only when chosen.
  `GuideContext` carries the inputs and the shared wording (species names, panel names, tools);
  it also writes the buying, preparing, gluing and panel-flattening stages.
- `BoardStages.kt` writes the rest: crosscut, arrange and glue, the final flatten (always with
  the never-a-planer warning), trim, edge details (groove, corner radius, profile, grips) and
  finish (feet last).
- `StageDiagrams.kt` draws the glue-up cross-sections, the crosscut side view with one dashed line
  per slice, and the arrangement from above (laid out with `buildLayout` at the glue-up size, so
  shifted rows show ragged and rotated rows are hatched).
- `StageDiagramSvgBuilder.kt` turns a `Diagram` into a self-contained SVG string. Numbers are
  written by hand so the JVM and Wasm give identical text.
- `DesignCompiler.kt` runs the whole pipeline: it adds the design's embedded custom woods, checks
  every species id, fits, builds the fabrication, cut list, lumber, layout (at the fitted size),
  warnings and stages, or returns `CompileResult.Failed`. `CompileError.message` gives each error
  as one sentence in the user's units.

## 9. Grain textures

In [core/.../texture/](../core/src/commonMain/kotlin/com/jaygindin/graincraft/core/texture/).

- `GrainTexture.kt` holds `TextureSpec`, `GrainTexture.render(spec, width, height)` (ARGB,
  row-major) and `textureSeed(speciesId, sourceStripIndex, laminationIndex)`. End grain draws
  warped rings around a pith 2 to 6 inches off the piece, with pores for open and semi-ring woods;
  long grain draws wavy lines along X. Everything is hand-written hash noise, so JVM and Wasm
  agree. `mirrored` flips x.

## 10. Shop packet

In [core/.../packet/](../core/src/commonMain/kotlin/com/jaygindin/graincraft/core/packet/).

- `ShopPacketExporter.kt`: `html(compiled, woods, profile, formatter)` returns one self-contained
  page (inline CSS with `@media print`, inline SVG): cover with a flat top-view board drawing in raw
  species colours, issues, shopping list, cut list, each stage with its diagram, and the shop
  settings. `defaultFileName(name)` gives `"<name> - shop packet.html"`. All text is escaped.

## 11. App theme

In [app/.../ui/theme/](../app/src/commonMain/kotlin/com/jaygindin/graincraft/app/ui/theme/).

- `GrainCraftTheme(dark, content)` applies the light or dark colour scheme (warm walnut and maple
  tones, every role set), `GrainCraftShapes` (4 / 8 / 12 / 16 / 28 dp), the type scale from
  `Type.kt` and `LocalRatingColors` (recommended, caution, not suitable, each with an on-colour).
  `ThemeChoice` is the System / Light / Dark setting. `Spacing` holds the 4 / 8 / 12 / 16 / 24 dp
  steps; `primaryButtonColors()` and `quietButtonColors()` style the two button weights.
- Fonts (Noto Sans Regular and Bold, with `OFL.txt`) are in
  [composeResources/font/](../app/src/commonMain/composeResources/font/); icons are Material Symbols
  vector drawables in [composeResources/drawable/](../app/src/commonMain/composeResources/drawable/)
  (`Res.drawable.ic_*`).
- `ThemeTest` fails on low contrast, small text, a missing bundled font, an icon that does not
  decode, or a `Color(0x`, named colour or `fontSize =` in any screen file outside `ui/theme/`,
  `ui/preview/` and `ui/diagram/`.

## 12. Workspace storage

In [app/.../data/](../app/src/commonMain/kotlin/com/jaygindin/graincraft/app/data/) (interface,
`AppSettings`) and the same folder under `desktopMain` (`FileWorkspaceStore`, `XdgPaths.kt`).

- `WorkspaceStore` is everything the app keeps on disk: designs, a 30-day trash, shop profile,
  custom woods and settings. `FileWorkspaceStore` keeps `designs/<id>.graincraft.json`,
  `trash/<id>.graincraft.json` (trash time is the file's modified time), `shop-profile.json`,
  `custom-woods.json` and `settings.json` under `xdgDataRoot(...)`.
- Every write goes to `<name>.tmp` and is renamed over the target. A design file that does not
  parse, or was made by a newer version, is listed as `Unreadable` and never overwritten; trash
  and restore refuse to replace an existing file. Profile, woods and settings that fail to parse
  load defaults and stay on disk until the next explicit save.

## 13. App shell and Settings

In [app/.../app/](../app/src/commonMain/kotlin/com/jaygindin/graincraft/app/) and
[ui/settings/](../app/src/commonMain/kotlin/com/jaygindin/graincraft/app/ui/settings/).

- `platform/Platform.kt` holds the platform interfaces: `FileDialogs` (open a design file's text,
  save text to a chosen path), `ExternalBrowser` (open a file or folder with the desktop's apps)
  and `Clock`. `desktopMain/.../platform/DesktopPlatform.kt` implements them with FileKit and
  `xdg-open`.
- `AppGraph` carries the store, dialogs, browser, clock, id source and the `ArgbImageFactory` for
  grain textures; `Main.kt` builds the real one, tests build one from `testutil/FakePlatform.kt`
  and `FakeWorkspaceStore.kt`.
- `App(graph)` creates the `SettingsViewModel`, picks light or dark from the theme setting,
  provides `LocalUnitFormatter` (in `ui/LocalUnitFormatter.kt`) and shows `AppScaffold`: the
  `NavigationRail` with the four `Destination`s (`Destination.kt`, with their icons and test-tag
  names) and the current screen beside it. Each screen brings its own top bar. Designs, Woods and
  Shop are `PlaceholderScreen`s until their screens land.
- `SettingsViewModel` loads `AppSettings` once and saves every change in order. `SettingsScreen`
  shows units, precision (disabled for metric), theme, the data folder with "Open folder", and
  About. `AppInfo.VERSION` is generated from `version.properties` by the `generateAppInfo` Gradle
  task.
- `Main.kt` restores the window's position and size from `AppSettings.window` (ignored when it is
  on no screen, see `restorablePlacement`) and saves them on close.
- Screenshot goldens live in [app/screenshots/](../app/screenshots/); `ShellScreenshotTest` renders
  `AppShell` at 1280 × 800 with a fixed version so goldens do not change with releases.

## 14. Designs library

In [ui/library/](../app/src/commonMain/kotlin/com/jaygindin/graincraft/app/ui/library/) and
[ui/preview/](../app/src/commonMain/kotlin/com/jaygindin/graincraft/app/ui/preview/).

- `LibraryViewModel` lists the store's designs, compiles each with `DesignCompiler` for its
  thumbnail and summary, and offers `create`, `rename`, `duplicate`, `delete` / `undoDelete`,
  `importFile` (a clashing id gets a new id and " (imported)") and `exportFile`. Actions run one at
  a time and a store refusal becomes `LibraryState.message`.
- `NewDesignDefaults.kt` has `PatternKind` and `defaultPattern(kind)`, the starting pattern of a new
  design (the editor reuses it).
- `LibraryScreen` is the card grid (`LazyVerticalGrid`) with the card menu, rename dialog, the empty
  state and the Undo snackbar; `NewDesignDialog` asks for name, grain, pattern and size (edge grain
  disables checkerboard and brick). Unreadable files show as "Can't open" cards.
- `ui/preview/FlatBoardDrawing.kt` draws a compiled layout from above in each species' raw colour;
  `DesignThumbnail` uses it, and the editor's preview will live beside it.
- `App` keeps the open design's id and shows `EditorStub` until the editor lands, and purges the
  trash once at start.

## 15. Woods and Shop

In [ui/woods/](../app/src/commonMain/kotlin/com/jaygindin/graincraft/app/ui/woods/),
[ui/shop/](../app/src/commonMain/kotlin/com/jaygindin/graincraft/app/ui/shop/) and
[ui/components/](../app/src/commonMain/kotlin/com/jaygindin/graincraft/app/ui/components/).

- `AppShell` owns one `WoodsViewModel` and one `ShopViewModel`; their `woods` and `profile` flows
  feed the library (the editor will use the same flows), and the library refreshes when either
  changes so thumbnails follow.
- `WoodsViewModel` loads the custom woods, keeps the search text and `WoodFilter` set, exposes the
  narrowed `visible` list, and saves on `saveCustom` / `deleteCustom`.
- `WoodsScreen` is a `LazyColumn` of expandable rows (swatches from `ui/preview/ColorSwatch`,
  rating chips in `LocalRatingColors`, flag icons with tooltips). `CustomWoodDialog` adds or edits a
  custom wood: hex fields with a live swatch, the rating computed by `rateCustomWood`, and an
  override switch that needs a reason.
- `ShopViewModel` loads and saves the `ShopProfile` on every change; `ShopScreen` is a form of cards
  with help text under each field and an "I have one" switch for the optional machines. "Reset to
  defaults" asks first.
- `LengthField` keeps what the user types while editing, shows `parseLength`'s reason as an error,
  and commits on valid input; otherwise it shows the length with `LocalUnitFormatter`.

## 16. Editor view model

In [ui/editor/](../app/src/commonMain/kotlin/com/jaygindin/graincraft/app/ui/editor/).

- `EditorViewModel` holds the open design and exposes `EditorState`: the design, the latest
  `CompileResult`, the woods the compiler sees (the user's plus the design's embedded customs),
  `SaveStatus`, the `Finish` (raw or oiled), undo and redo availability, and whether a grain switch
  is waiting for confirmation.
- `edit` is one undo step (history of 100); undo and redo carry the current revision forward so a
  save never counts back. Compiling runs on `Dispatchers.Default` 150 ms after the last change, at
  once on open, and again when the shared `woods`, `profile` or formatter flows change.
- Autosave writes 1 s after the last change, bumping `revision` and `updatedAtMillis` and setting
  `embeddedCustomWoods` to the custom woods the pattern uses. `flush()` writes a pending change at
  once; the editor awaits it before closing. A failed write sets `SaveStatus.ERROR` and keeps the
  change pending for the next edit or flush.
- The debounces run in an injectable scope and compile dispatcher, so `EditorViewModelTest` drives
  them with virtual time.

## 17. Board preview

In [ui/preview/](../app/src/commonMain/kotlin/com/jaygindin/graincraft/app/ui/preview/).

- `BoardPreview` draws a `BoardLayout` with zoom buttons, wheel zoom, drag to pan, a hover
  `PlainTooltip` (species, strip, slice) and an optional dimensions label. The zoom pill and the
  label sit in a band along the bottom, and the fitted board stays above it. Chrome colours come
  in as `BoardPreviewColors` (from the theme by default); the board is coloured by its woods.
  Only pieces on screen get their textures rendered.
- `PreviewTransform` maps board inches to screen pixels: `fit` centres the board with a 5% margin,
  `zoomAt` keeps the point under the pointer still and clamps zoom to 0.25–8 times the fit.
  `PreviewView` (private) keeps the board fitted until the user zooms or pans.
- `BoardDrawing.kt` prepares each piece once per layout and finish: its texture `TextureSpec` and
  its texture frame. An end-grain cell's frame is its source strip's largest cell on the board
  (by species, strip and lamination), so every slice of a strip shows the same rings and a cell
  trimmed more than the others shows part of the same frame. End-grain textures are drawn
  transposed (texture x along board y), so `mirrored` turns a rotated row's cells end for end.
- `TextureCache` renders `GrainTexture` pixels into bitmaps, keyed by spec and size, least
  recently used out past 256. `bucketPixelsPerInch` rounds the view density up to a power of two
  from 16 to 256 so zooming re-renders only when it doubles or halves.
- Common Compose has no fast pixels-to-bitmap call, so `ArgbImageFactory` is a platform service:
  `desktopMain/.../platform/SkiaArgbImageFactory.kt` copies the pixels into a Skia bitmap once.
  `AppShell` provides it as `LocalArgbImageFactory` from the `AppGraph`.
- `BoardPreviewScreenshotTest` records examples A, B and C (rebuilt in `PreviewExamples.kt` from the
  worked examples) in both finishes, stripes with a corner radius and groove, and A with dimensions.
