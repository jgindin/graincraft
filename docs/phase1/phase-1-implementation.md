# Phase 1 Implementation Plan: core and Linux desktop

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development
> (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use
> checkbox (`- [ ]`) syntax for tracking. When a task is finished, add a **Status:** line under its
> heading saying what was done and where it differs from the steps.

**Goal:** A native Linux desktop GrainCraft Studio that designs edge-grain stripes and end-grain
stripes, checkerboard and brick boards, previews them with accurate grain, and produces a correct
build guide, cut list, lumber list and printable shop packet.

**Architecture:** `:core` is pure Kotlin (JVM + Wasm/Node), holding every domain type and one pure
function, `DesignCompiler.compile`, that turns a design, the catalog and the shop profile into
layout, plan, cut list, lumber and issues. `:app` is Compose Multiplatform with only a desktop
target: Material 3 screens and view models in `commonMain`, storage and dialogs in `desktopMain`
behind interfaces, wired by a hand-written `AppGraph`.

**Tech Stack:** Kotlin 2.4.21, Compose Multiplatform 1.12.2 (Material 3), kotlinx-serialization
1.13.0, kotlinx-coroutines 1.11.0, androidx lifecycle (JetBrains) 2.11.0, FileKit 0.17.0,
Roborazzi 1.79.0 (Compose Desktop), ktfmt Gradle plugin 0.28.0, detekt 2.0.0-alpha.6, Kover
0.9.11, Gradle 9.9.1, JDK 21 via the foojay toolchain resolver 1.0.0. Bump each to the newest stable
release in Task 1, and record what you used in the catalog.

**Spec:** [phase-1-design.md](phase-1-design.md). Read it before starting; this plan argues from it.

## Global Constraints

- Package root `com.jaygindin.graincraft`; `:core` code under `...graincraft.core`, `:app` code
  under `...graincraft.app`.
- `:core` has no platform code and builds for `jvm()` and `wasmJs { nodejs() }`. No `java.*` or
  `javax.*` import in any `commonMain` (detekt `ForbiddenImport`, Task 1).
- Lengths are `Length` (nanometres in a `Long`). Never `Double` inches in domain types.
- Every user-facing measurement goes through `UnitFormatter` (Imperial fractions at 1/16 or 1/32,
  or metric mm with one decimal).
- UI uses Material 3 components. Screens use only theme colours, type and `Spacing`; no
  `Color(0x`, named colours or `fontSize =` outside `ui/theme/`, `ui/preview/` and `ui/diagram/`
  (enforced by `ThemeTest`, Task 14).
- Every interactive element has a `Modifier.testTag`. Tags are listed per screen task.
- ktfmt Google style, 100 columns; detekt with no baseline; comments explain why, in the present
  tense, never history or task numbers.
- Size limits: L 6"–36", W 4"–24", T 3/4"–3".
- Shop profile defaults exactly as in the design's `ShopProfile`.
- Version control: `jj`. One commit per task, on its own bookmark, one PR per task, merged with
  "Rebase and merge". Commit messages are a plain sentence and end with the `Co-Authored-By:`
  trailer you were given. Each PR bumps `version.properties` to `0.<PR number>` and adds a
  `CHANGELOG.md` entry, and updates `docs/CODE-TOUR.md` for what it adds.

Common commands (from the repository root):

```bash
./gradlew :core:allTests                 # core tests on JVM and Wasm
./gradlew :core:jvmTest --tests '<Class>' # one core test class, fast
./gradlew :app:desktopTest --tests '<Class>'
./gradlew verifyRoborazziDesktop          # compare screenshots with goldens
./gradlew recordRoborazziDesktop          # only when a visual change is intended
./scripts/ci.sh                           # everything CI runs
```

## Review Focus

1. **Closing the window less than 1 s after an edit:** the pending autosave must still be written.
   Test: `EditorViewModelTest.closingFlushesPendingSave` (Task 19).
2. **Degenerate pattern parameters** (a checkerboard cell larger than the board, a zero-width strip,
   an empty strip list, a brick wider than the board): compile returns a typed error or clamps as
   specified, never crashes or divides by zero. Tests in Tasks 5 and 11.
3. **Length input in forms people actually type:** `1 3/4"`, `1-3/4 in`, `¾`, `1½`, extra spaces,
   `44,5 mm`. Accepted where unambiguous, rejected with a reason otherwise. Test:
   `LengthParseTest.commonTypingVariants` (Task 2).
4. **Hand-edited or foreign files:** a design that names an unknown species, has negative or
   out-of-range dimensions, or comes from a newer schema shows as an error in the library or editor,
   and is never overwritten. Tests in Tasks 11 and 15.
5. **Metric everywhere:** with Metric selected, the editor fields, Issues, Build, Lumber and the
   shop packet show millimetres (board feet stays board feet), and switching units never changes
   the stored design. Test: `EditorScreenTest.metricShowsMillimetresEverywhere` (Task 21) and
   `ShopPacketExporterTest.metricPacket` (Task 13).

## Worked examples (used by several tasks)

Default shop profile throughout (kerf 1/8", planer 13", no drum sander, crosscut 12", router sled,
`Δp` 1/16", router-sled `Δf` 1/8", edge-grain `Δf` 1/16", end trim 2", side trim `e` 1/8", waste
30%, surfaced 4/4 13/16", 5/4 1-1/16", 6/4 1-5/16", 8/4 1-3/4").

**A. End-grain checkerboard,** Walnut/Hard Maple, target 18 × 12 × 1-1/2, cell 1-1/2.

- `M = 8`, `s = 1-1/2`, `N = 12`, actual length 18 (no fit note).
- 8 strips W M W M W M W M; outer strips (1 and 8) milled 1-5/8 wide, inner 1-1/2.
- `W1 = 12-1/4`, `H1 = 1-9/16`, `t = 1-5/8`, `N_s = 12`, `L1 = 23`.
- Limit `min(13, 12) = 12` < 12-1/4, so split: `1A` = strips 1–4 (6-1/8), `1B` = strips 5–8
  (6-1/8). 12 slices from each.
- Rows 1, 3, 5, … are rotated: they take `1B` then `1A`, each reversed.
- Cut parts per species: 3 @ 1-1/2 × 1-9/16 × 23 and 1 @ 1-5/8 × 1-9/16 × 23.
  - Inner: thickness 1-9/16 from 8/4, rip 1-1/2 → `2 × 1.625 × 23 / 144 = 0.519097` BF.
  - Outer: thickness 1-5/8 from 8/4, rip 1-9/16 → `2 × 1.6875 × 23 / 144 = 0.539063` BF.
  - Per species `2.096354 × 1.3 = 2.725260` → shows **2.7 BF**; total `5.450521` → **5.5 BF**.

**B. Edge-grain stripes,** target 16 × 8-1/4 × 1-1/4, strips Walnut 1-1/2, Maple 3/4, Cherry 1-1/2,
Maple 3/4, Cherry 1-1/2, Maple 3/4, Walnut 1-1/2 (sum 8-1/4, so no scaling).

- Strip height `1-1/4 + 1/16 = 1-5/16`, length `16 + 2 = 18`; outer walnut strips milled 1-5/8.
- `W_g = 8-1/2` ≤ 13: planer, no split.
- Width from stock thickness: walnut 1-5/8 and cherry 1-1/2 from 8/4; maple 3/4 from 4/4. Rip
  width 1-5/16 for all.
  - Walnut `2 × 2 × 1.4375 × 18 / 144 × 1.3 = 0.934375` → **0.9**.
  - Cherry the same → **0.9**.
  - Maple `3 × 1 × 1.4375 × 18 / 144 × 1.3 = 0.700781` → **0.7**.
  - Total `2.569531` → **2.6**.

**C. End-grain brick,** Cherry bricks, Hard Maple mortar, target 17-1/4 × 12 × 1-1/2, brick 3 × 1-1/2,
mortar 1/4.

- `P = 3-1/4`, `N_b = round(17.5 / 1.75) = 10`, `h = (17.25 − 9 × 0.25) / 10 = 1-1/2`, length
  exact.
- `n = ceil((12.25 + 3.25 + 0.25) / 3.25) = 5` bricks, 4 mortar strips, `W1 = 16`.
- Panel 1: B M B M B M B M B, `H1 = 1-9/16`, `t = 1-5/8`, `L1 = 10 × 1.625 + 10 × 0.125 + 2 =
  19-1/2`.
- Split at limit 12: cumulative 3, 3¼, 6¼, 6½, 9½, 9¾, 12¾, 13, 16. Best widest is 9½, tied
  between after-6½ and after-9½ (both with narrowest 6½), so the leftmost wins: `1A` = strips 1–4 (6-1/2), `1B` = strips 5–9
  (9-1/2).
- Mortar panel: maple, `W1 = 16`, height `5/16`, length `9 × 1.75 + 2 = 17-3/4`, split into 2 equal
  sub-panels `MA`, `MB` of 8.
- Rows: brick rows 0, 2, … centred; brick rows 1, 3, … shifted by 1-5/8; mortar row between each
  pair. Glue-up 17-1/4 × 16, trimmed to 12.
- Lumber:
  - Cherry: 5 × (thickness 1-9/16 from 8/4, rip 3) = `5 × 2 × 3.125 × 19.5 / 144 = 4.231771`,
    × 1.3 = `5.501302` → **5.5**.
  - Maple panel-1 strips: 4 × (thickness 1-9/16 from 8/4, rip 1/4) = `4 × 2 × 0.375 × 19.5 / 144 =
    0.40625`.
  - Maple mortar boards: 2 × (thickness 5/16 from 4/4, rip 8) = `2 × 1 × 8.125 × 17.75 / 144 =
    2.003038`.
  - Maple total `2.409288 × 1.3 = 3.132075` → **3.1**. Total `8.633377` → **8.6**.

---

### Task 1: Gradle build, tooling and an empty window

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`,
  `gradle/libs.versions.toml`, `gradle/wrapper/*`, `gradlew`, `gradlew.bat` (copy the wrapper from
  `~/src/gindin/straphanger`), `config/detekt/detekt.yml`, `version.properties`, `CHANGELOG.md`,
  `docs/CODE-TOUR.md`, `scripts/ci.sh`, `scripts/run-desktop.sh`
- Create: `core/build.gradle.kts`, `core/src/commonMain/kotlin/com/jaygindin/graincraft/core/Core.kt`,
  `core/src/commonTest/kotlin/com/jaygindin/graincraft/core/CoreSmokeTest.kt`
- Create: `app/build.gradle.kts`, `app/src/desktopMain/kotlin/com/jaygindin/graincraft/app/Main.kt`,
  `app/src/commonMain/kotlin/com/jaygindin/graincraft/app/App.kt`,
  `app/src/desktopTest/kotlin/com/jaygindin/graincraft/app/AppSmokeTest.kt`
- Modify: `.github/workflows/ci.yml` (drop every `if: hashFiles('gradlew') != ''`, run
  `./scripts/ci.sh`), `AGENTS.md` (replace "Current state" and "once the build exists" with what
  now exists)

**Interfaces:**
- Produces: modules `:core` and `:app`; `const val CORE_VERSION_MARKER = "graincraft-core"` in
  `Core.kt` (deleted in Task 2, it only proves the module builds); `@Composable fun App()` in
  `App.kt` showing the text "GrainCraft Studio".

- [ ] **Step 1: Build files.** Root `build.gradle.kts` applies ktfmt (Google style, 100 columns)
  and detekt (`buildUponDefaultConfig`, config `config/detekt/detekt.yml`, sources
  `src/**/kotlin/**/*.kt`) to all projects, as straphanger's does, with the same `exclude(...)` on
  ktfmt tasks. `settings.gradle.kts` uses the foojay resolver and includes `:core` and `:app`.
  `gradle.properties`: `org.gradle.jvmargs=-Xmx3G -Dfile.encoding=UTF-8`,
  `org.gradle.caching=true`.
- [ ] **Step 2: `:core`.** KMP with `jvm()` and `wasmJs { nodejs() }`, `jvmToolchain(21)`,
  kotlinx-serialization plugin, `commonMain` dependency `kotlinx-serialization-json`, `commonTest`
  dependency `kotlin-test`. Kover on `:core` with `koverVerify` minimum 85% lines from the start (the
  smoke test covers the one file).
- [ ] **Step 3: `:app`.** KMP with `jvm("desktop")`, Compose Multiplatform + compiler plugins,
  Material 3, `compose.components.resources`, lifecycle viewmodel-compose, coroutines (with
  `kotlinx-coroutines-swing` in `desktopMain`), FileKit dialogs, `project(":core")`. `desktopTest`:
  `compose.desktop.uiTestJUnit4`, `compose.desktop.currentOs`, `kotlin-test`, Roborazzi
  `roborazzi-compose-desktop`. Apply the Roborazzi plugin with `outputDir = file("screenshots")`.
  `compose.desktop.application`: `mainClass = "com.jaygindin.graincraft.app.MainKt"`,
  `nativeDistributions { targetFormats(TargetFormat.Deb); packageName = "graincraft-studio";
  packageVersion = "<versionName>.0" }` read from `version.properties`.
- [ ] **Step 4: detekt config.** Copy straphanger's `MagicNumber` and `FunctionNaming` overrides
  (adjusting excluded paths to `**/commonTest/**`, `**/desktopTest/**`, `**/ui/theme/**`), and add:
  ```yaml
  style:
    ForbiddenImport:
      active: true
      # Shared code also targets Wasm (and later Android); Java APIs belong in desktopMain/jvmMain.
      forbiddenImports: ['java.*', 'javax.*']
      excludes: ['**/desktopMain/**', '**/desktopTest/**', '**/jvmMain/**', '**/jvmTest/**']
  ```
- [ ] **Step 5: Smoke tests.** `CoreSmokeTest.markerIsSet` asserts `CORE_VERSION_MARKER ==
  "graincraft-core"`. `AppSmokeTest.showsAppName` uses `runComposeUiTest { setContent { App() } }`
  and asserts `onNodeWithText("GrainCraft Studio").assertExists()`.
- [ ] **Step 6: Prove the import ban.** Temporarily add `import java.io.File` to `Core.kt`, run
  `./gradlew detekt`, and expect a `ForbiddenImport` failure. Remove it.
- [ ] **Step 7: Scripts.** `scripts/ci.sh` (`set -euo pipefail`): `./gradlew ktfmtCheck detekt`
  then `./gradlew :core:allTests :app:desktopTest verifyRoborazziDesktop koverVerify
  :app:packageDeb`. `scripts/run-desktop.sh`: `./gradlew :app:run`. If the Roborazzi plugin names
  its tasks differently for a target called `desktop`, use the real names here, in CI, `AGENTS.md`
  and the design.
- [ ] **Step 8: Run** `./scripts/ci.sh`. Expected: BUILD SUCCESSFUL, the `.deb` in
  `app/build/compose/binaries/main/deb/`. Then `./scripts/run-desktop.sh` opens a window titled
  "GrainCraft Studio".
- [ ] **Step 9: Commit** on bookmark `build-setup`, push, open the PR, and check that CI's `verify`
  now runs Gradle. If the desktop UI tests fail on the runner for lack of a display, wrap the test
  command in `xvfb-run -a` in `ci.yml` and say so in `AGENTS.md`.

### Task 2: Lengths, parsing, formatting and board feet

**Files:**
- Create: `core/src/commonMain/kotlin/com/jaygindin/graincraft/core/units/Length.kt`,
  `LengthParser.kt`, `UnitFormatter.kt`, `Lumber.kt` (nominal sizes and board feet)
- Test: `core/src/commonTest/kotlin/com/jaygindin/graincraft/core/units/LengthTest.kt`,
  `LengthParseTest.kt`, `UnitFormatterTest.kt`, `BoardFeetTest.kt`
- Delete: `Core.kt`, `CoreSmokeTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  @Serializable @JvmInline value class Length(val nanometres: Long) : Comparable<Length> {
    operator fun plus(o: Length): Length; operator fun minus(o: Length): Length
    operator fun times(n: Int): Length; operator fun div(n: Int): Length   // rounds half up to 1 nm
    operator fun div(o: Length): Double
    fun roundToSixtyFourth(): Length                                        // half up
    val inches: Double; val millimetres: Double
    companion object {
      val ZERO: Length
      const val NM_PER_INCH = 25_400_000L
      fun inches(whole: Int, numerator: Int = 0, denominator: Int = 1): Length // exact; denominator must divide 64
      fun inchesApprox(value: Double): Length                                 // rounds to nm
      fun millimetres(value: Double): Length
    }
  }
  fun max(a: Length, b: Length): Length; fun min(a: Length, b: Length): Length
  fun Iterable<Length>.sum(): Length
  enum class LengthUnit { INCH, MILLIMETRE }
  sealed interface LengthParse { data class Ok(val length: Length) : LengthParse
    data class Error(val reason: String) : LengthParse }
  fun parseLength(text: String, defaultUnit: LengthUnit): LengthParse
  enum class FractionPrecision(val denominator: Int) { SIXTEENTH(16), THIRTY_SECOND(32) }
  class UnitFormatter(val unit: LengthUnit, val precision: FractionPrecision) {
    fun format(length: Length): String      // `1-3/4"`, `13"`, `3/32"`, `44.5 mm`
  }
  enum class Nominal(val quarters: Int) { FOUR_QUARTER(4), FIVE_QUARTER(5), SIX_QUARTER(6), EIGHT_QUARTER(8);
    val nominalThickness: Length; val label: String } // label "4/4"
  fun boardFeet(nominal: Nominal, ripWidth: Length, length: Length): Double // nominal in × width in × length in / 144
  ```
  In tests, `fun inch(w: Int, n: Int = 0, d: Int = 1) = Length.inches(w, n, d)`.

- [ ] **Step 1: Write `LengthTest`.** `sixtyFourthIsExact`: `Length.inches(0, 1, 64).nanometres ==
  396_875L`. `sumsDoNotDrift`: 64 × 1/64" == 1". `divisionRoundsHalfUp`:
  `Length(5) / 2 == Length(3)`. `roundToSixtyFourth`: `Length.inchesApprox(10.0 / 3)
  .roundToSixtyFourth() == inch(3, 21, 64)`. `rejectsNonBinaryDenominator`: `inches(0, 1, 3)`
  throws `IllegalArgumentException`.
- [ ] **Step 2: Write `LengthParseTest`.** Each of these parses to the given length:
  `"1-3/4"`, `"1 3/4"`, `"1.75"`, `"1-3/4\""`, `"1-3/4 in"` → 1-3/4"; `"3/32"` → 3/32";
  `"44.5mm"`, `"44.5 mm"`, `"4.45cm"` → 44.5 mm; with default MILLIMETRE, `"44.5"` → 44.5 mm.
  `commonTypingVariants`: `"  1   3/4  "`, `"¾"` (3/4"), `"1½"` (1-1/2"), `"44,5 mm"` (44.5 mm).
  Errors (each returns `LengthParse.Error` with a non-blank reason): `""`, `"abc"`, `"1/0"`,
  `"-2"`, `"1/3"` (not a binary fraction), `"2 ft"`.
- [ ] **Step 3: Write `UnitFormatterTest`.** At 1/16: `inch(1, 3, 4)` → `1-3/4"`, `inch(13)` →
  `13"`, `inch(0, 3, 32)` → `1/8"` (nearest 16th, half up), `inch(1, 9, 16)` → `1-9/16"`,
  `inch(0, 1, 64)` → `0"`. At 1/32: `inch(0, 3, 32)` → `3/32"`. Metric: `millimetres(44.5)` →
  `44.5 mm`, `inch(1)` → `25.4 mm`.
- [ ] **Step 4: Write `BoardFeetTest`.** `boardFeet(EIGHT_QUARTER, inch(1, 5, 8), inch(23))` ≈
  `0.519097` (± 1e-6) and `Nominal.FOUR_QUARTER.label == "4/4"`.
- [ ] **Step 5: Run** `./gradlew :core:jvmTest`. Expected: FAIL (unresolved references).
- [ ] **Step 6: Implement** the four files. The parser normalises whitespace, `″`/`"`/`in`, `½ ¼ ¾
  ⅛ ⅜ ⅝ ⅞`, and a comma decimal followed by a metric unit, then reads `whole[-| ]num/den`,
  decimals, and `mm`/`cm` suffixes.
- [ ] **Step 7: Run** `./gradlew :core:allTests`. Expected: PASS on JVM and Wasm.
- [ ] **Step 8: Commit** on bookmark `lengths`.

### Task 3: Wood catalog and cutting-board ratings

**Files:**
- Create: `core/.../core/wood/WoodSpecies.kt`, `WoodCatalog.kt` (the 26 built-ins),
  `CustomWoodRating.kt`
- Test: `core/.../core/wood/WoodCatalogTest.kt`, `CustomWoodRatingTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  @Serializable data class Rgb(val argb: Int) { val luminance: Double; companion object { fun hex(s: String): Rgb } }
  @Serializable data class WoodTones(val base: Rgb, val latewood: Rgb, val pore: Rgb)
  enum class PoreType { DIFFUSE, SEMI_RING, RING_OPEN, RING_CLOSED }
  enum class Rating { RECOMMENDED, CAUTION, NOT_SUITABLE }   // ordered best to worst
  @Serializable data class GrainRating(val rating: Rating, val reasons: List<String>)
  @Serializable data class Suitability(val edgeGrain: GrainRating, val endGrain: GrainRating)
  @Serializable data class WoodSpecies(
    val id: String, val name: String, val botanicalName: String, val jankaLbf: Int,
    val raw: WoodTones, val oiled: WoodTones, val poreType: PoreType,
    val oily: Boolean, val bleedsColor: Boolean, val irritant: Boolean,
    val suitability: Suitability, val notes: List<String>, val custom: Boolean = false)
  object WoodCatalog { val builtIns: List<WoodSpecies>; fun byId(id: String): WoodSpecies? }
  class Woods(customs: List<WoodSpecies>) { fun byId(id: String): WoodSpecies?; val all: List<WoodSpecies> } // custom ids never clash with built-ins
  fun rateCustomWood(jankaLbf: Int, poreType: PoreType, oily: Boolean, irritant: Boolean): Suitability
  fun derivedTones(base: Rgb): WoodTones  // latewood and pore darkened from base
  ```
  Built-in ids are kebab-case names: `hard-maple`, `black-walnut`, `american-cherry`, `red-oak`, …

- [ ] **Step 1: Write `WoodCatalogTest`.**
  - `hasTwentySixSpecies` with unique ids.
  - `ratingsMatchTheDesignTable`: a table in the test of `id → (janka, edge, end)` copied from the
    design's table, compared to the catalog.
  - `flagsMatchTheDesign`: oily = {teak, wenge, bocote, cocobolo}; bleeds = {african-padauk,
    bloodwood}; irritant includes purpleheart, african-padauk, yellowheart, zebrawood, wenge,
    bubinga, osage-orange, bocote, western-red-cedar, cocobolo, jatoba.
  - `everyCautionOrWorseHasAReason`.
  - `oiledIsDarkerThanRaw`: luminance of `oiled.base` < `raw.base` for every species.
- [ ] **Step 2: Write `CustomWoodRatingTest`.** `(1450, DIFFUSE, false, false)` → both
  RECOMMENDED. `(850, DIFFUSE, …)` → both CAUTION with reason containing "soft". `(550, …)` → both
  NOT_SUITABLE. `(1200, RING_OPEN, …)` → edge CAUTION, end NOT_SUITABLE. `(1200, DIFFUSE,
  oily = true, …)` → both CAUTION. Worst rule wins: `(550, RING_OPEN, true, true)` → both
  NOT_SUITABLE, with all four reasons.
- [ ] **Step 3: Run** `./gradlew :core:jvmTest --tests '*Wood*'`. Expected: FAIL.
- [ ] **Step 4: Implement.** Data comes from the design's table; tone hex values are the
  implementer's, chosen to be recognisable (walnut dark brown, maple pale cream, padauk orange-red,
  purpleheart purple, and so on). Keep the reasons' wording from the table.
- [ ] **Step 5: Run** `./gradlew :core:allTests`. Expected: PASS.
- [ ] **Step 6: Commit** on bookmark `wood-catalog`.

### Task 4: Designs, shop profile and their JSON files

**Files:**
- Create: `core/.../core/design/BoardDesign.kt` (`BoardDesign`, `GrainMode`, `Dimensions`),
  `PatternSpec.kt`, `StripePresets.kt`, `EdgeTreatments.kt`, `DesignJson.kt`,
  `core/.../core/shop/ShopProfile.kt`
- Test: `core/.../core/design/DesignJsonTest.kt`, `StripePresetsTest.kt`,
  `core/.../core/shop/ShopProfileTest.kt`

**Interfaces:**
- Consumes: `Length`, `WoodSpecies`, `Nominal`.
- Produces: the design's `BoardDesign`, `PatternSpec` (`Stripes`, `Checkerboard`, `Brick`),
  `EdgeTreatments` and `ShopProfile` exactly as written there, plus:
  ```kotlin
  enum class GrainMode { EDGE_GRAIN, END_GRAIN }
  @Serializable data class Dimensions(val length: Length, val width: Length, val thickness: Length)
  @Serializable data class Strip(val speciesId: String, val width: Length)
  // PatternSpec.Stripes also has `rowHeight: Length = 1-1/2"` (end grain only)
  enum class EdgeProfile { NONE, CHAMFER, ROUNDOVER }
  @Serializable data class JuiceGroove(val width: Length, val depth: Length, val inset: Length)
  @Serializable data class EdgeTreatments(val cornerRadius: Length = Length.ZERO,
    val edgeProfile: EdgeProfile = EdgeProfile.NONE, val edgeProfileSize: Length = Length.ZERO,
    val juiceGroove: JuiceGroove? = null, val fingerGrips: Boolean = false, val feet: Boolean = false)
  enum class FlatteningMethod { DRUM_SANDER, ROUTER_SLED, BELT_AND_ORBITAL }
  object StripePresets {
    fun uniform(speciesIds: List<String>, width: Length): List<Strip>
    fun alternating(speciesIds: List<String>, count: Int, width: Length): List<Strip>
    fun mirror(strips: List<Strip>): List<Strip>          // ABC → ABCBA
    fun fibonacci(a: String, b: String, unit: Length): List<Strip> // 1,1,2,3,5 mirrored, alternating species
    fun pinstripe(strips: List<Strip>, accentId: String, accentWidth: Length): List<Strip> // accent between each pair
  }
  object DesignJson {
    const val SCHEMA_VERSION = 1
    fun encode(design: BoardDesign): String
    fun decode(text: String): DesignDecode
  }
  sealed interface DesignDecode { data class Ok(val design: BoardDesign) : DesignDecode
    data class NewerSchema(val version: Int) : DesignDecode
    data class Invalid(val reason: String) : DesignDecode }
  object WorkspaceJson { val json: Json } // shared settings: ignoreUnknownKeys = true, encodeDefaults = true, prettyPrint = true
  ```
  `ShopProfile.endGrainFlattenAllowance` and `surfacedThickness` serialise as maps keyed by enum
  name.

- [ ] **Step 1: Write `DesignJsonTest`.** `roundTripsEveryPattern` (stripes, checkerboard, brick
  with edge treatments and an embedded custom wood): `decode(encode(d)) == Ok(d)`.
  `newerSchemaIsRefused`: JSON with `"schemaVersion": 2` → `NewerSchema(2)`. `garbageIsInvalid`:
  `"{"` → `Invalid`. `lengthsAreStoredAsNanometres`: `encode` of a 1" length contains
  `25400000`.
- [ ] **Step 2: Write `StripePresetsTest`.** `mirror(ABC)` species order `A B C B A`;
  `fibonacci("w", "m", 1/2")` widths `1/2, 1/2, 1, 1-1/2, 2-1/2, 1-1/2, 1, 1/2, 1/2` with species
  alternating starting `w`; `pinstripe([A,B], "p", 1/8")` = `A p B`.
- [ ] **Step 3: Write `ShopProfileTest.defaultsMatchTheDesign`:** every default value from the
  design, including the four surfaced thicknesses and the three end-grain allowances.
- [ ] **Step 4: Run, expect FAIL; implement; run** `./gradlew :core:allTests`, expect PASS.
- [ ] **Step 5: Commit** on bookmark `design-model`.

### Task 5: Fitting patterns to the target size

**Files:**
- Create: `core/.../core/fabrication/PatternFit.kt`
- Test: `core/.../core/fabrication/PatternFitTest.kt`

**Interfaces:**
- Consumes: `PatternSpec`, `Dimensions`, `Length`.
- Produces:
  ```kotlin
  data class FitStrip(val speciesId: String, val finishedWidth: Length, val role: StripRole)
  enum class StripRole { PLAIN, BRICK, MORTAR }
  data class Fit(
    val columns: List<FitStrip>,          // finished widths across Y, in order (before side trim allowance)
    val rows: List<FitRow>,               // along X; one row for edge grain
    val actual: Dimensions,
    val notes: List<String>)              // e.g. "Length is 17-7/8\", 1/8\" under target, to keep squares square"
  data class FitRow(val kind: RowKind, val height: Length)
  enum class RowKind { PLAIN, BRICK, MORTAR }
  sealed interface FitResult { data class Ok(val fit: Fit) : FitResult; data class Failed(val error: CompileError) : FitResult }
  fun fitPattern(mode: GrainMode, target: Dimensions, pattern: PatternSpec, formatter: UnitFormatter): FitResult
  ```
  `CompileError` is defined here and reused by later tasks:
  ```kotlin
  sealed interface CompileError {
    data class OutOfRange(val field: String, val min: Length, val max: Length) : CompileError
    data object PatternNotAllowedInEdgeGrain : CompileError
    data object NoStrips : CompileError
    data class NonPositiveWidth(val stripIndex: Int) : CompileError
    data class UnknownSpecies(val id: String) : CompileError
    data class NoValidSplit(val panel: String, val limit: Length) : CompileError
  }
  ```
  Brick `columns` describe one unshifted period sequence; shifting and widening happen in Task 7.

- [ ] **Step 1: Write the tests.**
  - `stripesScaleToWidth`: strips 2, 1, 2 at W 10 → 4, 2, 4.
  - `stripesRemainderGoesToWidest`: three strips of 1 at W 10 → 3-11/32 (214/64), 3-21/64,
    3-21/64; sum exactly 10. Ties go to the first.
  - `stripesAlreadyFittingAreUnchanged`: Example B's strips at 8-1/4.
  - `endGrainStripeRows`: L 16, rowHeight 1-1/2 → 11 rows of `16" / 11`.
  - `checkerboardExampleA`: 8 columns of 1-1/2, 12 rows, actual 18 × 12, no notes.
  - `checkerboardRoundsToEvenColumns`: W 12, cell 1-3/4 → `12/1.75 = 6.86` → `2·round(3.43) = 6`
    columns of 2; L 18 → 9 rows; actual length 18.
  - `checkerboardReportsActualLength`: W 12, L 17, cell 1-1/2 → 8 columns, `round(11.33) = 11`
    rows, actual length 16-1/2, and one note naming both lengths.
  - `checkerboardCellLargerThanBoardClampsToTwoColumns`: W 12, cell 20 → 2 columns of 6.
  - `brickExampleC`: 10 brick rows of 1-1/2 alternating with 9 mortar rows of 1/4, actual length
    17-1/4.
  - Errors: empty strips → `NoStrips`; a zero-width second strip → `NonPositiveWidth(1)`; checkerboard in
    edge grain → `PatternNotAllowedInEdgeGrain`; L 40 → `OutOfRange("length", 6", 36")`; T 1/2 →
    `OutOfRange("thickness", …)`; brick width 30 with W 12 is allowed (trimmed later).
- [ ] **Step 2: Run, expect FAIL; implement; run** `./gradlew :core:allTests`, expect PASS.
  Rounding: `round` is half up; `M = max(2, 2·round(W / (2·cell)))`; `N = max(1, round(L / s))`;
  `N_b = max(1, round((L + m) / (h + m)))`.
- [ ] **Step 3: Commit** on bookmark `pattern-fit`.

### Task 6: Flattening tool choice and panel splitting

**Files:**
- Create: `core/.../core/fabrication/Flattening.kt`, `PanelSplitter.kt`
- Test: `core/.../core/fabrication/FlatteningTest.kt`, `PanelSplitterTest.kt`

**Interfaces:**
- Consumes: `ShopProfile`, `Length`.
- Produces:
  ```kotlin
  enum class FlatteningTool { PLANER, DRUM_SANDER, ROUTER_SLED, BELT_AND_ORBITAL }
  data class LongGrainFlattening(val tool: FlatteningTool, val widthLimit: Length?, val allowance: Length)
  fun longGrainFlattening(panelWidth: Length, profile: ShopProfile): LongGrainFlattening
  fun splitLimit(flattening: LongGrainFlattening, endGrainPanel: Boolean, profile: ShopProfile): Length?
  /** Contiguous groups of strip indices: fewest groups, then the smallest widest group, then the
   *  widest narrowest group, then the leftmost boundaries (compared in order); null if impossible. */
  fun splitPanel(stripWidths: List<Length>, limit: Length?): List<IntRange>? // tie-breaks: see the doc comment
  fun splitEvenly(width: Length, limit: Length?): List<Length> // ceil(width/limit) equal parts (mortar panels)
  fun subPanelNames(base: String, count: Int): List<String>   // ("1", 1) → ["1"]; ("1", 2) → ["1A", "1B"]
  ```
  Tool rule from the design: the first set machine (planer, then drum sander) whose width fits; if
  none fits but one is set, the widest set machine (and the panel is split); if none is set, the
  profile's end-grain method with no limit. Machines use `panelFlattenAllowance`; the end-grain
  method uses its own allowance.

- [ ] **Step 1: Write `FlatteningTest`.** Default profile, width 12-1/4 → PLANER, limit 13,
  allowance 1/16. Planer 13 + drum 16, width 15 → DRUM_SANDER. Planer 13, no drum, width 15 →
  PLANER (split). No machines → ROUTER_SLED, limit null, allowance 1/8. `splitLimit` for an
  end-grain panel with planer 13 and crosscut 12 → 12; edge grain → 13; no machines, end grain →
  12.
- [ ] **Step 2: Write `PanelSplitterTest`.** Fits → one group. Example A widths (1-5/8, 1-1/2 × 6,
  1-5/8) at 12 → `[0..3, 4..7]`. Example C widths (3, 1/4, …, 3) at 12 → `[0..3, 4..8]`
  (leftmost tie). Ten strips of 2 at 6 → 4 groups `[0..1, 2..3, 4..6, 7..9]` (widest 6,
  narrowest 4, then leftmost boundaries). One strip of 14 at 12 → null. `null` limit → one group. `splitEvenly(16", 12")` →
  `[8", 8"]`. `subPanelNames("M", 2)` → `["MA", "MB"]`.
- [ ] **Step 3: Run, expect FAIL; implement; run** `./gradlew :core:allTests`, expect PASS.
- [ ] **Step 4: Commit** on bookmark `panel-splitting`.

### Task 7: Fabrication: panels, slices and rows

**Files:**
- Create: `core/.../core/fabrication/Fabrication.kt` (types), `FabricationCompiler.kt`
- Test: `core/.../core/fabrication/FabricationCompilerTest.kt`

**Interfaces:**
- Consumes: `Fit` (Task 5), `longGrainFlattening`, `splitLimit`, `splitPanel`, `splitEvenly`,
  `subPanelNames` (Task 6), `ShopProfile`.
- Produces:
  ```kotlin
  enum class PanelKind { EDGE_GRAIN_BOARD, PANEL_1, MORTAR }
  data class PanelStrip(val speciesId: String, val millWidth: Length, val sourceStripIndex: Int, val role: StripRole)
  data class Panel(val name: String, val kind: PanelKind, val strips: List<PanelStrip>,
    val width: Length, val millHeight: Length, val flattenTo: Length, val length: Length,
    val flattening: LongGrainFlattening)
  data class Row(val index: Int, val kind: RowKind, val height: Length, val rotated: Boolean,
    val shift: Length, val panels: List<String>) // sub-panel names, left to right as placed
  data class Fabrication(val mode: GrainMode, val panels: List<Panel>, val sliceThickness: Length?,
    val slicesPerPanel: Map<String, Int>, val rows: List<Row>, val glueUpWidth: Length,
    val glueUpLength: Length, val finalFlatten: FlatteningMethod?, val finalAllowance: Length)
  sealed interface FabricationResult { data class Ok(val fabrication: Fabrication) : FabricationResult
    data class Failed(val error: CompileError) : FabricationResult }
  fun compileFabrication(mode: GrainMode, target: Dimensions, fit: Fit, profile: ShopProfile): FabricationResult
  ```
  `millHeight` is what strips are milled to; `flattenTo` is what the panel is flattened down to
  (`r` for panel 1; `T` for edge grain; `m` for mortar). Edge-grain split boards flatten to
  `T + 1/32"`. For brick, panel 1 strips are the centred sequence of `n` bricks and `n − 1` mortar
  strips (design formula), with no extra side allowance; for stripes and checkerboard the outer
  strips' `millWidth` is `finishedWidth + e`.

- [ ] **Step 1: Write the tests, one per worked example, asserting every number listed there.**
  - `exampleA_endGrainCheckerboard`: panels `1A`, `1B` (4 strips each, widths 1-5/8 + 3 × 1-1/2 and
    3 × 1-1/2 + 1-5/8, species alternating starting walnut); `millHeight` 1-9/16; `flattenTo`
    1-1/2; `length` 23; `sliceThickness` 1-5/8; 12 slices each; 12 rows of 1-1/2 where odd rows
    have `rotated = true` and `panels == ["1B", "1A"]`, even rows `["1A", "1B"]`; glue-up
    18 × 12-1/4; final flatten ROUTER_SLED with allowance 1/8.
  - `exampleB_edgeGrainStripes`: one panel `1` of kind EDGE_GRAIN_BOARD, 7 strips with mill widths
    1-5/8, 3/4, 1-1/2, 3/4, 1-1/2, 3/4, 1-5/8; `millHeight` 1-5/16; `flattenTo` 1-1/4; `length`
    18; PLANER; no rows beyond one PLAIN row; `sliceThickness` null.
  - `exampleC_endGrainBrick`: panel 1 split `1A` (B M B M = 6-1/2) and `1B` (B M B M B = 9-1/2),
    `millHeight` 1-9/16, `length` 19-1/2; mortar panels `MA`, `MB` 8 wide, `millHeight` 5/16,
    `flattenTo` 1/4, `length` 17-3/4; 19 rows alternating BRICK/MORTAR starting BRICK; the brick
    rows at overall indices 2, 6, 10, 14, 18 have `shift` 1-5/8, all other rows 0; glue-up
    17-1/4 × 16; 10 slices from each panel-1 sub-panel and 9 from each mortar sub-panel.
  - `edgeGrainSplitBoardFlattensProud`: 20 strips of 3/4 (W 15, T 1-1/4, planer 13) → two
    sub-panels, `flattenTo` 1-9/32.
  - `noValidSplitIsAnError`: one 14" strip in end grain → `NoValidSplit("1", 12")`.
- [ ] **Step 2: Run, expect FAIL; implement; run** `./gradlew :core:allTests`, expect PASS.
- [ ] **Step 3: Commit** on bookmark `fabrication`.

### Task 8: Layout of the finished board

**Files:**
- Create: `core/.../core/layout/Layout.kt`, `LayoutBuilder.kt`
- Test: `core/.../core/layout/LayoutBuilderTest.kt`

**Interfaces:**
- Consumes: `Fabrication` (Task 7), `Dimensions`.
- Produces:
  ```kotlin
  enum class Fibre { X, Z }
  data class Point(val x: Length, val y: Length)
  data class Piece(val polygon: List<Point>, val speciesId: String, val fibre: Fibre,
    val sourceStripIndex: Int, val laminationIndex: Int, val sliceIndex: Int?, val flipped: Boolean,
    val shiftY: Length, val panelName: String)
  data class BoardLayout(val size: Dimensions, val pieces: List<Piece>)
  fun buildLayout(fabrication: Fabrication, finished: Dimensions): BoardLayout
  ```
  Coordinates are the finished board, `x ∈ [0, L]`, `y ∈ [0, W]`, after side trimming (centred
  trim for brick). Pieces are clipped rectangles. An edge-grain strip wider than 1-3/4" becomes two
  pieces with the same `sourceStripIndex` and `laminationIndex` 0 and 1. `sourceStripIndex` is the
  strip's index in panel 1 before splitting (mortar-panel pieces use −1 − sub-panel index).

- [ ] **Step 1: Write the tests.**
  - `piecesTileTheBoard` for examples A, B and C: the sum of piece areas equals `L × W` exactly
    (in nm²), and no two pieces overlap (pairwise rectangle check).
  - `checkerboardAlternates`: in example A, the cell at row 0, column 0 is walnut, row 1 column 0
    is maple, row 0 column 1 is maple; every odd-row piece has `flipped = true`.
  - `sameStripSameSource`: in example A, the cells in column 0 of rows 0 and 2 share
    `sourceStripIndex`; row 1 column 0 has the index of strip 8 (`7`).
  - `outerStripsAreTrimmedToFinishedWidth`: example A's column 0 and column 7 pieces are 1-1/2
    wide.
  - `brickRowsShift`: in example C (panel 1 centred, so it starts at `y = −2`), the vertical
    mortar pieces of row 0 start at `y` = 1, 4-1/4, 7-1/2, 10-3/4, and those of row 2 at 2-5/8,
    5-7/8, 9-1/8. Row 1 is one maple piece across the full width.
  - `fibreMatchesGrainMode`: all X for B; all Z for A and C.
  - `wideEdgeGrainStripIsLaminated`: a 2-1/2" strip yields two pieces of 1-1/4.
- [ ] **Step 2: Run, expect FAIL; implement; run** `./gradlew :core:allTests`, expect PASS.
- [ ] **Step 3: Commit** on bookmark `layout`.

### Task 9: Cut list and lumber

**Files:**
- Create: `core/.../core/fabrication/CutList.kt`
- Test: `core/.../core/fabrication/CutListTest.kt`

**Interfaces:**
- Consumes: `Fabrication`, `ShopProfile`, `Nominal`, `boardFeet`.
- Produces:
  ```kotlin
  data class CutPart(val speciesId: String, val count: Int, val width: Length, val height: Length,
    val length: Length, val stock: Nominal?, val fromThickness: Length, val ripWidth: Length,
    val laminated: Boolean, val panelNames: List<String>)  // stock null = too thick: laminate or buy thicker
  data class SpeciesLumber(val speciesId: String, val boardFeet: Double) // waste included
  data class Lumber(val bySpecies: List<SpeciesLumber>, val total: Double)
  fun cutList(fabrication: Fabrication, profile: ShopProfile): List<CutPart>
  fun lumber(parts: List<CutPart>, profile: ShopProfile): Lumber
  fun formatBoardFeet(bf: Double): String // one decimal, half up: "5.5 BF"
  ```
  Identical parts (species, width, height, length) merge, adding counts and panel names. Edge-grain
  parts take width from thickness; a width over the thickest surfaced size becomes two laminated
  parts of half the width (`laminated = true`). All other parts use the min-board-feet orientation
  rule from the design.

- [ ] **Step 1: Write the tests** asserting Example A, B and C's parts and lumber exactly as listed
  (counts, sizes, chosen nominal, rip width; board feet ± 1e-6; formatted 2.7 / 5.5, 0.9 / 0.9 /
  0.7 / 2.6, 5.5 / 3.1 / 8.6). Plus `thinStripsAreRippedFromThickStock`: a 1/4 × 1-9/16 part
  chooses 8/4 with rip 1/4. `tooThickBothWays`: 2 × 2-1/2 part → `stock == null`.
  `wideEdgeGrainStripIsTwoLaminations`: 2-1/2 strip → 2 parts at 1-1/4, `laminated`.
- [ ] **Step 2: Run, expect FAIL; implement; run** `./gradlew :core:allTests`, expect PASS.
- [ ] **Step 3: Commit** on bookmark `cut-list`.

### Task 10: Woodworking warnings

**Files:**
- Create: `core/.../core/audit/WoodAuditor.kt`
- Test: `core/.../core/audit/WoodAuditorTest.kt`

**Interfaces:**
- Consumes: `Fit`, `Woods`, `GrainMode`, `Dimensions`.
- Produces:
  ```kotlin
  enum class Severity { INFO, WARNING, AVOID }
  enum class IssueCode { SPECIES_CAUTION, SPECIES_NOT_SUITABLE, JANKA_SPREAD, OILY_SPECIES,
    COLOR_BLEED, STRIP_TOO_THIN, END_GRAIN_TOO_THIN }
  data class Issue(val code: IssueCode, val severity: Severity, val title: String, val detail: String,
    val speciesIds: List<String> = emptyList(), val stripIndices: List<Int> = emptyList())
  fun audit(mode: GrainMode, finished: Dimensions, fit: Fit, woods: Woods, formatter: UnitFormatter): List<Issue> // sorted AVOID, WARNING, INFO
  ```
  Thresholds: Janka spread `> 1100` lbf; strip or mortar under 1/8"; end-grain thickness under
  1-1/4". Colour bleed fires when a bleeding species and any species with a higher raw base
  luminance are in the same board. The design's edge-grain "under 3/4"" warning is not
  implemented: the size range already starts at 3/4", so it could never fire.

- [ ] **Step 1: Write the tests.** Red oak end grain → one AVOID with the catalog's reason; red oak
  edge grain → WARNING. Hard maple + walnut → no species issues, no Janka issue (440). Bloodwood +
  cherry → JANKA_SPREAD WARNING naming both. Teak present → OILY_SPECIES INFO. Padauk + maple →
  COLOR_BLEED; padauk + wenge (darker) → none. A 1/16" pinstripe → STRIP_TOO_THIN naming its
  index. End grain at T 1" → END_GRAIN_TOO_THIN; at 1-1/4" → none. Ordering: AVOID first.
- [ ] **Step 2: Run, expect FAIL; implement; run** `./gradlew :core:allTests`, expect PASS.
- [ ] **Step 3: Commit** on bookmark `auditor`.

### Task 11: Build stages, diagrams and the design compiler

**Files:**
- Create: `core/.../core/guide/Diagram.kt`, `StagePlanner.kt`, `StageDiagramSvgBuilder.kt`,
  `core/.../core/compile/DesignCompiler.kt`
- Test: `core/.../core/guide/StagePlannerTest.kt`, `StageDiagramSvgBuilderTest.kt`,
  `core/.../core/compile/DesignCompilerTest.kt`

**Interfaces:**
- Consumes: everything from Tasks 2–10.
- Produces:
  ```kotlin
  sealed interface DiagramElement {
    data class Box(val x: Double, val y: Double, val w: Double, val h: Double, val fill: Rgb?, val label: String?, val hatched: Boolean = false) : DiagramElement
    data class Line(val x1: Double, val y1: Double, val x2: Double, val y2: Double, val dashed: Boolean = false) : DiagramElement
    data class Text(val x: Double, val y: Double, val text: String) : DiagramElement
    data class Dimension(val x1: Double, val y1: Double, val x2: Double, val y2: Double, val label: String) : DiagramElement
  }
  data class Diagram(val width: Double, val height: Double, val elements: List<DiagramElement>) // units: inches
  enum class CalloutKind { WARNING, TIP }
  data class Callout(val kind: CalloutKind, val text: String)
  enum class StageKind { BUY_AND_MILL, PREPARE, GLUE_PANELS, FLATTEN_PANELS, CROSSCUT, ARRANGE_AND_GLUE, FLATTEN_BOARD, TRIM, EDGE_DETAILS, FINISH }
  data class Stage(val number: Int, val kind: StageKind, val title: String, val steps: List<String>, val callouts: List<Callout>, val diagram: Diagram?)
  fun planStages(design: BoardDesign, fabrication: Fabrication, parts: List<CutPart>, lumber: Lumber, woods: Woods, profile: ShopProfile, formatter: UnitFormatter): List<Stage>
  object StageDiagramSvgBuilder { fun svg(diagram: Diagram, pixelsPerInch: Double = 24.0): String }
  data class CompiledDesign(val design: BoardDesign, val fit: Fit, val fabrication: Fabrication,
    val layout: BoardLayout, val cutList: List<CutPart>, val lumber: Lumber, val stages: List<Stage>, val issues: List<Issue>)
  sealed interface CompileResult { data class Ok(val compiled: CompiledDesign) : CompileResult
    data class Failed(val errors: List<CompileError>) : CompileResult }
  object DesignCompiler { fun compile(design: BoardDesign, woods: Woods, profile: ShopProfile, formatter: UnitFormatter): CompileResult }
  fun CompileError.message(formatter: UnitFormatter): String
  ```
  Stage numbers are consecutive over the stages present. Edge grain has BUY_AND_MILL, PREPARE,
  GLUE_PANELS, FLATTEN_PANELS, TRIM, EDGE_DETAILS (only if any treatment), FINISH. End grain has all
  ten (EDGE_DETAILS only if any treatment). Feet, if chosen, are the last step of FINISH.
  `woods` passed to the compiler already includes the design's embedded custom woods.

- [ ] **Step 1: Write `StagePlannerTest`.**
  - `endGrainAlwaysWarnsAgainstThePlaner`: example A with a profile whose planer is 24" still has
    a FLATTEN_BOARD WARNING callout containing "planer".
  - `acetoneOnlyWithOilyWoods`: PREPARE mentions acetone for a teak design and not for example A.
  - `edgeGrainStageList`: example B's kinds are exactly BUY_AND_MILL, PREPARE, GLUE_PANELS,
    FLATTEN_PANELS, TRIM, FINISH, numbered 1–6.
  - `crosscutStepsNameCountAndThickness`: example A's CROSSCUT mentions `12 slices` and `1-5/8"`
    for each of `1A` and `1B`.
  - `arrangeMentionsRotationAndMortar`: example A mentions rotating every other row; example C
    mentions the mortar rows and the 1-5/8" shift.
  - `edgeDetailsOrder`: groove, corner radius, profile, grips in that order; feet last in FINISH.
  - Diagrams exist for GLUE_PANELS, CROSSCUT and ARRANGE_AND_GLUE.
