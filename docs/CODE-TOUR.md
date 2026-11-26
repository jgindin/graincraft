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
  composable.

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
  merges them with the user's custom species (a custom id never replaces a built-in).
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
