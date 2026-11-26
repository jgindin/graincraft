# AGENTS.md

Read this before changing anything. It is for AI coding agents (Claude Code, Gemini CLI, Codex and
similar) and for people. If something below disagrees with the repository, trust the repository and
fix this file in the same change.

## What this is

**GrainCraft Studio** (`github.com/jgindin/graincraft`, public, Apache-2.0): a cutting board
designer and shop planner. It previews a board's woods and grain, warns about woods that are bad for
cutting boards, and works backwards from the finished size to a lumber list, cut list and
step-by-step build guide based on the user's shop tools.

- Kotlin Multiplatform + Compose Multiplatform. Package root `com.jaygindin.graincraft`.
- The first app is a **native Linux desktop app**. Windows, Android and web come later from the same
  code; see [docs/roadmap.md](docs/roadmap.md).
- **Current state:** planning. The repository has no Gradle build yet. Phase 1's first task creates
  it; update this file as the build, modules and commands land.

## Planned modules (phase 1)

| Module | What | Targets |
|---|---|---|
| `:core` | The domain: lengths and units, wood catalog and ratings, designs, pattern fitting, build math, warnings, build guide, grain textures, shop packet HTML. Pure Kotlin, no platform code. | `jvm()` and `wasmJs { nodejs() }` |
| `:app` | The Compose UI, view models and theme (`commonMain`), plus the desktop entry point, file storage and dialogs (`desktopMain`). | `jvm("desktop")` in phase 1 |

- `:core` builds for Wasm so that Java-only APIs can't creep into shared code. `:app` has only a JVM
  target in phase 1, so a detekt `ForbiddenImport` rule bans `java.*` and `javax.*` in every
  `commonMain`. Platform services are interfaces in `commonMain`, implemented in `desktopMain`.

## Build, test and run (once the build exists)

Always use the Gradle wrapper from the repository root (`./gradlew`). Versions live in
`gradle/libs.versions.toml`; do not hardcode them in build scripts. The build uses Java 21.

```bash
./gradlew :core:allTests           # the domain tests, on the JVM and on Wasm (Node)
./gradlew :app:desktopTest         # Compose Desktop UI tests
./gradlew verifyRoborazziDesktop   # compare the screenshot tests with their goldens
./gradlew :app:run                 # run the desktop app
./gradlew :app:packageDeb          # build the .deb
```

## Code quality gates (CI enforces all of these)

```bash
./gradlew ktfmtCheck detekt     # formatting + static analysis; run before every commit
./gradlew ktfmtFormat           # fix formatting
./gradlew koverVerify           # coverage gate on :core
```

- **Formatting: ktfmt**, Google style, **100 columns**, for all Kotlin and `.kts`. Long `//`
  comments are not re-wrapped by ktfmt: wrap them by hand.
- **Static analysis: detekt** (`dev.detekt` 2.0.0-alpha series, the one that supports the Kotlin
  2.4 compiler). Config: `config/detekt/detekt.yml` (overrides only, each with a reason). **There is
  no baseline: fix findings, don't suppress them.** A narrow `@Suppress` with a reason comment is
  acceptable only where a rule conflicts with an API shape the code doesn't control.
- **UI is Material 3 Compose components throughout.** Colours, type, shapes and spacing come from
  the app theme (`GrainCraftTheme`, `Spacing`); screens never use colour literals or `fontSize =`.
  A theme test enforces this. Hand-drawn UI is only for the board preview and the build diagrams.
- Comments explain *why the code is the way it is*, in the present tense. Do not write comments that
  refer to how the code used to be ("previously", "originally", "was moved from") or to plan/task
  numbers; they rot.

## Tests

- Write the test first, and **mutation-check** every test you add: break the code under test once,
  see the test fail, restore.
- Build math is the part where a mistake wastes real lumber. Every formula in the phase 1 design has
  hand-checked worked examples as tests.
- Fixtures are synthetic. Never commit real user data or credentials.
- **Screenshot tests: Roborazzi** on Compose Desktop, with goldens in `app/screenshots/`.
  `verifyRoborazziDesktop` runs in CI; run `recordRoborazziDesktop` only when a visual change is
  intended, and say so in the PR. Dependency bumps (Compose, Skiko, Roborazzi) must pass
  `verifyRoborazziDesktop` with no golden changes.

## Version control and hosting

- Colocated **Jujutsu (`jj`) over Git**; the remote is `github.com/jgindin/graincraft`. Commit with
  `jj commit -m ...`, not `git commit`. Messages are a plain complete sentence on what changed and
  why. Use a heredoc or single quotes for messages containing backticks (double quotes make the
  shell eat them). Agents add the `Co-Authored-By:` trailer they were told to use.
- **Work on a bookmark, open a PR, and merge with "Rebase and merge" only.** Never squash and never
  create merge commits. `main` is protected: a PR is required (no approvals needed) and the `verify`
  check must pass on an up-to-date branch. The repository settings allow only "Rebase and merge",
  and merged branches are deleted automatically.
- **One purpose per commit and PR.** Before committing, check the diff against the commit message:
  anything the message does not account for comes out.
- Typical flow:
  ```bash
  jj new main                              # start from the current main
  # ... edit, test ...
  jj commit -m 'Plain sentence about what and why'
  jj bookmark create my-change -r @-
  jj git push --bookmark my-change
  gh pr create --head my-change --base main --fill   # --head is needed: jj leaves git HEAD detached
  # after the PR is merged on GitHub:
  jj git fetch                              # jj recognizes the rebase-merged commits by itself
  ```
- When `main` has moved and your branch is "out-of-date": `jj git fetch`,
  `jj rebase -b my-change -d main`, push.
- **Never commit credentials, keystores or API keys** (Dropbox app keys included, in later phases).
  `.gitignore` covers the usual files. If a secret is committed by mistake, stop and treat it as
  compromised; deleting it in a follow-up commit is not enough because the repository is public.
- Dependabot opens PRs weekly (Gradle and the workflow actions). Check CI before merging.

## CI (`.github/workflows/ci.yml`)

| Job | Runs | Required? |
|---|---|---|
| `verify` | `ktfmtCheck detekt`, then `:core:allTests :app:desktopTest verifyRoborazziDesktop koverVerify :app:packageDeb` | **Yes** (branch protection) |

Its Gradle steps are skipped while the repository has no `gradlew`, so the required check still
reports.

## Planning documents

- `docs/roadmap.md`: the phases (1 core and Linux desktop, 2 more patterns, 3 3D viewer, 4 photo
  import, 5 Android, 6 Dropbox sync, 7 web, 8 prompts, 9 Windows).
- `docs/phase1/phase-1-design.md`: the phase 1 design. Each phase gets its own design and
  implementation plan under `docs/phaseN/`; keep status notes current when you finish work.