- [ ] **Step 2: Write `StageDiagramSvgBuilderTest`.** Output starts with `<svg`, parses as XML
  (a small hand-written well-formedness check in common code is enough: balanced tags, escaped
  `&<>` in labels), has one `<rect` per `Box`, and contains no `http` other than the SVG namespace.
- [ ] **Step 3: Write `DesignCompilerTest`.** Each example compiles `Ok` with the fit, panels, cut
  list and lumber of Tasks 5–9. `unknownSpeciesFails`: a strip naming `"no-such-wood"` →
  `Failed([UnknownSpecies("no-such-wood")])`. `outOfRangeFails`. `embeddedCustomWoodIsUsed`: a
  design using a custom wood that is not in the user's list still compiles. `isDeterministic`:
  two compiles are equal.
- [ ] **Step 4: Run, expect FAIL; implement; run** `./gradlew :core:allTests`, expect PASS.
- [ ] **Step 5: Commit** on bookmark `stages-and-compiler`.

### Task 12: Grain textures

**Files:**
- Create: `core/.../core/texture/GrainTexture.kt`
- Test: `core/.../core/texture/GrainTextureTest.kt`

**Interfaces:**
- Consumes: `WoodTones`, `PoreType`, `Fibre`.
- Produces:
  ```kotlin
  data class TextureSpec(val tones: WoodTones, val poreType: PoreType, val fibre: Fibre,
    val seed: Int, val mirrored: Boolean, val pixelsPerInch: Float)
  object GrainTexture { fun render(spec: TextureSpec, widthPx: Int, heightPx: Int): IntArray } // ARGB, row-major
  fun textureSeed(speciesId: String, sourceStripIndex: Int, laminationIndex: Int): Int
  ```
  End grain: rings around a pith outside the piece, chosen from the seed; long grain: lines along X.
  Same seed and size → identical output. The algorithm is the implementer's (see the design's
  acceptance bar).

