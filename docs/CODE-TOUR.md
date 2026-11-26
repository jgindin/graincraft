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
