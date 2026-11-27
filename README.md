# GrainCraft Studio

A cutting board designer and shop planner. Pick the woods, the pattern and the finished size;
GrainCraft Studio shows what the board will look like, warns about woods that are bad for a cutting
board, and works backwards to a shop plan: lumber to buy, cuts to make, glue-up order, and
allowances for kerf, trimming and flattening, based on the tools in your shop.

It is built with Kotlin Multiplatform and Compose Multiplatform. The first app is a native Linux
desktop app; Windows, Android and a web version follow from the same code.

## What it does now

Phase 1 (see [the roadmap](docs/roadmap.md) and [the phase 1 design](docs/phase1/phase-1-design.md)):

- **Designs:** edge-grain stripes, and end-grain stripes, checkerboard and brick. A library of
  cards with thumbnails; new, rename, duplicate, delete with undo, and import or export of
  `.graincraft.json` files. Everything saves itself.
- **Editor:** size, pattern, woods and edge details on the left; a preview of the finished board
  in the centre, with grain textures in raw or oiled tones; and the issues, the step-by-step build
  guide with diagrams, and the lumber and cut list on the right. Undo and redo.
- **Woods:** 26 built-in species rated for edge- and end-grain boards, plus your own custom woods.
- **Shop:** your saws, planer, sander and allowances, which the build math follows.
- **Shop packet:** Export packet saves one HTML page (cover, issues, shopping and cut lists, every
  stage with its diagram, and the shop settings) that you open in a browser to print or save as
  PDF.
- Inches at 1/16" or 1/32", or millimetres; light and dark themes.

Keyboard shortcuts: Ctrl+N new design, Ctrl+Z undo, Ctrl+Shift+Z or Ctrl+Y redo, Ctrl+P export the
shop packet, Ctrl+F search woods.

Your designs and settings live in `~/.local/share/graincraft-studio/` (or
`$XDG_DATA_HOME/graincraft-studio/`).

## Run it

From a clone of this repository, with a JDK installed (Gradle fetches Java 25 itself):

```bash
./scripts/run-desktop.sh
```

## Install it on Ubuntu

Build the Debian package and install it:

```bash
./gradlew :app:packageDeb
sudo apt install ./app/build/compose/binaries/main/deb/graincraft-studio_*.deb
```

GrainCraft Studio then appears in the app menu under Graphics. Remove it with
`sudo apt remove graincraft-studio`; your designs stay in the data folder.

## License

Apache License 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