- [ ] **Step 1: Write the tests.** `deterministic`. `mirroredIsTheMirrorImage`: every row of the
  mirrored render is the reverse of the unmirrored row. `longGrainVariesAcrossNotAlong`: for a
  64 × 64 X-fibre render, the mean absolute difference between horizontally adjacent pixels is
  less than half that between vertically adjacent pixels. `endGrainVariesBothWays`: for Z, the two
  are within a factor of 3 of each other. `usesTheSpeciesTones`: mean colour is closer to
  `tones.base` than to any other species' base for walnut, maple and padauk.
  `differentStripsDiffer`: seeds from strip 0 and strip 1 render differently.
- [ ] **Step 2: Run, expect FAIL; implement; run** `./gradlew :core:allTests`, expect PASS.
- [ ] **Step 3: Commit** on bookmark `grain-texture`.

### Task 13: Shop packet HTML

**Files:**
- Create: `core/.../core/packet/ShopPacketExporter.kt`
- Test: `core/.../core/packet/ShopPacketExporterTest.kt`

**Interfaces:**
- Consumes: `CompiledDesign`, `Woods`, `ShopProfile`, `UnitFormatter`, `StageDiagramSvgBuilder`.
- Produces: `object ShopPacketExporter { fun html(compiled: CompiledDesign, woods: Woods, profile:
  ShopProfile, formatter: UnitFormatter): String; fun defaultFileName(designName: String): String }`
  (`"<name> - shop packet.html"`, with `/` and other path characters replaced by `-`).

- [ ] **Step 1: Write the tests** with example A. `hasEverySection`: headings "Issues",
  "Shopping list", "Cut list", each stage title, "Shop settings". `boardDrawingIsInline`: an
  `<svg` before the first stage, with one `<rect` per layout piece. `selfContained`: no `src=`,
  no `href="http`, no `<link`, no `<script`. `printStyles`: contains `@media print`.
  `escapesNames`: a design named `<Board & "Co">` appears escaped. `metricPacket`: with a metric
  formatter, contains `457.2 mm` (18") and no `"` inch marks in measurements.
  `defaultFileName("Walnut/Maple")` → `"Walnut-Maple - shop packet.html"`.
- [ ] **Step 2: Run, expect FAIL; implement; run** `./gradlew :core:allTests koverVerify`, expect
  PASS.
- [ ] **Step 3: Commit** on bookmark `shop-packet`.

### Task 14: Theme, icons and the theme test

**Files:**
- Create: `app/src/commonMain/kotlin/com/jaygindin/graincraft/app/ui/theme/GrainCraftTheme.kt`
  (light and dark schemes, `GrainCraftTheme`), `Type.kt`, `Dimens.kt` (`GrainCraftShapes`,
  `Spacing`), `RatingColors.kt`, `app/src/commonMain/composeResources/font/<font>.ttf` (+ its
  licence file in the same folder), `composeResources/drawable/ic_*.xml`
- Test: `app/src/desktopTest/kotlin/com/jaygindin/graincraft/app/ui/theme/ThemeTest.kt`,
  `app/src/desktopTest/.../testutil/Contrast.kt`

**Interfaces:**
- Produces:
  ```kotlin
  enum class ThemeChoice { SYSTEM, LIGHT, DARK }
  @Composable fun GrainCraftTheme(dark: Boolean, content: @Composable () -> Unit)
  data class RatingColors(val recommended: Color, val onRecommended: Color, val caution: Color,
    val onCaution: Color, val notSuitable: Color, val onNotSuitable: Color)
  val LocalRatingColors: ProvidableCompositionLocal<RatingColors>
  object Spacing { val xs = 4.dp; val sm = 8.dp; val md = 12.dp; val lg = 16.dp; val xl = 24.dp }
  @Composable fun primaryButtonColors(): ButtonColors; @Composable fun quietButtonColors(): ButtonColors
  ```
  Shapes 4 / 8 / 12 / 16 / 28 dp; type scale as straphanger's `Type.kt` with the bundled font
  family (Inter or Noto Sans, OFL); body never under 13 sp. Icons (Material Symbols, vector XML):
  add, arrow_back, close, delete, undo, redo, print, folder_open, upload_file, download, search,
  settings, carpenter (Shop), forest (Woods), grid_view (Designs), warning, info, error, check,
  edit, more_vert, zoom_in, zoom_out, fit_screen, arrow_upward, arrow_downward.

- [ ] **Step 1: Write `ThemeTest`** (pattern: straphanger's `ThemeTest`), for both schemes:
  `textRolesAreReadable` (contrast ≥ 4.5 for onSurface/surface, onSurfaceVariant/surfaceContainer,
  primary/background, onPrimaryContainer/primaryContainer, error/surfaceContainer, and each
  rating's on-colour on its colour); `bodyAndLabelTextAreNotTooSmall` (bodySmall ≥ 13 sp,
  labelSmall ≥ 11 sp); `usesTheBundledFont`; `screensUseTheThemeNotLiterals` (scan
  `src/commonMain/kotlin/com/jaygindin/graincraft/app` for `Color(0x`, `Color.White`, `Color.Black`,
  `Color.Gray`, `Color.LightGray`, `Color.DarkGray`, `fontSize =`, excluding `/ui/theme/`,
  `/ui/preview/` and `/ui/diagram/`).
- [ ] **Step 2: Run, expect FAIL; implement** a warm, wood-toned palette; **run**
  `./gradlew :app:desktopTest --tests '*ThemeTest'`, expect PASS.
- [ ] **Step 3: Commit** on bookmark `theme`.

### Task 15: Workspace storage

**Files:**
- Create: `app/src/commonMain/.../app/data/WorkspaceStore.kt` (interface and result types),
  `app/src/commonMain/.../app/data/AppSettings.kt`,
  `app/src/desktopMain/.../app/data/FileWorkspaceStore.kt`, `XdgPaths.kt`
- Test: `app/src/desktopTest/.../app/data/FileWorkspaceStoreTest.kt`, `XdgPathsTest.kt`

**Interfaces:**
- Consumes: `DesignJson`, `WorkspaceJson`, `ShopProfile`, `WoodSpecies`.
- Produces:
  ```kotlin
  @Serializable data class WindowPlacement(val x: Int, val y: Int, val width: Int, val height: Int)
  @Serializable data class AppSettings(val units: LengthUnit = LengthUnit.INCH,
    val precision: FractionPrecision = FractionPrecision.SIXTEENTH, val theme: ThemeChoice = ThemeChoice.SYSTEM,
    val window: WindowPlacement? = null)
  sealed interface StoredDesign { val fileName: String
    data class Readable(override val fileName: String, val design: BoardDesign) : StoredDesign
    data class Unreadable(override val fileName: String, val reason: String) : StoredDesign }
  interface WorkspaceStore {
    val rootDescription: String
    suspend fun listDesigns(): List<StoredDesign>
    suspend fun saveDesign(design: BoardDesign)                 // atomic; file designs/<id>.graincraft.json
    suspend fun trashDesign(id: String); suspend fun restoreDesign(id: String)
    suspend fun purgeTrash(nowMillis: Long)                     // removes trash older than 30 days
    suspend fun loadShopProfile(): ShopProfile; suspend fun saveShopProfile(p: ShopProfile)
    suspend fun loadCustomWoods(): List<WoodSpecies>; suspend fun saveCustomWoods(w: List<WoodSpecies>)
    suspend fun loadSettings(): AppSettings; suspend fun saveSettings(s: AppSettings)
  }
  class FileWorkspaceStore(root: java.nio.file.Path, clock: () -> Long) : WorkspaceStore
  fun xdgDataRoot(env: Map<String, String>, home: String): String // $XDG_DATA_HOME/graincraft-studio or ~/.local/share/graincraft-studio
  ```
  Missing files load defaults. A design whose `schemaVersion` is newer is `Unreadable` with "made by
  a newer GrainCraft Studio", and `saveDesign` refuses to overwrite such a file
  (`IllegalStateException`).

- [ ] **Step 1: Write the tests** against a temporary directory. `savesAndListsDesigns`.
  `writeIsAtomic`: after `saveDesign`, no `*.tmp` remains and a second save replaces the file.
  `unparseableFileIsListedNotDeleted`: write `"{"` to `designs/x.graincraft.json` → `Unreadable`,
  and the file's bytes are unchanged. `newerSchemaIsNotOverwritten`. `trashAndRestore`.
  `purgeRemovesOnlyOldTrash`. `defaultsWhenMissing` for profile, woods, settings.
  `XdgPathsTest`: with and without `XDG_DATA_HOME`.
- [ ] **Step 2: Run, expect FAIL; implement; run** `./gradlew :app:desktopTest --tests
  '*WorkspaceStore*' --tests '*XdgPaths*'`, expect PASS.
- [ ] **Step 3: Commit** on bookmark `storage`.

### Task 16: App shell, navigation and Settings

**Files:**
- Create: `app/src/commonMain/.../app/AppGraph.kt`, `AppScaffold.kt` (rail and destinations),
  `app/.../app/ui/settings/SettingsScreen.kt`, `SettingsViewModel.kt`,
  `app/src/commonMain/.../app/platform/Platform.kt` (`FileDialogs`, `ExternalBrowser`, `Clock`
  interfaces), `app/src/desktopMain/.../app/platform/DesktopPlatform.kt`
- Modify: `App.kt`, `Main.kt`
- Test: `app/src/desktopTest/.../app/AppShellTest.kt`, `ui/settings/SettingsScreenTest.kt`,
  `screenshots/ShellScreenshotTest.kt`, `app/src/desktopTest/.../app/testutil/FakePlatform.kt`,
  `FakeWorkspaceStore.kt`

**Interfaces:**
- Consumes: `WorkspaceStore`, `AppSettings`, `GrainCraftTheme`.
- Produces:
  ```kotlin
  interface FileDialogs { suspend fun openDesignFile(): String?; suspend fun saveFile(suggestedName: String, extension: String, contents: String): String? }
  interface ExternalBrowser { fun openFile(path: String); fun openFolder(path: String) }
  class AppGraph(val store: WorkspaceStore, val dialogs: FileDialogs, val browser: ExternalBrowser,
    val clock: () -> Long, val newId: () -> String)
  enum class Destination { DESIGNS, WOODS, SHOP, SETTINGS }
  @Composable fun App(graph: AppGraph)
  class SettingsViewModel(store: WorkspaceStore) : ViewModel { val settings: StateFlow<AppSettings>; fun update(s: AppSettings) }
  val LocalUnitFormatter: ProvidableCompositionLocal<UnitFormatter>
  ```
  Window: title "GrainCraft Studio", default 1280 × 800, minimum 1024 × 700, placement restored from
  and saved to `AppSettings.window`. Test tags: `rail-designs`, `rail-woods`, `rail-shop`,
  `rail-settings`, `settings-units`, `settings-precision`, `settings-theme`, `settings-open-folder`.

- [ ] **Step 1: Write the tests.** `railSwitchesDestinations`. `unitsSettingIsSavedAndApplied`
  (choosing Metric saves to the fake store and `LocalUnitFormatter` formats mm).
  `themeOverride` (Dark makes `MaterialTheme.colorScheme` the dark scheme). `openFolderUsesBrowser`.
  Screenshots: Settings, light and dark (`ShellScreenshotTest`, `captureRoboImage`).
- [ ] **Step 2: Run, expect FAIL; implement; run** `./gradlew :app:desktopTest`, then
  `./gradlew recordRoborazziDesktop`, look at each new image, then `verifyRoborazziDesktop`.
  Expected: PASS.
- [ ] **Step 3: Commit** (with the new goldens) on bookmark `app-shell`.

### Task 17: Designs library

**Files:**
- Create: `app/.../app/ui/library/LibraryScreen.kt`, `LibraryViewModel.kt`, `NewDesignDialog.kt`,
  `DesignThumbnail.kt`
- Test: `app/src/desktopTest/.../ui/library/LibraryViewModelTest.kt`, `LibraryScreenTest.kt`,
  screenshots in `ShellScreenshotTest`

**Interfaces:**
- Consumes: `WorkspaceStore`, `DesignCompiler`, `FileDialogs`, `DesignJson`, `AppGraph.newId`.
- Produces:
  ```kotlin
  data class LibraryItem(val id: String, val name: String, val summary: String, val updatedAtMillis: Long, val compiled: CompiledDesign?)
  data class LibraryState(val items: List<LibraryItem>, val unreadable: List<StoredDesign.Unreadable>, val pendingUndo: String?)
  class LibraryViewModel(graph: AppGraph, woods: () -> Woods, profile: () -> ShopProfile) : ViewModel {
    val state: StateFlow<LibraryState>
    fun create(name: String, mode: GrainMode, pattern: PatternKind, size: Dimensions): String // returns new id
    fun rename(id: String, name: String); fun duplicate(id: String); fun delete(id: String); fun undoDelete()
    suspend fun importFile(); suspend fun exportFile(id: String)
  }
  enum class PatternKind { STRIPES, CHECKERBOARD, BRICK }
  ```
  New designs get a sensible default pattern per kind (stripes: walnut / hard maple alternating,
  7 strips of 1-1/2; checkerboard: walnut and hard maple, 1-1/2 cells; brick: cherry and hard
  maple, 3 × 1-1/2 bricks, 1/4 mortar). Size presets in the dialog: 18 × 12 × 1-1/2,
  16 × 10 × 1-1/4, 24 × 16 × 1-3/4, Custom. Edge grain disables checkerboard and brick in the
  dialog. Summary text: `18" × 12" · End-grain checkerboard`. Test tags: `library-new`,
  `library-import`, `design-card-<id>`, `design-menu-<id>`, `menu-open`, `menu-rename`,
  `menu-duplicate`, `menu-export`, `menu-delete`, `snackbar-undo`, `new-name`, `new-grain`,
  `new-pattern-<kind>`, `new-size-<preset>`, `new-create`.

- [ ] **Step 1: Write the tests.** View model: `createSavesAndLists`, `duplicateGetsNewIdAndName`
  (" copy"), `deleteThenUndoRestores`, `importCollisionGetsNewIdAndSuffix` (" (imported)"),
  `importNewerSchemaShowsError`. Screen: `unreadableDesignShowsCantOpen` with its reason;
  `edgeGrainDisablesCheckerAndBrick`. Screenshots: library with three designs, empty library, the
  new-design dialog.
- [ ] **Step 2: Run, expect FAIL; implement; run** tests and record/verify screenshots as in
  Task 16. Expected: PASS.
- [ ] **Step 3: Commit** on bookmark `library`.

### Task 18: Woods and Shop screens

**Files:**
- Create: `app/.../app/ui/woods/WoodsScreen.kt`, `WoodsViewModel.kt`, `CustomWoodDialog.kt`,
  `app/.../app/ui/shop/ShopScreen.kt`, `ShopViewModel.kt`, `app/.../app/ui/components/LengthField.kt`
- Test: `WoodsViewModelTest.kt`, `WoodsScreenTest.kt`, `ShopScreenTest.kt`, `LengthFieldTest.kt`,
  screenshots

**Interfaces:**
- Consumes: `WoodCatalog`, `rateCustomWood`, `derivedTones`, `WorkspaceStore`, `parseLength`.
- Produces:
  ```kotlin
  @Composable fun LengthField(label: String, value: Length, onValueChange: (Length) -> Unit, modifier: Modifier = Modifier, testTag: String)
  class WoodsViewModel(store: WorkspaceStore) : ViewModel { val woods: StateFlow<Woods>; val query: StateFlow<String>
    fun setQuery(q: String); fun setFilter(f: WoodFilter, on: Boolean); fun saveCustom(w: WoodSpecies); fun deleteCustom(id: String) }
  enum class WoodFilter { RECOMMENDED_ONLY, HIDE_NOT_SUITABLE, CUSTOM }
  class ShopViewModel(store: WorkspaceStore) : ViewModel { val profile: StateFlow<ShopProfile>; fun update(p: ShopProfile); fun resetToDefaults() }
  ```
  `LengthField` is an `OutlinedTextField` that keeps the typed text while it's being edited,
  shows `parseLength`'s reason as supporting text and the error state, and commits on valid input.
  Custom wood ids: `custom-<uuid>`. Test tags: `woods-search`, `woods-filter-<filter>`,
  `wood-row-<id>`, `woods-add-custom`, `custom-name`, `custom-janka`, `custom-pore`, `custom-oily`,
  `custom-bleeds`, `custom-irritant`, `custom-raw-hex`, `custom-oiled-hex`, `custom-override`,
  `custom-override-reason`, `custom-save`, `shop-<fieldName>` for every `ShopProfile` field,
  `shop-reset`, `shop-reset-confirm`.

- [ ] **Step 1: Write the tests.** `LengthFieldTest`: typing `1-3/` shows no commit and an
  error; finishing to `1-3/4` commits 1-3/4". Woods: `searchFilters` ("oak" → red and white oak),
  `customWoodShowsComputedRating` (Janka 550 → "Not suitable"), `overrideKeepsReason`,
  `deleteCustom`. Shop: `kerfChangeIsSaved`, `resetAsksFirst`. Screenshots: Woods with red oak
  expanded, the custom wood dialog, Shop.
- [ ] **Step 2: Run, expect FAIL; implement; run** tests and record/verify screenshots. Expected:
  PASS.
- [ ] **Step 3: Commit** on bookmark `woods-and-shop`.

### Task 19: Editor view model: editing, undo, compile and autosave

**Files:**
- Create: `app/.../app/ui/editor/EditorViewModel.kt`, `EditorState.kt`
- Test: `app/src/desktopTest/.../ui/editor/EditorViewModelTest.kt`

**Interfaces:**
- Consumes: `BoardDesign`, `DesignCompiler`, `WorkspaceStore`, `Woods`, `ShopProfile`,
  `UnitFormatter`.
- Produces:
  ```kotlin
  enum class SaveStatus { SAVED, SAVING, ERROR }
  enum class Finish { RAW, OILED }
  data class EditorState(val design: BoardDesign, val compile: CompileResult?, val saveStatus: SaveStatus,
    val finish: Finish, val canUndo: Boolean, val canRedo: Boolean, val pendingGrainSwitch: Boolean)
  class EditorViewModel(initial: BoardDesign, store: WorkspaceStore, woods: StateFlow<Woods>,
    profile: StateFlow<ShopProfile>, formatter: StateFlow<UnitFormatter>, clock: () -> Long,
    scope: CoroutineScope? = null) : ViewModel {
    val state: StateFlow<EditorState>
    fun edit(change: (BoardDesign) -> BoardDesign)   // one undo step
    fun requestGrainMode(mode: GrainMode); fun confirmGrainSwitch(); fun cancelGrainSwitch()
    fun undo(); fun redo(); fun setFinish(f: Finish)
    suspend fun flush()                               // writes any pending save now
  }
  ```
  Compile runs on `Dispatchers.Default` 150 ms after the last change; save 1 000 ms after the last
  change, bumping `revision` and `updatedAtMillis`. Undo history holds 100 steps. Switching to edge
  grain with a non-stripes pattern sets `pendingGrainSwitch`; confirming replaces the pattern with
  the stripes default from Task 17. Recompiles when woods, profile or formatter change. Saving
  sets `embeddedCustomWoods` to a copy of each custom wood the design uses, and the woods the
  compiler sees are the user's woods plus the design's embedded ones.

- [ ] **Step 1: Write the tests** with `runTest` and a `StandardTestDispatcher`.
  `editCompilesAfterDebounce` (no compile at 100 ms, compiled at 150 ms).
  `autosaveAfterOneSecond` (store unchanged at 999 ms, saved with revision + 1 at 1 000 ms).
  `rapidEditsSaveOnce`. `undoRedo`. `saveEmbedsUsedCustomWoods` (one used, one unused custom wood:
  only the used one is embedded). `closingFlushesPendingSave` (edit, then `flush()` at 200 ms
  writes it). `grainSwitchAsksForNonStripes`. `profileChangeRecompiles`.
  `saveFailureShowsError` (a store that throws → `SaveStatus.ERROR`, and the next edit retries).
- [ ] **Step 2: Run, expect FAIL; implement; run** `./gradlew :app:desktopTest --tests
  '*EditorViewModel*'`, expect PASS.
- [ ] **Step 3: Commit** on bookmark `editor-model`.

### Task 20: Board preview canvas

**Files:**
- Create: `app/.../app/ui/preview/BoardPreview.kt`, `TextureCache.kt`, `PreviewTransform.kt`
- Test: `app/src/desktopTest/.../ui/preview/PreviewTransformTest.kt`, `TextureCacheTest.kt`,
  `BoardPreviewScreenshotTest.kt`

**Interfaces:**
- Consumes: `BoardLayout`, `Piece`, `GrainTexture`, `textureSeed`, `Woods`, `Finish`,
  `EdgeTreatments`, `UnitFormatter`.
- Produces:
  ```kotlin
  @Composable fun BoardPreview(layout: BoardLayout, woods: Woods, finish: Finish, edges: EdgeTreatments,
    target: Dimensions, showDimensions: Boolean, modifier: Modifier = Modifier)
  class TextureCache(maxEntries: Int = 256) { fun get(spec: TextureSpec, widthPx: Int, heightPx: Int): ImageBitmap }
  data class PreviewTransform(val scale: Float, val offset: Offset) { fun zoomAt(factor: Float, focus: Offset): PreviewTransform; fun fit(board: Size, viewport: Size): PreviewTransform }
  ```
  Each piece is drawn by clipping to its polygon and drawing its cached texture; the board outline
  uses `cornerRadius`, and the juice groove is drawn as an inset rounded outline. Zoom 0.25×–8×
  with the scroll wheel and buttons (`preview-zoom-in`, `preview-zoom-out`, `preview-fit`), pan by
  dragging. Hover shows a `PlainTooltip` with species, strip number and slice (`preview-tooltip`).
  The dimensions overlay (`preview-dimensions`) shows the actual size, and the target in brackets
  when it differs. Texture pixel size is bucketed to the next power of two so zooming doesn't
  thrash the cache.

- [ ] **Step 1: Write the tests.** `PreviewTransformTest`: `fit` centres the board with 5%
  margin; `zoomAt` keeps the focus point fixed; zoom clamps to 0.25–8. `TextureCacheTest`: same
  spec and bucket returns the same bitmap instance; evicts least recently used past `maxEntries`.
  Screenshots (light theme only, the board is data-coloured): examples A, B and C in Raw and in
  Oiled; a stripes board with corner radius and juice groove; example A with the dimensions overlay.
- [ ] **Step 2: Run, expect FAIL; implement; run** tests, record the screenshots, and **look at
  each one** against the design's acceptance bar (rings vs lines unmistakable, same-strip cells
  match, flipped rows mirrored, species recognisable). Then verify. Expected: PASS.
- [ ] **Step 3: Commit** on bookmark `preview`.

### Task 21: Editor screen

**Files:**
- Create: `app/.../app/ui/editor/EditorScreen.kt`, `DesignPane.kt` (left), `StripListEditor.kt`,
  `PatternParams.kt`, `EdgeDetailsSection.kt`, `ResultsPane.kt` (Issues / Build / Lumber tabs),
  `IssuesTab.kt`, `BuildTab.kt`, `LumberTab.kt`, `app/.../app/ui/diagram/DiagramCanvas.kt`
- Test: `EditorScreenTest.kt`, `DiagramCanvasTest.kt`, `EditorScreenshotTest.kt`

**Interfaces:**
- Consumes: `EditorViewModel`, `BoardPreview`, `LengthField`, `Stage`, `Diagram`, `CutPart`,
  `Lumber`, `Issue`, `LocalRatingColors`, `StripePresets`.
- Produces: `@Composable fun EditorScreen(viewModel: EditorViewModel, onBack: () -> Unit, onExport: () -> Unit)`,
  `@Composable fun DiagramCanvas(diagram: Diagram, modifier: Modifier = Modifier)`.
  Layout as in the design: left pane 360 dp, right pane 380 dp, centre preview; below 1200 dp of
  window width a `PrimaryTabRow` "Preview | Issues | Build | Lumber" replaces the right pane. Top bar:
  back, editable name, save status, undo, redo, Raw/Oiled segmented button, Export packet.
  Compile errors show in an error card above the preview, using `CompileError.message`. Test tags:
  `editor-back`, `editor-name`, `editor-save-status`, `editor-undo`, `editor-redo`,
  `editor-finish-raw`, `editor-finish-oiled`, `editor-export`, `grain-edge`, `grain-end`,
  `grain-switch-confirm`, `dim-length`, `dim-width`, `dim-thickness`, `pattern-<kind>`,
  `strip-<i>-species`, `strip-<i>-width`, `strip-<i>-up`, `strip-<i>-down`, `strip-<i>-remove`,
  `strip-add`, `stripe-presets`, `row-height`, `checker-a`, `checker-b`, `checker-cell`,
  `brick-species`, `mortar-species`, `brick-width`, `brick-height`, `mortar-width`, `edge-corner`,
  `edge-profile`, `edge-groove`, `edge-grips`, `edge-feet`, `tab-issues`, `tab-build`,
  `tab-lumber`, `tab-preview`, `issue-<code>`, `stage-<number>`, `lumber-total`.

- [ ] **Step 1: Write the tests.** `changingWidthUpdatesLumber` (example A to W 14 changes
  `lumber-total`). `addingRedOakShowsAvoid` (end grain, a red oak strip → `issue-SPECIES_NOT_SUITABLE`).
  `buildTabListsStages` (example A: 10 stages minus edge details = 9). `undoRestoresWidth`.
  `grainSwitchConfirmation`. `narrowWindowCollapsesToTabs` (width 1100 dp shows `tab-preview`).
  `metricShowsMillimetresEverywhere` (Metric formatter: `dim-length` shows `457.2`, Build and
  Lumber show `mm`, the stored design is unchanged). `compileErrorShowsCard` (L 40").
  `DiagramCanvasTest`: draws without error for every stage diagram of A, B and C. Screenshots: the
  editor for A, B and C, light and dark; the collapsed layout; each tab for A; each stage diagram
  of A on its own.
- [ ] **Step 2: Run, expect FAIL; implement; run** tests and record/verify screenshots. Expected:
  PASS.
- [ ] **Step 3: Commit** on bookmark `editor-screen`.

### Task 22: Packet export, shortcuts, packaging and docs

**Files:**
- Create: `app/.../app/ui/editor/ExportPacket.kt`, `app/src/desktopMain/.../app/Shortcuts.kt`
- Modify: `App.kt`, `Main.kt` (shortcuts, flush on close), `README.md`, `AGENTS.md`,
  `docs/CODE-TOUR.md`, `docs/phase1/phase-1-implementation.md` (status notes),
  `app/build.gradle.kts` (deb metadata: maintainer, menu group "Graphics", icon)
- Test: `ExportPacketTest.kt`, `ShortcutsTest.kt`

**Interfaces:**
- Consumes: `ShopPacketExporter`, `FileDialogs`, `ExternalBrowser`, `EditorViewModel.flush`.
- Produces: `suspend fun exportPacket(compiled: CompiledDesign, woods: Woods, profile: ShopProfile,
  formatter: UnitFormatter, dialogs: FileDialogs): String?` (saved path); after saving, a snackbar
  with "Open in browser" (`snackbar-open-packet`). Shortcuts: Ctrl+N new design, Ctrl+Z undo,
  Ctrl+Shift+Z and Ctrl+Y redo, Ctrl+P export packet, Ctrl+F focus wood search. Closing the window
  awaits `flush()` on the open editor before exiting.

- [ ] **Step 1: Write the tests.** `exportUsesDefaultName` (fake dialogs receive
  `"<name> - shop packet.html"` and the HTML). `cancelledExportDoesNothing`. `openInBrowserOpensSavedFile`.
  `ShortcutsTest`: each shortcut reaches its action in the editor and library.
- [ ] **Step 2: Run, expect FAIL; implement; run** `./scripts/ci.sh`. Expected: BUILD
  SUCCESSFUL.
- [ ] **Step 3: Manual check** on the owner's Ubuntu machine: install the `.deb`
  (`sudo apt install ./app/build/compose/binaries/main/deb/*.deb`), start GrainCraft Studio from the
  app menu, build example A, export the packet, open it in the browser, and print preview it. Note
  anything off in the status line.
- [ ] **Step 4: Docs.** `README.md`: what it does now, how to run and install. `AGENTS.md`: real
  modules, commands and test layout. `docs/CODE-TOUR.md`: every package. Status notes on each task
  here.
- [ ] **Step 5: Commit** on bookmark `packet-and-packaging`.
